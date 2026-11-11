package com.ledgerline.dto;

public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserDTO user) {}
