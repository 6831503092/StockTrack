package com.sras.backend.dto;

import jakarta.validation.constraints.Positive;

public record ZoneUpdateRequest(
        String zoneName,
        @Positive(message = "Capacity must be positive")
        Integer capacity
) {}
