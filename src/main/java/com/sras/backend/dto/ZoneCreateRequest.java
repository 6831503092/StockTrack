package com.sras.backend.dto;

public record ZoneCreateRequest(
        String zoneCode,
        String zoneName,
        Integer capacity
) {}
