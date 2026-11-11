package com.ledgerline.dto;

import com.ledgerline.entity.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public record AttemptDTO(Long id, String requestReference, Long accountId, Long initiatedBy,
                         TransactionType transactionType, BigDecimal amount, String failureReason, Instant createdAt) {}
