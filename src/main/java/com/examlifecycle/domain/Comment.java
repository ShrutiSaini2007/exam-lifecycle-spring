package com.examlifecycle.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long draftVersionId;

    @Column(nullable = false)
    private Long commenterId;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Comment() {}

    public Comment(Long draftVersionId, Long commenterId, String text) {
        this.draftVersionId = draftVersionId;
        this.commenterId = commenterId;
        this.text = text;
    }

    public Long getId() { return id; }
    public Long getDraftVersionId() { return draftVersionId; }
    public Long getCommenterId() { return commenterId; }
    public String getText() { return text; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public Instant getCreatedAt() { return createdAt; }
}
