package com.sras.backend.dto;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        Integer productId,
        String productName,
        BigDecimal defaultCost,
        BigDecimal sellingPrice,
        Integer currentStock,
        Integer minStockAlert
) {
    public ProductUpdateRequest(Integer productId, String productName, BigDecimal sellingPrice, Integer currentStock) {
        this(productId, productName, null, sellingPrice, currentStock, null);
    }
}
