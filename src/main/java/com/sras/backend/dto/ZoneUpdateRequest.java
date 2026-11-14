package com.sras.backend.dto;

public record ZoneUpdateRequest(
        String zoneName,
        Integer capacity
) {}
