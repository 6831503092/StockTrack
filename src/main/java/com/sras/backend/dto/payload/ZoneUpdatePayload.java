package com.sras.backend.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ZoneUpdatePayload(
        Integer zoneId,
        String zoneCode,
        String zoneName,
        String oldZoneName,
        Integer capacity,
        Integer oldCapacity,
        Integer storedUnits,
        String reason
) {}
