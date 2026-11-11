package com.ledgerline.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WithdrawRequest(@NotNull Long accountId, @NotNull BigDecimal amount) {}
