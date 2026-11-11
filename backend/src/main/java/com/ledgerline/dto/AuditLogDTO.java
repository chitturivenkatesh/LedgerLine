package com.ledgerline.dto;

import java.time.Instant;

public record AuditLogDTO(Long id, Long userId, String action, String entityType, String entityId, String details, Instant createdAt) {}
