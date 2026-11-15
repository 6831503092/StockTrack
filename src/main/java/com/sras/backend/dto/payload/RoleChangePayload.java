package com.sras.backend.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoleChangePayload(
        Integer userId,
        String oldRole,
        String newRole,
        String reason
) {}
