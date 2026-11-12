package com.sras.backend.dto;

import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.entity.PurchaseOrderItem;
import com.sras.backend.entity.Supplier;
import com.sras.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderDto {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Integer id;
    private Integer poId;
    private Integer requestId;
    private Integer supplierId;
    private String supplier;
    private Integer createdByUserId;
    private String createdBy;
    private String userId;
    private LocalDateTime orderDatetime;
    private String date; // "dd/MM/yyyy"
    private Integer totalQty;
    private BigDecimal totalPrice;
    private String status; // "Pending", "Approved", "Rejected", "Received"
    private List<PurchaseOrderItemDto> items;

    public static PurchaseOrderDto fromEntity(PurchaseOrder po, List<PurchaseOrderItem> items) {
        if (po == null) return null;

        List<PurchaseOrderItem> safeItems = items != null ? items : List.of();
        int totalQty = safeItems.stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
        List<PurchaseOrderItemDto> itemDtos = safeItems.stream()
                .map(PurchaseOrderItemDto::fromEntity)
                .toList();

        Supplier supplier = po.getSupplier();
        User creator = po.getCreatedBy();
        Integer creatorUserId = creator != null ? creator.getUserId() : null;

        return PurchaseOrderDto.builder()
                .id(po.getPoId())
                .poId(po.getPoId())
                .requestId(po.getRequest() != null ? po.getRequest().getRequestId() : null)
                .supplierId(supplier != null ? supplier.getSupplierId() : null)
                .supplier(supplier != null ? supplier.getCompanyName() : "-")
                .createdByUserId(creatorUserId)
                .createdBy(creator != null ? creator.getFullName() : "-")
                .userId(creatorUserId != null ? String.valueOf(creatorUserId) : "")
                .orderDatetime(po.getOrderDatetime())
                .date(po.getOrderDatetime() != null ? po.getOrderDatetime().format(DATE_FMT) : "")
                .totalQty(totalQty)
                .totalPrice(po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                .status(po.getStatus() != null ? formatStatus(po.getStatus().name()) : "Pending")
                .items(itemDtos)
                .build();
    }

    private static String formatStatus(String raw) {
        if (raw == null) return "Pending";
        return switch (raw.toUpperCase()) {
            case "PENDING" -> "Pending";
            case "APPROVED" -> "Approved";
            case "REJECTED" -> "Rejected";
            case "RECEIVED" -> "Received";
            case "REJECTED_ON_RECEIPT" -> "Rejected on Receipt";
            default -> raw;
        };
    }
}
