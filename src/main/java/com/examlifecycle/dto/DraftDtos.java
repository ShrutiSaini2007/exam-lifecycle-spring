package com.examlifecycle.dto;

import com.examlifecycle.domain.ApprovalDecision;
import com.examlifecycle.domain.ApprovalStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public class DraftDtos {

    public record CreateDraftRequest(
            @NotNull(message = "blueprintId is required") Long blueprintId,
            @NotBlank(message = "title is required") String title,
            @NotEmpty(message = "questionIds must not be empty") List<Long> questionIds) {}

    public record NewVersionRequest(@NotEmpty(message = "questionIds must not be empty") List<Long> questionIds) {}

    public record CommentRequest(@NotBlank(message = "text is required") String text) {}

    public record ApproveRequest(
            @NotNull(message = "stage is required") ApprovalStage stage,
            @NotNull(message = "decision is required") ApprovalDecision decision,
            String comment) {}

    /** Question shape as returned to reviewers/export — same fields the front end already expects. */
    public record QuestionView(Long id, String text, String topic, Integer marks, String difficulty) {}

    public record DraftDetailResponse(
            Long id, Long blueprintId, String title, com.examlifecycle.domain.DraftStatus status,
            Integer currentVersion, Long createdBy, Instant createdAt,
            String finalHash, Instant lockedAt,
            List<com.examlifecycle.domain.DraftVersion> versions,
            List<com.examlifecycle.domain.Approval> approvals) {}

    public record CommentView(
            Long id, Long draftVersionId, Long commenterId, String commenterUsername,
            String text, boolean resolved, Instant createdAt) {}

    public record AuditLogView(
            Long id, Long userId, String username, String action, String entityType,
            Long entityId, String details, String ip, Instant createdAt) {}

    public record DraftContentResponse(
            Long draftId, Integer version, String contentHash,
            List<QuestionView> questions, int totalMarks,
            String preparedBy, Instant preparedAt) {}

    public record ExportResponse(
            Long id, String title, String finalHash, String watermark,
            List<QuestionView> questions,
            String issuedTo, Instant issuedAt,
            String preparedBy, Instant preparedAt,
            String moderatedBy, Instant moderatedAt,
            String approvedBy, Instant approvedAt) {}
}
