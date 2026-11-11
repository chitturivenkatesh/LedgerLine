package com.ledgerline.dto;

import com.ledgerline.entity.PermissionType;
import jakarta.validation.constraints.NotNull;

public record PermissionRequest(@NotNull Long userId, @NotNull PermissionType permissionType) {}
