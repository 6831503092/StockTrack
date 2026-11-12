package com.sras.backend.dto;

import com.sras.backend.entity.Supplier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDto {
    private Integer supplierId;
    private String companyName;
    private String zoneCode;
    private String zoneName;
    private String category;
    private String displayName;
    private Boolean isActive;

    public static SupplierDto fromEntity(Supplier supplier) {
        if (supplier == null) return null;
        String zCode = supplier.getZoneCode();
        String cat = supplier.getCategory();

        // Fallback default zone mapping if not set
        if (zCode == null && supplier.getCompanyName() != null) {
            switch (supplier.getCompanyName().trim()) {
                case "JRScomp." -> { zCode = "Z1"; cat = "Tables"; }
                case "Thai Wood Supply Co." -> { zCode = "Z2"; cat = "Chairs"; }
                case "Modern Steel Ltd." -> { zCode = "Z3"; cat = "Beds"; }
                case "Decor Fabric Asia" -> { zCode = "Z4"; cat = "Cabinets"; }
            }
        }
        String zName = zCode != null ? ("Zone " + zCode.replace("Z", "")) : null;

        return SupplierDto.builder()
                .supplierId(supplier.getSupplierId())
                .companyName(supplier.getCompanyName())
                .zoneCode(zCode)
                .zoneName(zName)
                .category(cat)
                .displayName(supplier.getCompanyName())
                .isActive(supplier.getIsActive())
                .build();
    }
}
