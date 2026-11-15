package com.sras.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank(message = "Customer name is required")
        String customerName
) {}
