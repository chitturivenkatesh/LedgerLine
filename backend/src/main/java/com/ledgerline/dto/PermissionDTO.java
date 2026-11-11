package com.ledgerline.dto;

import com.ledgerline.entity.PermissionType;

public record PermissionDTO(Long id, Long userId, String userName, Long accountId, PermissionType permissionType) {}
