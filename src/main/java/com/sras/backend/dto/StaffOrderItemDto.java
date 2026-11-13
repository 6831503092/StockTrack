package com.sras.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOrderItemDto {
    private Integer itemId;
    private Integer productId;
    private String productName;
    private String zoneName;
    private Integer expectedQty;
    private BigDecimal unitPriceOrCost;
}
