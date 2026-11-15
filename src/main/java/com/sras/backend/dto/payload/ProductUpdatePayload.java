package com.sras.backend.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductUpdatePayload(
        Integer productId,
        String productName,
        BigDecimal sellingPrice,
        BigDecimal defaultCost,
        Integer currentStock,
        Integer minStockAlert,
        String reason
) {}
