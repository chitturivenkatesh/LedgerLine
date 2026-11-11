package com.ledgerline.dto;

import com.ledgerline.entity.*;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionDTO(Long id, String transactionReference, TransactionType transactionType, BigDecimal amount,
                             TransactionStatus status, Long sourceAccountId, String sourceAccountNumber,
                             Long destinationAccountId, String destinationAccountNumber,
                             Long initiatedById, String initiatedBy, Instant createdAt,
                             BigDecimal sourceBalanceAfter, BigDecimal destinationBalanceAfter) {}
