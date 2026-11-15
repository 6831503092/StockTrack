package com.sras.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOrderDto {
    private Integer id;
    private String code;
    private String reference;
    private String requester;
    private String partyName;
    private String date;
    private BigDecimal totalAmount;
    private String status;
    private List<StaffOrderItemDto> items;
}
