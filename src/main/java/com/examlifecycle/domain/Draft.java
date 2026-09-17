package com.examlifecycle.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "drafts")
public class Draft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long blueprintId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftStatus status = DraftStatus.DRAFT;

    @Column(nullable = false)
    private Integer currentVersion = 1;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // Populated only once LOCKED — this is the tamper-evident seal over
    // {contentHash, lockedBy, lockedAt}, not just the paper content itself.
    private String finalHash;
    private Instant lockedAt;
    private Long lockedBy;

    public Draft() {}

    public Draft(Long blueprintId, String title, Long createdBy) {
        this.blueprintId = blueprintId;
        this.title = title;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public Long getBlueprintId() { return blueprintId; }
    public String getTitle() { return title; }
    public DraftStatus getStatus() { return status; }
    public void setStatus(DraftStatus status) { this.status = status; }
    public Integer getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public String getFinalHash() { return finalHash; }
    public void setFinalHash(String finalHash) { this.finalHash = finalHash; }
    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }
    public Long getLockedBy() { return lockedBy; }
    public void setLockedBy(Long lockedBy) { this.lockedBy = lockedBy; }
}
