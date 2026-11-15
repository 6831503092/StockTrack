package com.sras.backend.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupplierPayload(
        Integer supplierId,
        String companyName,
        String category,
        String zoneCode,
        String reason
) {}
