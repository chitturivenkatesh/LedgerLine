package com.ledgerline.dto;

import com.ledgerline.entity.AccountType;
import jakarta.validation.constraints.NotNull;

/** holderId is honoured only for ADMIN callers; ACCOUNT_HOLDER callers always get an account for themselves. */
public record CreateAccountRequest(@NotNull AccountType accountType, Long holderId) {}
