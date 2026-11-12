package com.sras.backend.dto;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        Integer productId,
        String productName,
        BigDecimal sellingPrice,
        Integer currentStock
) {}
