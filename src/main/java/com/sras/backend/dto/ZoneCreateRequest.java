package com.sras.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ZoneCreateRequest(
        @NotBlank(message = "Zone code is required")
        String zoneCode,
        @NotBlank(message = "Zone name is required")
        String zoneName,
        @Positive(message = "Capacity must be positive")
        Integer capacity
) {}
