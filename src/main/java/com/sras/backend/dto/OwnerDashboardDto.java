package com.sras.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDashboardDto {
    private String totalRevenue;
    private String inventoryValue;
    private String totalSale;
    private String totalExport;
    private String totalProfit;
    private List<OwnerProductPerformanceDto> hotSellers;
    private List<OwnerProductPerformanceDto> topImports;
}
