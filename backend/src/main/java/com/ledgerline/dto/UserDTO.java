package com.ledgerline.dto;

import com.ledgerline.entity.Role;
import com.ledgerline.entity.User;
import java.time.Instant;

public record UserDTO(Long id, String fullName, String email, Role role, boolean enabled, Instant createdAt) {
    public static UserDTO from(User u) {
        return new UserDTO(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isEnabled(), u.getCreatedAt());
    }
}
