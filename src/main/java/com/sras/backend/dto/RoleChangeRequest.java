package com.sras.backend.dto;

public record RoleChangeRequest(
        Integer userId,
        String newRole,
        String reason
) {}
