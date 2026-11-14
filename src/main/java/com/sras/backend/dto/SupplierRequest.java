package com.sras.backend.dto;

public record SupplierRequest(
        String companyName,
        String zoneCode,
        String category
) {}
