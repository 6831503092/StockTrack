package com.sras.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderCreateRequest {
    private Integer customerId;
    private String customerName;
    private List<SalesOrderItemRequest> items;
    private String note;
}
