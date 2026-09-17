package com.examlifecycle.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // nullable: e.g. a failed login has no known user

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String entityType;

    private Long entityId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String details; // JSON string

    private String ip;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public AuditLog() {}

    public AuditLog(Long userId, String action, String entityType, Long entityId, String details, String ip) {
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.ip = ip;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getDetails() { return details; }
    public String getIp() { return ip; }
    public Instant getCreatedAt() { return createdAt; }
}
