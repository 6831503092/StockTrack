package com.sras.backend.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerPayload(
        Integer customerId,
        String customerName,
        String reason
) {}
