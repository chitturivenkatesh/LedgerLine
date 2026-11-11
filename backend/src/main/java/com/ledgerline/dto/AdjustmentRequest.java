package com.ledgerline.dto;

import com.ledgerline.entity.AdjustmentDirection;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AdjustmentRequest(@NotNull Long accountId, @NotNull BigDecimal amount,
                                @NotNull AdjustmentDirection direction,
                                @NotBlank @Size(max = 200) String reason) {}
