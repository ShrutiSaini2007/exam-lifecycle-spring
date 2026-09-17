package com.examlifecycle.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "draft_versions", uniqueConstraints = @UniqueConstraint(columnNames = {"draft_id", "version_number"}))
public class DraftVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "draft_id", nullable = false)
    private Long draftId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    // JSON snapshot: {"questionIds":[1,2,3]} — canonicalized before hashing.
    // @JsonIgnore: the version-history listing (GET /api/drafts/{id}) should
    // show hash + metadata only, matching the Python prototype. Use
    // GET /api/drafts/{id}/content (or /export once locked) to actually read
    // the paper — that route is also what gets audit-logged as a content read.
    @JsonIgnore
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentJson;

    @Column(nullable = false)
    private String contentHash;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public DraftVersion() {}

    public DraftVersion(Long draftId, Integer versionNumber, String contentJson, String contentHash, Long createdBy) {
        this.draftId = draftId;
        this.versionNumber = versionNumber;
        this.contentJson = contentJson;
        this.contentHash = contentHash;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public Long getDraftId() { return draftId; }
    public Integer getVersionNumber() { return versionNumber; }
    public String getContentJson() { return contentJson; }
    public String getContentHash() { return contentHash; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
