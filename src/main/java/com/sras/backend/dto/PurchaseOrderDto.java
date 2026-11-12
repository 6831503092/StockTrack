package com.sras.backend.dto;

import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.entity.PurchaseOrderItem;
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
public class PurchaseOrderDto {
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

        int totalQty = 0;
        List<PurchaseOrderItemDto> itemDtos = new ArrayList<>();
        if (items != null) {
            for (PurchaseOrderItem item : items) {
                totalQty += (item.getQuantity() != null ? item.getQuantity() : 0);
                itemDtos.add(PurchaseOrderItemDto.fromEntity(item));
            }
        }

        String formattedDate = "";
        if (po.getOrderDatetime() != null) {
            formattedDate = po.getOrderDatetime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        String supplierName = po.getSupplier() != null ? po.getSupplier().getCompanyName() : "-";
        String createdByName = po.getCreatedBy() != null ? po.getCreatedBy().getFullName() : "-";
        Integer creatorUserId = po.getCreatedBy() != null ? po.getCreatedBy().getUserId() : null;

        String displayStatus = po.getStatus() != null ? formatStatus(po.getStatus().name()) : "Pending";

        return PurchaseOrderDto.builder()
                .id(po.getPoId())
                .poId(po.getPoId())
                .requestId(po.getRequest() != null ? po.getRequest().getRequestId() : null)
                .supplierId(po.getSupplier() != null ? po.getSupplier().getSupplierId() : null)
                .supplier(supplierName)
                .createdByUserId(creatorUserId)
                .createdBy(createdByName)
                .userId(creatorUserId != null ? String.valueOf(creatorUserId) : "")
                .orderDatetime(po.getOrderDatetime())
                .date(formattedDate)
                .totalQty(totalQty)
                .totalPrice(po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
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
            case "RECEIVED": return "Received";
            case "REJECTED_ON_RECEIPT": return "Rejected on Receipt";
            default: return raw;
        }
    }
}
