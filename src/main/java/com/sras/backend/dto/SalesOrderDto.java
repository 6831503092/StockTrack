package com.sras.backend.dto;

import com.sras.backend.entity.Customer;
import com.sras.backend.entity.SalesOrder;
import com.sras.backend.entity.SalesOrderItem;
import com.sras.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderDto {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

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
    private String date; // "dd MMM yyyy, HH:mm"
    private Integer totalQty;
    private BigDecimal totalPrice;
    private String status; // "Pending", "Approved", "Rejected", "Issued"
    private String decisionReason;
    private String decidedByName;
    private LocalDateTime decidedDatetime;
    private List<SalesOrderItemDto> items;

    public static SalesOrderDto fromEntity(SalesOrder so, List<SalesOrderItem> items) {
        if (so == null) return null;

        List<SalesOrderItem> safeItems = items != null ? items : List.of();
        int totalQty = safeItems.stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
        List<SalesOrderItemDto> itemDtos = safeItems.stream()
                .map(SalesOrderItemDto::fromEntity)
                .toList();

        Customer cust = so.getCustomer();
        String custName = cust != null ? cust.getCustomerName() : "-";
        User creator = so.getCreatedBy();
        Integer creatorUserId = creator != null ? creator.getUserId() : null;

        var appReq = so.getRequest();
        String decisionReason = appReq != null ? appReq.getDecisionReason() : null;
        String decidedByName = (appReq != null && appReq.getDecidedBy() != null)
                ? appReq.getDecidedBy().getFullName() : null;
        LocalDateTime decidedDatetime = appReq != null ? appReq.getDecidedDatetime() : null;

        return SalesOrderDto.builder()
                .id(so.getSoId())
                .soId(so.getSoId())
                .requestId(appReq != null ? appReq.getRequestId() : null)
                .customerId(cust != null ? cust.getCustomerId() : null)
                .customer(custName)
                .customerName(custName)
                .createdByUserId(creatorUserId)
                .createdBy(creator != null ? creator.getFullName() : "-")
                .userId(creatorUserId != null ? String.valueOf(creatorUserId) : "")
                .orderDatetime(so.getOrderDatetime())
                .date(so.getOrderDatetime() != null ? so.getOrderDatetime().format(DATE_FMT) : "")
                .totalQty(totalQty)
                .totalPrice(so.getTotalAmount() != null ? so.getTotalAmount() : BigDecimal.ZERO)
                .status(so.getStatus() != null ? formatStatus(so.getStatus().name()) : "Pending")
                .decisionReason(decisionReason)
                .decidedByName(decidedByName)
                .decidedDatetime(decidedDatetime)
                .items(itemDtos)
                .build();
    }

    private static String formatStatus(String raw) {
        if (raw == null) return "Pending";
        return switch (raw.toUpperCase()) {
            case "PENDING" -> "Pending";
            case "APPROVED" -> "Approved";
            case "REJECTED" -> "Rejected";
            case "ISSUED", "STOCK_OUT" -> "Stock Out";
            case "REJECTED_ON_ISSUE", "REJECTED_ON_STOCK_OUT" -> "Rejected on Stock Out";
            default -> raw;
        };
    }
}
