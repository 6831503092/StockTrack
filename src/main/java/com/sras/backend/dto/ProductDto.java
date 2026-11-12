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
    private String imageUrl;
    private Boolean isActive;

    public static String resolveImageUrl(String productName, String zoneCode) {
        if (productName == null) return "../picture/favicon/favicon.png";
        String clean = productName.toLowerCase().replaceAll("[^a-z0-9]", "");
        switch (clean) {
            case "officedesk": return "../picture/zone1/OfficeDesk.webp";
            case "diningtable": return "../picture/zone1/DiningTable.webp";
            case "coffeetable": return "../picture/zone1/cofeetable.webp";
            case "computerdesk": return "../picture/zone1/computer-desk.webp";
            case "meetingtable": return "../picture/zone1/meetingtable.webp";
            case "officechair": return "../picture/zone2/OfficeChair.webp";
            case "diningchair": return "../picture/zone2/dinningchair.webp";
            case "gamingchair": return "../picture/zone2/GamingChair.webp";
            case "foldingchair": return "../picture/zone2/FoldingChair.webp";
            case "sofachair": return "../picture/zone2/SofaChair.webp";
            case "singlebed": return "../picture/zone3/SingleBed.webp";
            case "queenbed": return "../picture/zone3/queenbed.webp";
            case "kingbed": return "../picture/zone3/KingBed.webp";
            case "bunkbed": return "../picture/zone3/BunkBed.webp";
            case "foldingbed": return "../picture/zone3/FoldingBed.webp";
            case "wardrobe": return "../picture/zone4/Wardrobe.webp";
            case "shoecabinet": return "../picture/zone4/ShoeCabinet.webp";
            case "bookshelf": return "../picture/zone4/bookshelf.webp";
            case "tvcabinet": return "../picture/zone4/TVCabinet.webp";
            case "drawercabinet": return "../picture/zone4/DrawerCabinet.webp";
            default:
                if (zoneCode != null) {
                    if (zoneCode.equalsIgnoreCase("Z1")) return "../picture/zone1/OfficeDesk.webp";
                    if (zoneCode.equalsIgnoreCase("Z2")) return "../picture/zone2/OfficeChair.webp";
                    if (zoneCode.equalsIgnoreCase("Z3")) return "../picture/zone3/SingleBed.webp";
                    if (zoneCode.equalsIgnoreCase("Z4")) return "../picture/zone4/bookshelf.webp";
                }
                return "../picture/favicon/favicon.png";
        }
    }

    public static ProductDto fromEntity(Product product) {
        if (product == null) {
            return null;
        }
        int stock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int minStock = product.getMinStockAlert() != null ? product.getMinStockAlert() : 20;
        String status = stock <= minStock ? "Low Stock" : "In Stock";
        String zoneCode = product.getZone() != null ? product.getZone().getZoneCode() : "-";

        return ProductDto.builder()
                .id(product.getProductId())
                .name(product.getProductName())
                .location(zoneCode)
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
                .imageUrl(resolveImageUrl(product.getProductName(), zoneCode))
                .isActive(product.getIsActive())
                .build();
    }
}
