package com.sras.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        String userId,
        String username,
        @NotBlank(message = "Password is required") String password
) {
    public String getIdentifier() {
        if (userId != null && !userId.isBlank()) {
            return userId.trim();
        }
        if (username != null && !username.isBlank()) {
            return username.trim();
        }
        return "";
    }
}
