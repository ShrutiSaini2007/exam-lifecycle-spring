package com.examlifecycle.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long blueprintId;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    private String topic;

    @Column(nullable = false)
    private Integer marks;

    private String difficulty;

    @Column(nullable = false)
    private Long importedBy;

    @Column(nullable = false)
    private Instant importedAt = Instant.now();

    public Question() {}

    public Question(Long blueprintId, String text, String topic, Integer marks, String difficulty, Long importedBy) {
        this.blueprintId = blueprintId;
        this.text = text;
        this.topic = topic;
        this.marks = marks;
        this.difficulty = difficulty;
        this.importedBy = importedBy;
    }

    public Long getId() { return id; }
    public Long getBlueprintId() { return blueprintId; }
    public String getText() { return text; }
    public String getTopic() { return topic; }
    public Integer getMarks() { return marks; }
    public String getDifficulty() { return difficulty; }
    public Long getImportedBy() { return importedBy; }
    public Instant getImportedAt() { return importedAt; }
}
