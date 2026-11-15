package com.sras.backend.dto;

import com.sras.backend.entity.Product;
import com.sras.backend.entity.Zone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private static final String DEFAULT_IMAGE = "../picture/favicon/favicon.png";

    private static final Map<String, String> PRODUCT_IMAGES = Map.ofEntries(
            Map.entry("officedesk", "../picture/zone1/OfficeDesk.webp"),
            Map.entry("diningtable", "../picture/zone1/DiningTable.webp"),
            Map.entry("coffeetable", "../picture/zone1/cofeetable.webp"),
            Map.entry("computerdesk", "../picture/zone1/computer-desk.webp"),
            Map.entry("meetingtable", "../picture/zone1/meetingtable.webp"),
            Map.entry("officechair", "../picture/zone2/OfficeChair.webp"),
            Map.entry("diningchair", "../picture/zone2/dinningchair.webp"),
            Map.entry("gamingchair", "../picture/zone2/GamingChair.webp"),
            Map.entry("foldingchair", "../picture/zone2/FoldingChair.webp"),
            Map.entry("sofachair", "../picture/zone2/SofaChair.webp"),
            Map.entry("singlebed", "../picture/zone3/SingleBed.webp"),
            Map.entry("queenbed", "../picture/zone3/queenbed.webp"),
            Map.entry("kingbed", "../picture/zone3/KingBed.webp"),
            Map.entry("bunkbed", "../picture/zone3/BunkBed.webp"),
            Map.entry("foldingbed", "../picture/zone3/FoldingBed.webp"),
            Map.entry("wardrobe", "../picture/zone4/Wardrobe.webp"),
            Map.entry("shoecabinet", "../picture/zone4/ShoeCabinet.webp"),
            Map.entry("bookshelf", "../picture/zone4/bookshelf.webp"),
            Map.entry("tvcabinet", "../picture/zone4/TVCabinet.webp"),
            Map.entry("drawercabinet", "../picture/zone4/DrawerCabinet.webp")
    );

    private static final Map<String, String> ZONE_DEFAULT_IMAGES = Map.of(
            "Z1", "../picture/zone1/OfficeDesk.webp",
            "Z2", "../picture/zone2/OfficeChair.webp",
            "Z3", "../picture/zone3/SingleBed.webp",
            "Z4", "../picture/zone4/bookshelf.webp"
    );

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
        if (productName == null) return DEFAULT_IMAGE;
        String clean = productName.toLowerCase().replaceAll("[^a-z0-9]", "");
        String matched = PRODUCT_IMAGES.get(clean);
        if (matched != null) return matched;
        if (zoneCode != null) {
            return ZONE_DEFAULT_IMAGES.getOrDefault(zoneCode.toUpperCase(), DEFAULT_IMAGE);
        }
        return DEFAULT_IMAGE;
    }

    public static ProductDto fromEntity(Product product) {
        if (product == null) return null;

        int stock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int minStock = product.getMinStockAlert() != null ? product.getMinStockAlert() : 20;
        Zone zone = product.getZone();
        String zoneCode = zone != null ? zone.getZoneCode() : "-";

        return ProductDto.builder()
                .id(product.getProductId())
                .name(product.getProductName())
                .location(zoneCode)
                .zoneName(zone != null ? zone.getZoneName() : "-")
                .zoneId(zone != null ? zone.getZoneId() : null)
                .stock(stock)
                .status(stock <= minStock ? "Low Stock" : "In Stock")
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
