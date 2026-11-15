package com.sras.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank(message = "Company name is required")
        String companyName,
        String zoneCode,
        String category
) {}
