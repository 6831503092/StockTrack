package com.sras.backend.dto;

import java.util.List;

public record ProductMoveRequest(
        String sourceZoneCode,
        String targetZoneCode,
        Integer targetZoneId,
        List<Integer> productIds,
        String reason
) {
    public ProductMoveRequest(String targetZoneCode, Integer targetZoneId, List<Integer> productIds, String reason) {
        this(null, targetZoneCode, targetZoneId, productIds, reason);
    }
}
