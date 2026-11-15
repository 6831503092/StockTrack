package com.sras.backend.dto;

import com.sras.backend.entity.Product;
import com.sras.backend.entity.SalesOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderItemDto {
    private Integer soItemId;
    private Integer productId;
    private String productName;
    private String zoneCode;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal unitCostSnapshot;
    private BigDecimal subtotal;

    public static SalesOrderItemDto fromEntity(SalesOrderItem item) {
        if (item == null) return null;
        Product prod = item.getProduct();
        String prodName = prod != null ? prod.getProductName() : "";
        String zone = (prod != null && prod.getZone() != null) ? prod.getZone().getZoneCode() : "";

        return SalesOrderItemDto.builder()
                .soItemId(item.getSoItemId())
                .productId(prod != null ? prod.getProductId() : null)
                .productName(prodName)
                .zoneCode(zone)
                .imageUrl(ProductDto.resolveImageUrl(prodName, zone))
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .unitCostSnapshot(item.getUnitCostSnapshot())
                .subtotal(item.getSubtotal())
                .build();
    }
}
