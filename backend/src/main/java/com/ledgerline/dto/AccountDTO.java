package com.ledgerline.dto;

import com.ledgerline.entity.*;
import java.math.BigDecimal;
import java.time.Instant;

public record AccountDTO(Long id, String accountNumber, AccountType accountType, BigDecimal balance,
                         AccountStatus status, Long holderId, String holderName, Instant createdAt) {}
