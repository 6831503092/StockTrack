package com.sras.backend.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserCreatePayload(
        Integer userId,
        String fullName,
        String role,
        String passwordHash,
        String reason
) {}
