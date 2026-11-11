package com.ledgerline.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.Instant;

@Entity
@Immutable
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", updatable = false)
    private Long userId;
    @Column(nullable = false, length = 60, updatable = false)
    private String action;
    @Column(name = "entity_type", nullable = false, length = 60, updatable = false)
    private String entityType;
    @Column(name = "entity_id", length = 60, updatable = false)
    private String entityId;
    @Column(columnDefinition = "TEXT", updatable = false)
    private String details;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String v) { this.entityType = v; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String v) { this.entityId = v; }
    public String getDetails() { return details; }
    public void setDetails(String v) { this.details = v; }
    public Instant getCreatedAt() { return createdAt; }
}
