package com.examlifecycle.controller;




import com.examlifecycle.domain.*;
import com.examlifecycle.dto.DraftDtos.*;
import com.examlifecycle.security.AuthUtil;
import com.examlifecycle.security.CurrentUser;
import com.examlifecycle.service.DraftService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drafts")
public class DraftController {

    private final DraftService draftService;

    public DraftController(DraftService draftService) {
        this.draftService = draftService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody CreateDraftRequest req) {
        CurrentUser user = AuthUtil.current();
        AuthUtil.requireRole(user, Role.PAPER_SETTER);
        Draft draft = draftService.createDraft(user, req);
        return Map.of(
                "id", draft.getId(), "title", draft.getTitle(),
                "status", draft.getStatus(), "version", draft.getCurrentVersion());
    }

    @GetMapping
    public List<Draft> list() {
        AuthUtil.current();
        return draftService.listAll();
    }

    @GetMapping("/{id}")
    public DraftDetailResponse get(@PathVariable Long id) {
        CurrentUser user = AuthUtil.current();
        Draft draft = draftService.viewDraft(user, id);
        return new DraftDetailResponse(
                draft.getId(), draft.getBlueprintId(), draft.getTitle(), draft.getStatus(),
                draft.getCurrentVersion(), draft.getCreatedBy(), draft.getCreatedAt(),
                draft.getFinalHash(), draft.getLockedAt(),
                draftService.versionsFor(id), draftService.approvalsFor(id));
    }

    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> newVersion(@PathVariable Long id, @Valid @RequestBody NewVersionRequest req) {
        CurrentUser user = AuthUtil.current();
        AuthUtil.requireRole(user, Role.PAPER_SETTER);
        DraftVersion version = draftService.newVersion(user, id, req);
        return Map.of("draftId", id, "version", version.getVersionNumber(), "contentHash", version.getContentHash());
    }

    @PostMapping("/{id}/submit")
    public Map<String, Object> submit(@PathVariable Long id) {
        CurrentUser user = AuthUtil.current();
        AuthUtil.requireRole(user, Role.PAPER_SETTER);
        Draft draft = draftService.submitForModeration(user, id);
        return Map.of("draftId", id, "status", draft.getStatus());
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentView addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest req) {
        CurrentUser user = AuthUtil.current();
        AuthUtil.requireRole(user, Role.MODERATOR);
        return draftService.addComment(user, id, req);
    }

    @GetMapping("/{id}/comments")
    public List<CommentView> listComments(@PathVariable Long id) {
        AuthUtil.current();
        return draftService.commentsFor(id);
    }

    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(@PathVariable Long id, @Valid @RequestBody ApproveRequest req) {
        CurrentUser user = AuthUtil.current();
        if (req.stage() == ApprovalStage.MODERATOR_REVIEW) {
            AuthUtil.requireRole(user, Role.MODERATOR);
        } else {
            AuthUtil.requireRole(user, Role.EXAM_OFFICER);
        }
        Draft draft = draftService.approve(user, id, req);
        return Map.of("draftId", id, "stage", req.stage(), "decision", req.decision(), "status", draft.getStatus());
    }

    @PostMapping("/{id}/lock")
    public Map<String, Object> lock(@PathVariable Long id) {
        CurrentUser user = AuthUtil.current();
        AuthUtil.requireRole(user, Role.EXAM_OFFICER);
        Draft draft = draftService.lock(user, id);
        return Map.of("draftId", id, "status", draft.getStatus(), "finalHash", draft.getFinalHash());
    }

    /** Live paper review at any stage — distinct from /export, which is locked-only and watermarked. */
    @GetMapping("/{id}/content")
    public DraftContentResponse content(@PathVariable Long id) {
        CurrentUser user = AuthUtil.current();
        return draftService.getContent(user, id);
    }

    @GetMapping("/{id}/export")
    public ExportResponse export(@PathVariable Long id) {
        CurrentUser user = AuthUtil.current();
        return draftService.export(user, id);
    }

@GetMapping("/{id}/download-pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        CurrentUser user = AuthUtil.current();
        byte[] pdfBytes = draftService.generatePdf(user, id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "exam-draft-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/audit")
    public List<AuditLogView> auditForDraft(@PathVariable Long id) {
        AuthUtil.requireRole(AuthUtil.current(), Role.EXAM_OFFICER);
        return draftService.auditFor(id);
    }
}
