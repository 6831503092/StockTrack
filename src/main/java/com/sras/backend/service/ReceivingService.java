package com.sras.backend.service;

import com.sras.backend.dto.StaffCheckRequest;
import com.sras.backend.dto.StaffOrderDto;
import com.sras.backend.dto.StaffOrderItemDto;
import com.sras.backend.dto.StaffRejectRequest;
import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.entity.PurchaseOrderItem;
import com.sras.backend.entity.ReceivingCheck;
import com.sras.backend.entity.ReceivingCheckItem;
import com.sras.backend.entity.User;
import com.sras.backend.enums.PurchaseOrderStatus;
import com.sras.backend.enums.ReceivingCheckResult;
import com.sras.backend.exception.BusinessRuleException;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.PurchaseOrderItemRepository;
import com.sras.backend.repository.PurchaseOrderRepository;
import com.sras.backend.repository.ReceivingCheckItemRepository;
import com.sras.backend.repository.ReceivingCheckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivingService {

    private static final ZoneId BANGKOK_ZONE = ZoneId.of("Asia/Bangkok");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final ReceivingCheckRepository receivingCheckRepository;
    private final ReceivingCheckItemRepository receivingCheckItemRepository;
    private final StockMovementService stockMovementService;
    private final AuditLogService auditLogService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    @Transactional(readOnly = true)
    public List<StaffOrderDto> getPendingReceives() {
        List<PurchaseOrder> approvedPos = purchaseOrderRepository.findByStatusOrderByOrderDatetimeDesc(PurchaseOrderStatus.APPROVED);
        return approvedPos.stream().map(this::mapPoToDto).collect(Collectors.toList());
    }

    @Transactional
    public void acceptReceive(Integer poId, User staffUser, StaffCheckRequest req) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new NotFoundException("Purchase Order not found: " + poId));

        if (po.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new BusinessRuleException("Purchase Order is not awaiting receiving (status=" + po.getStatus() + ")");
        }

        ReceivingCheck check = ReceivingCheck.builder()
                .purchaseOrder(po)
                .checkedBy(staffUser)
                .checkedDatetime(LocalDateTime.now(BANGKOK_ZONE))
                .result(ReceivingCheckResult.ACCEPTED)
                .rejectReason(null)
                .build();
        receivingCheckRepository.save(check);

        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderPoId(poId);
        Map<Integer, StaffCheckRequest.ItemCheck> checkMap = (req != null && req.getItems() != null)
                ? req.getItems().stream()
                    .filter(i -> i.getItemId() != null)
                    .collect(Collectors.toMap(StaffCheckRequest.ItemCheck::getItemId, i -> i, (a, b) -> a))
                : Collections.emptyMap();

        // Validate items before applying changes
        for (PurchaseOrderItem item : items) {
            StaffCheckRequest.ItemCheck checkItem = checkMap.get(item.getPoItemId());
            int counted = (checkItem != null && checkItem.getCountedQty() != null)
                    ? checkItem.getCountedQty()
                    : item.getQuantity();
            boolean isCorrect = (checkItem == null || checkItem.getIsCorrectProduct() == null || checkItem.getIsCorrectProduct());

            String pName = item.getProduct() != null ? item.getProduct().getProductName() : ("Item #" + item.getPoItemId());

            if (!isCorrect) {
                throw new ValidationException(
                        "Cannot accept receipt: product verification marked incorrect for " + pName +
                                ". Please reject the delivery with a specified reason.");
            }

            if (counted != item.getQuantity()) {
                throw new ValidationException(
                        "Received quantity must match expected quantity exactly for " + pName +
                                ". Expected: " + item.getQuantity() + ", Counted: " + counted +
                                ". If goods are incomplete or damaged, please reject the delivery with a reason.");
            }
        }

        int totalUnitsReceived = 0;
        for (PurchaseOrderItem item : items) {
            StaffCheckRequest.ItemCheck checkItem = checkMap.get(item.getPoItemId());
            int counted = (checkItem != null && checkItem.getCountedQty() != null && checkItem.getCountedQty() > 0)
                    ? checkItem.getCountedQty()
                    : item.getQuantity();
            boolean isCorrect = (checkItem == null || checkItem.getIsCorrectProduct() == null || checkItem.getIsCorrectProduct());

            ReceivingCheckItem rci = ReceivingCheckItem.builder()
                    .receivingCheck(check)
                    .purchaseOrderItem(item)
                    .expectedQty(item.getQuantity())
                    .countedQty(counted)
                    .isCorrectProduct(isCorrect)
                    .build();
            receivingCheckItemRepository.save(rci);

            if (item.getProduct() != null) {
                stockMovementService.adjustStock(item.getProduct(), counted);
            }
            totalUnitsReceived += counted;
        }

        po.setStatus(PurchaseOrderStatus.RECEIVED);
        purchaseOrderRepository.save(po);

        String note = (req != null && req.getNotes() != null && !req.getNotes().isBlank()) ? " Note: " + req.getNotes().trim() : "";
        auditLogService.log(staffUser, com.sras.backend.enums.AuditAction.RECEIVE_ACCEPT, "PO", poId,
                "Received PO #" + poId + " (" + totalUnitsReceived + " units)." + note);
        log.info("PO #{} received successfully by staff #{}", poId, staffUser.getUserId());
    }

    @Transactional
    public void rejectReceive(Integer poId, User staffUser, StaffRejectRequest req) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new NotFoundException("Purchase Order not found: " + poId));

        if (po.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new BusinessRuleException("Purchase Order is not awaiting receiving (status=" + po.getStatus() + ")");
        }

        if (req == null || req.getReason() == null || req.getReason().trim().isBlank()) {
            throw new ValidationException("A reason is required to reject this order.");
        }
        String reason = req.getReason().trim();

        ReceivingCheck check = ReceivingCheck.builder()
                .purchaseOrder(po)
                .checkedBy(staffUser)
                .checkedDatetime(LocalDateTime.now(BANGKOK_ZONE))
                .result(ReceivingCheckResult.REJECTED)
                .rejectReason(reason)
                .build();
        receivingCheckRepository.save(check);

        po.setStatus(PurchaseOrderStatus.REJECTED_ON_RECEIPT);
        purchaseOrderRepository.save(po);

        auditLogService.log(staffUser, com.sras.backend.enums.AuditAction.RECEIVE_REJECT, "PO", poId,
                "Rejected receiving for PO #" + poId + ". Reason: " + reason);
        log.info("PO #{} rejected on receipt by staff #{}: {}", poId, staffUser.getUserId(), reason);
    }

    public StaffOrderDto mapPoToDto(PurchaseOrder po) {
        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderPoId(po.getPoId());
        List<StaffOrderItemDto> itemDtos = items.stream().map(i -> StaffOrderItemDto.builder()
                .itemId(i.getPoItemId())
                .productId(i.getProduct() != null ? i.getProduct().getProductId() : null)
                .productName(i.getProduct() != null ? i.getProduct().getProductName() : "Product")
                .zoneName(i.getProduct() != null && i.getProduct().getZone() != null ? i.getProduct().getZone().getZoneName() : "Warehouse")
                .expectedQty(i.getQuantity())
                .unitPriceOrCost(i.getUnitCost())
                .build()).collect(Collectors.toList());

        return StaffOrderDto.builder()
                .id(po.getPoId())
                .code("PO-" + po.getPoId())
                .reference("PO-" + String.format("%04d", po.getPoId()))
                .requester(po.getCreatedBy() != null ? po.getCreatedBy().getFullName() : "Admin")
                .partyName(po.getSupplier() != null ? po.getSupplier().getCompanyName() : "Supplier")
                .date(po.getOrderDatetime() != null ? po.getOrderDatetime().format(DATE_FORMATTER) : "")
                .totalAmount(po.getTotalAmount())
                .status(po.getStatus() != null ? po.getStatus().name() : "")
                .items(itemDtos)
                .build();
    }
}
