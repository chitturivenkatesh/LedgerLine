package com.ledgerline.dto;

import com.ledgerline.entity.*;
import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryDTO(Long id, Long transactionId, String transactionReference, TransactionType transactionType,
                             Long accountId, String accountNumber, EntryType entryType, BigDecimal amount,
                             BigDecimal balanceAfter, String initiatedBy, Instant createdAt) {}
