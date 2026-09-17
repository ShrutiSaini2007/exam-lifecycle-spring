package com.examlifecycle.service;

// --- ADD THE OPENPDF IMPORTS HERE ---
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import java.awt.Color;
import com.lowagie.text.pdf.PdfContentByte;


import com.examlifecycle.domain.*;
import com.examlifecycle.dto.DraftDtos.*;
import com.examlifecycle.exception.ApiException;
import com.examlifecycle.repository.*;
import com.examlifecycle.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DraftService {

    private final DraftRepository draftRepository;
    private final DraftVersionRepository versionRepository;
    private final CommentRepository commentRepository;
    private final ApprovalRepository approvalRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final HashService hashService;
    private final ObjectMapper mapper;

    public DraftService(DraftRepository draftRepository, DraftVersionRepository versionRepository,
                         CommentRepository commentRepository, ApprovalRepository approvalRepository,
                         QuestionRepository questionRepository, UserRepository userRepository,
                         AuditLogRepository auditLogRepository, AuditService auditService,
                         HashService hashService, ObjectMapper mapper) {
        this.draftRepository = draftRepository;
        this.versionRepository = versionRepository;
        this.commentRepository = commentRepository;
        this.approvalRepository = approvalRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.hashService = hashService;
        this.mapper = mapper;
    }

    private record ContentPayload(List<Long> questionIds) {}
    private record LockPayload(String contentHash, Long lockedBy, long lockedAtEpochMillis) {}

    // ------------------------------------------------------------------
    // Drafts & versioning
    // ------------------------------------------------------------------

    @Transactional
    public Draft createDraft(CurrentUser user, CreateDraftRequest req) {
        Draft draft = new Draft(req.blueprintId(), req.title(), user.id());
        draft = draftRepository.save(draft);

        String contentJson = writeContentJson(req.questionIds());
        String contentHash = hashService.sha256Json(new ContentPayload(req.questionIds()));
        versionRepository.save(new DraftVersion(draft.getId(), 1, contentJson, contentHash, user.id()));

        auditService.record(user.id(), "CREATE_DRAFT", "draft", draft.getId(),
                Map.of("version", 1, "hash", contentHash));
        return draft;
    }

    public List<Draft> listAll() {
        return draftRepository.findAllByOrderByIdDesc();
    }

    public Draft getOrThrow(Long id) {
        return draftRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "draft not found"));
    }

    public List<DraftVersion> versionsFor(Long draftId) {
        return versionRepository.findByDraftIdOrderByVersionNumberAsc(draftId);
    }

    public List<Approval> approvalsFor(Long draftId) {
        return approvalRepository.findByDraftIdOrderByIdAsc(draftId);
    }

    @Transactional
    public Draft viewDraft(CurrentUser user, Long draftId) {
        Draft draft = getOrThrow(draftId);
        auditService.record(user.id(), "VIEW_DRAFT", "draft", draftId);
        return draft;
    }

    @Transactional
    public DraftVersion newVersion(CurrentUser user, Long draftId, NewVersionRequest req) {
        Draft draft = getOrThrow(draftId);
        if (draft.getStatus() == DraftStatus.LOCKED) {
            throw new ApiException(HttpStatus.CONFLICT, "draft is locked; no further versions permitted");
        }
        int nextVersion = draft.getCurrentVersion() + 1;
        String contentJson = writeContentJson(req.questionIds());
        String contentHash = hashService.sha256Json(new ContentPayload(req.questionIds()));
        DraftVersion version = versionRepository.save(
                new DraftVersion(draftId, nextVersion, contentJson, contentHash, user.id()));

        // a new revision resets any prior approvals — the paper must be re-reviewed
        draft.setCurrentVersion(nextVersion);
        draft.setStatus(DraftStatus.DRAFT);
        draftRepository.save(draft);

        auditService.record(user.id(), "NEW_VERSION", "draft", draftId,
                Map.of("version", nextVersion, "hash", contentHash));
        return version;
    }

    @Transactional
    public Draft submitForModeration(CurrentUser user, Long draftId) {
        Draft draft = getOrThrow(draftId);
        if (draft.getStatus() != DraftStatus.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "cannot submit from status " + draft.getStatus());
        }
        draft.setStatus(DraftStatus.IN_MODERATION);
        draftRepository.save(draft);
        auditService.record(user.id(), "SUBMIT_FOR_MODERATION", "draft", draftId);
        return draft;
    }

    // ------------------------------------------------------------------
    // Comments
    // ------------------------------------------------------------------

    @Transactional
    public CommentView addComment(CurrentUser user, Long draftId, CommentRequest req) {
        Draft draft = getOrThrow(draftId);
        DraftVersion version = currentVersionOrThrow(draft);
        Comment comment = commentRepository.save(new Comment(version.getId(), user.id(), req.text()));
        auditService.record(user.id(), "ADD_COMMENT", "draft_version", version.getId(), Map.of("text", req.text()));
        return toCommentView(comment);
    }

    public List<CommentView> commentsFor(Long draftId) {
        Draft draft = getOrThrow(draftId);
        return versionsFor(draftId).stream()
                .flatMap(v -> commentRepository.findByDraftVersionIdOrderByIdAsc(v.getId()).stream())
                .map(this::toCommentView)
                .toList();
    }

    private CommentView toCommentView(Comment c) {
        return new CommentView(c.getId(), c.getDraftVersionId(), c.getCommenterId(),
                usernameOf(c.getCommenterId()), c.getText(), c.isResolved(), c.getCreatedAt());
    }

    // ------------------------------------------------------------------
    // Sequential approval gate: MODERATOR_REVIEW -> OFFICER_APPROVAL
    // ------------------------------------------------------------------

    @Transactional
    public Draft approve(CurrentUser user, Long draftId, ApproveRequest req) {
        Draft draft = getOrThrow(draftId);
        DraftStatus newStatus;

        if (req.stage() == ApprovalStage.MODERATOR_REVIEW) {
            if (draft.getStatus() != DraftStatus.IN_MODERATION) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "draft must be IN_MODERATION, currently " + draft.getStatus());
            }
            newStatus = req.decision() == ApprovalDecision.APPROVED
                    ? DraftStatus.MODERATOR_APPROVED : DraftStatus.DRAFT;
        } else { // OFFICER_APPROVAL — sequential gate: only after moderator approval
            if (draft.getStatus() != DraftStatus.MODERATOR_APPROVED) {
                throw new ApiException(HttpStatus.CONFLICT, "officer approval requires prior MODERATOR_APPROVED status");
            }
            newStatus = req.decision() == ApprovalDecision.APPROVED
                    ? DraftStatus.OFFICER_APPROVED : DraftStatus.DRAFT;
        }

        approvalRepository.save(new Approval(draftId, req.stage(), user.id(), req.decision(), req.comment()));
        draft.setStatus(newStatus);
        draftRepository.save(draft);
        auditService.record(user.id(), "APPROVAL_" + req.decision(), "draft", draftId, Map.of("stage", req.stage().name()));
        return draft;
    }

    // ------------------------------------------------------------------
    // Final hash & lock
    // ------------------------------------------------------------------

    @Transactional
    public Draft lock(CurrentUser user, Long draftId) {
        Draft draft = getOrThrow(draftId);
        if (draft.getStatus() != DraftStatus.OFFICER_APPROVED) {
            throw new ApiException(HttpStatus.CONFLICT, "draft must be OFFICER_APPROVED before it can be locked");
        }
        DraftVersion version = currentVersionOrThrow(draft);

        Instant lockedAt = Instant.now();
        String finalHash = hashService.sha256Json(
                new LockPayload(version.getContentHash(), user.id(), lockedAt.toEpochMilli()));

        draft.setStatus(DraftStatus.LOCKED);
        draft.setFinalHash(finalHash);
        draft.setLockedAt(lockedAt);
        draft.setLockedBy(user.id());
        draftRepository.save(draft);

        auditService.record(user.id(), "LOCK_DRAFT", "draft", draftId, Map.of("finalHash", finalHash));
        return draft;
    }

    // ------------------------------------------------------------------
    // Live content review (any stage) vs. final watermarked export (LOCKED only)
    // ------------------------------------------------------------------

    @Transactional
    public DraftContentResponse getContent(CurrentUser user, Long draftId) {
        Draft draft = getOrThrow(draftId);
        DraftVersion version = currentVersionOrThrow(draft);
        List<QuestionView> questions = loadQuestionViews(version);
        int totalMarks = questions.stream().mapToInt(QuestionView::marks).sum();

        auditService.record(user.id(), "VIEW_DRAFT_CONTENT", "draft_version", version.getId(),
                Map.of("version", draft.getCurrentVersion()));

        String preparedBy = usernameOf(draft.getCreatedBy());
        return new DraftContentResponse(draftId, draft.getCurrentVersion(), version.getContentHash(),
                questions, totalMarks, preparedBy, draft.getCreatedAt());
    }

    @Transactional
    public ExportResponse export(CurrentUser user, Long draftId) {
        Draft draft = getOrThrow(draftId);
        if (draft.getStatus() != DraftStatus.LOCKED) {
            throw new ApiException(HttpStatus.CONFLICT, "only locked drafts can be exported");
        }
        DraftVersion version = currentVersionOrThrow(draft);
        List<QuestionView> questions = loadQuestionViews(version);

        String preparedBy = usernameOf(draft.getCreatedBy());
        Optional<Approval> modApproval = approvalRepository.findFirstByDraftIdAndStageAndDecisionOrderByIdDesc(
                draftId, ApprovalStage.MODERATOR_REVIEW, ApprovalDecision.APPROVED);
        Optional<Approval> offApproval = approvalRepository.findFirstByDraftIdAndStageAndDecisionOrderByIdDesc(
                draftId, ApprovalStage.OFFICER_APPROVAL, ApprovalDecision.APPROVED);
        String moderatedBy = modApproval.map(a -> usernameOf(a.getApproverId())).orElse(null);
        String lockedBy = usernameOf(draft.getLockedBy());

        String hashFragment = draft.getFinalHash().length() > 12 ? draft.getFinalHash().substring(0, 12) : draft.getFinalHash();
        String watermark = "ISSUED TO " + user.username() + " (" + user.roleName() + ") — "
                + "prepared by " + valueOr(preparedBy) + " — "
                + "moderated by " + valueOr(moderatedBy) + " — "
                + "approved & locked by " + valueOr(lockedBy) + " — "
                + "hash " + hashFragment + "…";

        auditService.record(user.id(), "EXPORT_LOCKED_DRAFT", "draft", draftId, Map.of("finalHash", draft.getFinalHash()));

        return new ExportResponse(draftId, draft.getTitle(), draft.getFinalHash(), watermark, questions,
                user.username(), Instant.now(),
                preparedBy, draft.getCreatedAt(),
                moderatedBy, modApproval.map(Approval::getCreatedAt).orElse(null),
                lockedBy, draft.getLockedAt());
    }

    // ------------------------------------------------------------------
    // Audit trail
    // ------------------------------------------------------------------

    public List<AuditLogView> auditFor(Long draftId) {
        List<Long> versionIds = versionsFor(draftId).stream().map(DraftVersion::getId).toList();
        return auditLogRepository.findForDraft(draftId, versionIds.isEmpty() ? List.of(-1L) : versionIds)
                .stream().map(this::toAuditView).toList();
    }

    public List<AuditLogView> globalAudit() {
        return auditLogRepository.findAllByOrderByIdDesc().stream().map(this::toAuditView).toList();
    }

    private AuditLogView toAuditView(com.examlifecycle.domain.AuditLog a) {
        return new AuditLogView(a.getId(), a.getUserId(), usernameOf(a.getUserId()), a.getAction(),
                a.getEntityType(), a.getEntityId(), a.getDetails(), a.getIp(), a.getCreatedAt());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private DraftVersion currentVersionOrThrow(Draft draft) {
        return versionRepository.findByDraftIdAndVersionNumber(draft.getId(), draft.getCurrentVersion())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "current version record missing"));
    }

    private List<QuestionView> loadQuestionViews(DraftVersion version) {
        List<Long> ids = readQuestionIds(version.getContentJson());
        if (ids.isEmpty()) return List.of();
        Map<Long, Question> byId = questionRepository.findByIdIn(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Question::getId, q -> q));
        // preserve the original selection order rather than DB insertion order
        return ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(q -> new QuestionView(q.getId(), q.getText(), q.getTopic(), q.getMarks(), q.getDifficulty()))
                .toList();
    }

    private String usernameOf(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(User::getUsername).orElse(null);
    }

    private String valueOr(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private String writeContentJson(List<Long> questionIds) {
        try {
            return mapper.writeValueAsString(new ContentPayload(questionIds));
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize draft content", e);
        }
    }

    private List<Long> readQuestionIds(String contentJson) {
        try {
            return mapper.readValue(contentJson, ContentPayload.class).questionIds();
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse stored draft content", e);
        }
    }

  // ADD THE GENERATE PDF METHOD HERE INSIDE THE CLASS
@Transactional
    public byte[] generatePdf(CurrentUser user, Long draftId) {
        ExportResponse exportData = export(user, draftId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // 1. Calculate Total Marks from Questions
            int totalMarks = 0;
            if (exportData.questions() != null) {
                for (var q : exportData.questions()) {
                    totalMarks += q.marks();
                }
            }

            // Extract clean username string (e.g. "Officer_1") safely
            String downloaderName = "Authorized Officer";
            if (user != null) {
                String userStr = user.toString();
                if (userStr.contains("username=")) {
                    int start = userStr.indexOf("username=") + 9;
                    int end = userStr.indexOf(",", start);
                    if (end == -1) end = userStr.indexOf(";", start);
                    if (end == -1) end = userStr.indexOf("]", start);
                    if (end > start) {
                        downloaderName = userStr.substring(start, end).trim();
                    }
                } else {
                    downloaderName = userStr;
                }
            }

            // 2. Add Header Section (Title centered, Total Marks & Issued To on the right)
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font headerMetaFont = new Font(Font.HELVETICA, 13, Font.BOLD);
            Font regularFont = new Font(Font.HELVETICA, 11, Font.NORMAL);

            Paragraph titlePara = new Paragraph( exportData.title() != null ? "Subject :"+ exportData.title() : "EXAM PAPER", titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            document.add(titlePara);

            Paragraph metaPara = new Paragraph(
                "Total Marks - " + totalMarks +" marks"+"\n" +
                "Issued to " + downloaderName, 
                headerMetaFont
            );
            metaPara.setAlignment(Element.ALIGN_RIGHT);
            document.add(metaPara);
            
            document.add(new Paragraph("\n")); // Spacing
            document.add(new Paragraph("----------------------------------------------------------------------------------------------------------------------------------"));

            // 3. Add Questions with Marks Distribution format
            if (exportData.questions() != null) {
                for (int i = 0; i < exportData.questions().size(); i++) {
                    var q = exportData.questions().get(i);
                    Paragraph qPara = new Paragraph("Q" + (i + 1) + ". " + q.text() + "  [" + q.marks() + " marks]", regularFont);
                    qPara.setSpacingAfter(10f);
                    document.add(qPara);
                }
            }

            // 4. Add Clean Diagonal Watermark & Audit Stamp in Light Red Color
            String preparedBy = exportData.preparedBy() != null ? exportData.preparedBy() : "—";
            String moderatedBy = exportData.moderatedBy() != null ? exportData.moderatedBy() : "—";
            String approvedBy = exportData.approvedBy() != null ? exportData.approvedBy() : "—";
            String downloadTime = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(java.time.LocalDateTime.now());

            PdfContentByte canvas = writer.getDirectContentUnder();
            canvas.saveState();
            canvas.setColorFill(new Color(255, 127, 127)); // Light red color
            
            com.lowagie.text.pdf.BaseFont bf = com.lowagie.text.pdf.BaseFont.createFont(
                com.lowagie.text.pdf.BaseFont.HELVETICA_BOLD, 
                com.lowagie.text.pdf.BaseFont.WINANSI, 
                com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED
            );

            // Draw clean single-line or compact diagonal audit trail stamp across the center
            float rotationAngle = 45f;
            
            canvas.beginText();
            canvas.setFontAndSize(bf, 38);
            canvas.showTextAligned(Element.ALIGN_CENTER, "SECURE - LOCKED & SEALED", 300f, 480f, rotationAngle);
              canvas.endText();
            canvas.beginText(); 
            canvas.setFontAndSize(bf, 15f);
            canvas.showTextAligned(Element.ALIGN_CENTER, "Prepared by: " + preparedBy + " | Moderated by: " + moderatedBy, 300f, 460f, rotationAngle);
            canvas.showTextAligned(Element.ALIGN_CENTER, "Approved & locked by: " + approvedBy + " | Downloaded by: " + downloaderName, 300f, 442f, rotationAngle);
            canvas.showTextAligned(Element.ALIGN_CENTER, "Timestamp: " + downloadTime, 300f, 424f, rotationAngle);
            canvas.endText();
            
            canvas.restoreState();

            document.close();
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating formatted PDF: " + e.getMessage(), e);
        }
    }
}