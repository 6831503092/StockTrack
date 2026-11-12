package com.sras.backend.dto;

import com.sras.backend.entity.SalesOrder;
import com.sras.backend.entity.SalesOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderDto {
    private Integer id;
    private Integer soId;
    private Integer requestId;
    private Integer customerId;
    private String customer;
    private String customerName;
    private Integer createdByUserId;
    private String createdBy;
    private String userId;
    private LocalDateTime orderDatetime;
    private String date; // "dd/MM/yyyy"
    private Integer totalQty;
    private BigDecimal totalPrice;
    private String status; // "Pending", "Approved", "Rejected", "Issued"
    private List<SalesOrderItemDto> items;

    public static SalesOrderDto fromEntity(SalesOrder so, List<SalesOrderItem> items) {
        if (so == null) return null;

        int totalQty = 0;
        List<SalesOrderItemDto> itemDtos = new ArrayList<>();
        if (items != null) {
            for (SalesOrderItem item : items) {
                totalQty += (item.getQuantity() != null ? item.getQuantity() : 0);
                itemDtos.add(SalesOrderItemDto.fromEntity(item));
            }
        }

        String formattedDate = "";
        if (so.getOrderDatetime() != null) {
            formattedDate = so.getOrderDatetime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        String custName = so.getCustomer() != null ? so.getCustomer().getCustomerName() : "-";
        Integer custId = so.getCustomer() != null ? so.getCustomer().getCustomerId() : null;
        String createdByName = so.getCreatedBy() != null ? so.getCreatedBy().getFullName() : "-";
        Integer creatorUserId = so.getCreatedBy() != null ? so.getCreatedBy().getUserId() : null;

        String displayStatus = so.getStatus() != null ? formatStatus(so.getStatus().name()) : "Pending";

        return SalesOrderDto.builder()
                .id(so.getSoId())
                .soId(so.getSoId())
                .requestId(so.getRequest() != null ? so.getRequest().getRequestId() : null)
                .customerId(custId)
                .customer(custName)
                .customerName(custName)
                .createdByUserId(creatorUserId)
                .createdBy(createdByName)
                .userId(creatorUserId != null ? String.valueOf(creatorUserId) : "")
                .orderDatetime(so.getOrderDatetime())
                .date(formattedDate)
                .totalQty(totalQty)
                .totalPrice(so.getTotalAmount() != null ? so.getTotalAmount() : BigDecimal.ZERO)
                .status(displayStatus)
                .items(itemDtos)
                .build();
    }

    private static String formatStatus(String raw) {
        if (raw == null) return "Pending";
        switch (raw.toUpperCase()) {
            case "PENDING": return "Pending";
            case "APPROVED": return "Approved";
            case "REJECTED": return "Rejected";
            case "ISSUED": return "Issued";
            case "REJECTED_ON_ISSUE": return "Rejected on Issue";
            default: return raw;
        }
    }
}
