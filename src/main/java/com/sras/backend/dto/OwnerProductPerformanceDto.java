package com.sras.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerProductPerformanceDto {
    private Integer rank;
    private Integer productId;
    private String productName;
    private String zoneName;
    private Integer quantity;
    private String amount;
}
