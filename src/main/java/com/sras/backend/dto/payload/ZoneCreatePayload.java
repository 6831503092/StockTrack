package com.sras.backend.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ZoneCreatePayload(
        String zoneCode,
        String zoneName,
        Integer capacity,
        String reason
) {}
