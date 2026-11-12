package com.sras.backend.dto;

import com.sras.backend.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Integer id;
    private String name;
    private String location;
    private String zoneName;
    private Integer zoneId;
    private Integer stock;
    private String status;
    private BigDecimal defaultCost;
    private BigDecimal sellingPrice;
    private Integer minStockAlert;
    private Integer poMinQty;
    private Integer poMaxQty;
    private Integer soMinQty;
    private Integer soMaxQty;
    private Boolean isActive;

    public static ProductDto fromEntity(Product product) {
        if (product == null) {
            return null;
        }
        int stock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int minStock = product.getMinStockAlert() != null ? product.getMinStockAlert() : 20;
        String status = stock <= minStock ? "Low Stock" : "In Stock";

        return ProductDto.builder()
                .id(product.getProductId())
                .name(product.getProductName())
                .location(product.getZone() != null ? product.getZone().getZoneCode() : "-")
                .zoneName(product.getZone() != null ? product.getZone().getZoneName() : "-")
                .zoneId(product.getZone() != null ? product.getZone().getZoneId() : null)
                .stock(stock)
                .status(status)
                .defaultCost(product.getDefaultCost())
                .sellingPrice(product.getSellingPrice())
                .minStockAlert(minStock)
                .poMinQty(product.getPoMinQty())
                .poMaxQty(product.getPoMaxQty())
                .soMinQty(product.getSoMinQty())
                .soMaxQty(product.getSoMaxQty())
                .isActive(product.getIsActive())
                .build();
    }
}
