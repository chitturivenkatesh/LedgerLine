package com.ledgerline.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Immutable
@Table(name = "transaction_attempts")
public class TransactionAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "request_reference", nullable = false, length = 40, updatable = false)
    private String requestReference;
    @Column(name = "account_id", updatable = false)
    private Long accountId;
    @Column(name = "initiated_by", updatable = false)
    private Long initiatedBy;
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, updatable = false)
    private TransactionType transactionType;
    @Column(precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;
    @Column(name = "failure_reason", nullable = false, length = 255, updatable = false)
    private String failureReason;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getRequestReference() { return requestReference; }
    public void setRequestReference(String v) { this.requestReference = v; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long v) { this.accountId = v; }
    public Long getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(Long v) { this.initiatedBy = v; }
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType v) { this.transactionType = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String v) { this.failureReason = v; }
    public Instant getCreatedAt() { return createdAt; }
}
