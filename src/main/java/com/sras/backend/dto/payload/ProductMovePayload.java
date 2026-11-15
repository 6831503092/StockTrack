package com.sras.backend.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductMovePayload(
        String operation,
        Integer sourceZoneId,
        String sourceZoneCode,
        String sourceZoneName,
        Integer targetZoneId,
        String targetZoneCode,
        String targetZoneName,
        Integer productId,
        List<ProductMoveItemPayload> sourceProducts,
        List<ProductMoveItemPayload> targetProducts,
        List<ProductMoveItemPayload> products,
        String reason
) {}
