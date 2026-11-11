package com.ledgerline.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 40, updatable = false)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, updatable = false)
    private TransactionType transactionType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_account_id", updatable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_account_id", updatable = false)
    private Account destinationAccount;

    @Column(nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TransactionStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 100, updatable = false)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64, updatable = false)
    private String requestFingerprint;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "initiated_by", nullable = false, updatable = false)
    private User initiatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String r) { this.transactionReference = r; }
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType t) { this.transactionType = t; }
    public Account getSourceAccount() { return sourceAccount; }
    public void setSourceAccount(Account a) { this.sourceAccount = a; }
    public Account getDestinationAccount() { return destinationAccount; }
    public void setDestinationAccount(Account a) { this.destinationAccount = a; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal a) { this.amount = a; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus s) { this.status = s; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String k) { this.idempotencyKey = k; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public void setRequestFingerprint(String f) { this.requestFingerprint = f; }
    public User getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(User u) { this.initiatedBy = u; }
    public Instant getCreatedAt() { return createdAt; }
}
