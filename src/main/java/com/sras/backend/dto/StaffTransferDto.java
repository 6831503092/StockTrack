package com.sras.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffTransferDto {
    private Integer movementId;
    private String code;
    private String reference;
    private String requestedBy;
    private String date;
    private Integer productId;
    private String productName;
    private Integer fromZoneId;
    private String fromZoneName;
    private Integer toZoneId;
    private String toZoneName;
    private Integer quantity;
    private String status;
}
