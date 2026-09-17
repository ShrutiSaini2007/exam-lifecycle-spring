package com.examlifecycle.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "approvals")
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long draftId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStage stage;

    @Column(nullable = false)
    private Long approverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalDecision decision;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Approval() {}

    public Approval(Long draftId, ApprovalStage stage, Long approverId, ApprovalDecision decision, String comment) {
        this.draftId = draftId;
        this.stage = stage;
        this.approverId = approverId;
        this.decision = decision;
        this.comment = comment;
    }

    public Long getId() { return id; }
    public Long getDraftId() { return draftId; }
    public ApprovalStage getStage() { return stage; }
    public Long getApproverId() { return approverId; }
    public ApprovalDecision getDecision() { return decision; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
