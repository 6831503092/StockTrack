package com.sras.backend.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductMoveItemPayload(
        Integer productId,
        String productName,
        String fromZoneCode,
        String toZoneCode,
        Integer stock
) {}
