package com.ledgerline.dto;

import com.ledgerline.entity.Role;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(@Size(min = 1, max = 120) String fullName, Role role, Boolean enabled) {}
