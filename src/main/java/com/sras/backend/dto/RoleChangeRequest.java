package com.sras.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(
        @NotNull(message = "User ID is required")
        Integer userId,
        @NotBlank(message = "New role is required")
        String newRole,
        String reason
) {}
