package com.ledgerline.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Amount rules (>0, max 2 decimals) are enforced in the service so refused attempts are recorded. */
public record DepositRequest(@NotNull Long accountId, @NotNull BigDecimal amount) {}
