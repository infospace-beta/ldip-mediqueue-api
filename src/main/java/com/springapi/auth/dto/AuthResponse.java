package com.springapi.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
