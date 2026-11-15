package com.sras.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @JsonAlias({"fullName", "username"})
        String userId,
        @NotBlank(message = "Password is required") String password
) {
    public String getIdentifier() {
        return userId != null ? userId.trim() : "";
    }
}
