package com.ledgerline.service;

import com.ledgerline.entity.AdjustmentDirection;
import com.ledgerline.entity.TransactionType;
import java.math.BigDecimal;

/**
 * accountId: deposit target / withdrawal source / transfer source / adjustment target.
 * rawAmount is what the client sent; it is validated in TransactionService.
 */
public record TransactionCommand(TransactionType type, Long actorId, Long accountId, String counterpartyNumber,
                                 AdjustmentDirection direction, String reason, BigDecimal rawAmount, String idempotencyKey) {}
