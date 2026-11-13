package com.sras.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffActivityItemDto {
    private String date;
    private String operation;
    private String product;
    private Integer quantity;
    private String status;
    private String reference;
}
