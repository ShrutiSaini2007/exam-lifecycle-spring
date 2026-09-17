package com.examlifecycle;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end workflow test mirroring the Python prototype's smoke_test.py,
 * so the two implementations can be checked against the same acceptance
 * criteria: version accuracy, unauthorized-access prevention, workflow
 * completion, and audit integrity.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DraftWorkflowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private ResponseEntity<Map> call(HttpMethod method, String path, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return rest.exchange(url(path), method, entity, Map.class);
    }

    private String loginAndGetToken(String username, String password) {
        ResponseEntity<Map> res = call(HttpMethod.POST, "/api/login", null,
                Map.of("username", username, "password", password));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) res.getBody().get("token");
    }

    private static String adminToken;
    private static String setterToken;
    private static String moderatorToken;
    private static String officerToken;
    private static Number draftId;

    @Test
    @Order(1)
    void adminLogsInAndProvisionsAccounts() {
        adminToken = loginAndGetToken("admin", "test-admin-pw123");
        assertThat(adminToken).isNotBlank();

        var setter = call(HttpMethod.POST, "/api/register", adminToken,
                Map.of("username", "setter1", "password", "pw12345678", "role", "PAPER_SETTER"));
        assertThat(setter.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        call(HttpMethod.POST, "/api/register", adminToken,
                Map.of("username", "mod1", "password", "pw12345678", "role", "MODERATOR"));
        call(HttpMethod.POST, "/api/register", adminToken,
                Map.of("username", "officer1", "password", "pw12345678", "role", "EXAM_OFFICER"));

        setterToken = loginAndGetToken("setter1", "pw12345678");
        moderatorToken = loginAndGetToken("mod1", "pw12345678");
        officerToken = loginAndGetToken("officer1", "pw12345678");
    }

    @Test
    @Order(2)
    void nonAdminCannotSelfProvisionAccounts() {
        var res = call(HttpMethod.POST, "/api/register", setterToken,
                Map.of("username", "sneaky", "password", "pw12345678", "role", "ADMIN"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(3)
    void fullLifecycleFromBlueprintToLockedExport() {
        var course = call(HttpMethod.POST, "/api/courses", setterToken,
                Map.of("code", "BIT-55", "name", "Secure Exam Lifecycle"));
        assertThat(course.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number courseId = (Number) course.getBody().get("id");

        var section = new LinkedHashMap<String, Object>();
        section.put("topic", "DBMS");
        section.put("marks", 20);
        section.put("count", 4);
        var blueprint = call(HttpMethod.POST, "/api/blueprints", setterToken,
                Map.of("courseId", courseId, "title", "Sem V Endsem", "sections", List.of(section)));
        assertThat(blueprint.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number blueprintId = (Number) blueprint.getBody().get("id");

        var q1 = Map.of("text", "Explain ACID properties", "marks", 5, "topic", "DBMS", "difficulty", "medium");
        var q2 = Map.of("text", "Explain normalization", "marks", 5, "topic", "DBMS", "difficulty", "medium");

        // moderator must not be able to import questions
        var blocked = call(HttpMethod.POST, "/api/blueprints/" + blueprintId + "/questions", moderatorToken,
                Map.of("questions", List.of(Map.of("text", "x", "marks", 1))));
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var imported = call(HttpMethod.POST, "/api/blueprints/" + blueprintId + "/questions", setterToken,
                Map.of("questions", List.of(q1, q2)));
        assertThat(imported.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        List<Number> questionIds = (List<Number>) imported.getBody().get("ids");
        assertThat(questionIds).hasSize(2);

        var draft = call(HttpMethod.POST, "/api/drafts", setterToken,
                Map.of("blueprintId", blueprintId, "title", "Endsem Draft A", "questionIds", questionIds));
        assertThat(draft.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        draftId = (Number) draft.getBody().get("id");
        assertThat(draft.getBody().get("version")).isEqualTo(1);

        // a moderator/officer must be able to read the paper before approving it
        var content = call(HttpMethod.GET, "/api/drafts/" + draftId + "/content", moderatorToken, null);
        assertThat(content.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) content.getBody().get("questions")).hasSize(2);

        var submit = call(HttpMethod.POST, "/api/drafts/" + draftId + "/submit", setterToken, null);
        assertThat(submit.getBody().get("status")).isEqualTo("IN_MODERATION");

        var comment = call(HttpMethod.POST, "/api/drafts/" + draftId + "/comments", moderatorToken,
                Map.of("text", "Please rephrase Q2."));
        assertThat(comment.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // sequential gate: officer cannot approve before the moderator does
        var earlyOfficer = call(HttpMethod.POST, "/api/drafts/" + draftId + "/approve", officerToken,
                Map.of("stage", "OFFICER_APPROVAL", "decision", "APPROVED"));
        assertThat(earlyOfficer.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        var modApprove = call(HttpMethod.POST, "/api/drafts/" + draftId + "/approve", moderatorToken,
                Map.of("stage", "MODERATOR_REVIEW", "decision", "APPROVED"));
        assertThat(modApprove.getBody().get("status")).isEqualTo("MODERATOR_APPROVED");

        var offApprove = call(HttpMethod.POST, "/api/drafts/" + draftId + "/approve", officerToken,
                Map.of("stage", "OFFICER_APPROVAL", "decision", "APPROVED"));
        assertThat(offApprove.getBody().get("status")).isEqualTo("OFFICER_APPROVED");

        var locked = call(HttpMethod.POST, "/api/drafts/" + draftId + "/lock", officerToken, null);
        assertThat(locked.getBody().get("status")).isEqualTo("LOCKED");
        String finalHash = (String) locked.getBody().get("finalHash");
        assertThat(finalHash).isNotBlank();

        // immutability: no further versions once locked
        var blockedVersion = call(HttpMethod.POST, "/api/drafts/" + draftId + "/versions", setterToken,
                Map.of("questionIds", questionIds));
        assertThat(blockedVersion.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        var export = call(HttpMethod.GET, "/api/drafts/" + draftId + "/export", officerToken, null);
        assertThat(export.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(export.getBody().get("finalHash")).isEqualTo(finalHash);
        assertThat(export.getBody().get("preparedBy")).isEqualTo("setter1");
        assertThat(export.getBody().get("moderatedBy")).isEqualTo("mod1");
        assertThat(export.getBody().get("approvedBy")).isEqualTo("officer1");
    }

    @Test
    @Order(4)
    void auditTrailIsRestrictedAndCapturesKeyEvents() {
        var forbidden = call(HttpMethod.GET, "/api/drafts/" + draftId + "/audit", setterToken, null);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<List> audit = rest.exchange(url("/api/drafts/" + draftId + "/audit"),
                HttpMethod.GET, new HttpEntity<>(authHeaders(officerToken)), List.class);
        assertThat(audit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(audit.getBody()).isNotEmpty();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
