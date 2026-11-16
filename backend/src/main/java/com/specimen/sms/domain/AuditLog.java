package com.specimen.sms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_log", indexes = @Index(name = "idx_audit_entity", columnList = "entityType,entityId"))
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 48)
    private String action;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false, length = 64)
    private String performedBy;

    @Column(nullable = false)
    private Instant performedAt;

    protected AuditLog() {
    }

    public AuditLog(String entityType, Long entityId, String action, String details, String performedBy) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.details = details;
        this.performedBy = performedBy;
        this.performedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }
}
