package com.examlifecycle.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "blueprints")
public class Blueprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private String title;

    // Stores a JSON array like [{"topic":"DBMS","marks":20,"count":4}, ...].
    // Kept as raw JSON (via the shared ObjectMapper in the service layer)
    // rather than a child entity table — mirrors the Python prototype's
    // schema exactly and keeps the marks-distribution shape flexible.
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String sectionsJson;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Blueprint() {}

    public Blueprint(Long courseId, String title, String sectionsJson, Long createdBy) {
        this.courseId = courseId;
        this.title = title;
        this.sectionsJson = sectionsJson;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public String getSectionsJson() { return sectionsJson; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
