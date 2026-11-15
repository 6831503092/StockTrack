package com.sras.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneProductSummaryDto {
    private long activeZonesCount;
    private long storedUnits;
    private long totalProducts;
    private long lowStockCount;
    private long totalCapacity;
}
