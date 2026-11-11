package com.ledgerline.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TransferRequest(@NotNull Long sourceAccountId,
                              @NotBlank @Size(max = 20) String destinationAccountNumber,
                              @NotNull BigDecimal amount) {}
