package com.sras.backend.dto;

import com.sras.backend.entity.Zone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneDto {
    private Integer zoneId;
    private String zoneCode;
    private String zoneName;
    private Long productCount;
    private Long totalUnits;
    private Integer capacity;

    public static ZoneDto fromEntity(Zone zone, Long productCount, Long totalUnits) {
        if (zone == null) {
            return null;
        }
        return ZoneDto.builder()
                .zoneId(zone.getZoneId())
                .zoneCode(zone.getZoneCode())
                .zoneName(zone.getZoneName())
                .productCount(productCount != null ? productCount : 0L)
                .totalUnits(totalUnits != null ? totalUnits : 0L)
                .capacity(500)
                .build();
    }
}
