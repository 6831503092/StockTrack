package com.sras.backend.dto;

import com.sras.backend.entity.PurchaseOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemDto {
    private Integer poItemId;
    private Integer productId;
    private String productName;
    private String zoneCode;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public static PurchaseOrderItemDto fromEntity(PurchaseOrderItem item) {
        if (item == null) return null;
        String zone = "";
        String prodName = "";
        Integer prodId = null;
        if (item.getProduct() != null) {
            prodId = item.getProduct().getProductId();
            prodName = item.getProduct().getProductName();
            if (item.getProduct().getZone() != null) {
                zone = item.getProduct().getZone().getZoneCode();
            }
        }
        return PurchaseOrderItemDto.builder()
                .poItemId(item.getPoItemId())
                .productId(prodId)
                .productName(prodName)
                .zoneCode(zone)
                .imageUrl(ProductDto.resolveImageUrl(prodName, zone))
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitCost())
                .subtotal(item.getSubtotal())
                .build();
    }
}
