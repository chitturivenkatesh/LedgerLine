package com.ledgerline.dto;

import com.ledgerline.entity.Role;
import jakarta.validation.constraints.*;

public record AdminCreateUserRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role) {}
