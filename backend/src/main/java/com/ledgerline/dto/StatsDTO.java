package com.ledgerline.dto;

import java.math.BigDecimal;

public record StatsDTO(long totalUsers, long totalAccounts, long totalTransactions, long failedAttempts,
                       long ledgerEntries, BigDecimal totalBalance) {}
