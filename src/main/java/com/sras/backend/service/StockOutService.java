package com.sras.backend.service;

import com.sras.backend.dto.StaffCheckRequest;
import com.sras.backend.dto.StaffOrderDto;
import com.sras.backend.dto.StaffOrderItemDto;
import com.sras.backend.dto.StaffRejectRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.SalesOrder;
import com.sras.backend.entity.SalesOrderItem;
import com.sras.backend.entity.StockOutCheck;
import com.sras.backend.entity.StockOutCheckItem;
import com.sras.backend.entity.User;
import com.sras.backend.enums.SalesOrderStatus;
import com.sras.backend.enums.StockOutCheckResult;
import com.sras.backend.exception.BusinessRuleException;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.SalesOrderItemRepository;
import com.sras.backend.repository.SalesOrderRepository;
import com.sras.backend.repository.StockOutCheckItemRepository;
import com.sras.backend.repository.StockOutCheckRepository;
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
public class StockOutService {

    private static final ZoneId BANGKOK_ZONE = ZoneId.of("Asia/Bangkok");
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final StockOutCheckRepository stockOutCheckRepository;
    private final StockOutCheckItemRepository stockOutCheckItemRepository;
    private final StockMovementService stockMovementService;
    private final AuditLogService auditLogService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    @Transactional(readOnly = true)
    public List<StaffOrderDto> getPendingStockOuts() {
        List<SalesOrder> approvedSos = salesOrderRepository.findByStatusOrderByOrderDatetimeDesc(SalesOrderStatus.APPROVED);
        return approvedSos.stream().map(this::mapSoToDto).collect(Collectors.toList());
    }

    @Transactional
    public void confirmStockOut(Integer soId, User staffUser, StaffCheckRequest req) {
        SalesOrder so = salesOrderRepository.findById(soId)
                .orElseThrow(() -> new NotFoundException("Sales Order not found: " + soId));

        if (so.getStatus() != SalesOrderStatus.APPROVED) {
            throw new BusinessRuleException("Sales Order is not awaiting stock out (status=" + so.getStatus() + ")");
        }

        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderSoId(soId);
        Map<Integer, StaffCheckRequest.ItemCheck> checkMap = (req != null && req.getItems() != null)
                ? req.getItems().stream()
                    .filter(i -> i.getItemId() != null)
                    .collect(Collectors.toMap(StaffCheckRequest.ItemCheck::getItemId, i -> i, (a, b) -> a))
                : Collections.emptyMap();

        // 1. Verify stock availability before applying any deduction
        for (SalesOrderItem item : items) {
            StaffCheckRequest.ItemCheck checkItem = checkMap.get(item.getSoItemId());
            int counted = (checkItem != null && checkItem.getCountedQty() != null && checkItem.getCountedQty() > 0)
                    ? checkItem.getCountedQty()
                    : item.getQuantity();

            Product product = item.getProduct();
            int current = (product != null && product.getCurrentStock() != null) ? product.getCurrentStock() : 0;
            if (current < counted) {
                String pName = product != null ? product.getProductName() : ("Item #" + item.getSoItemId());
                throw new ValidationException(
                        "Insufficient stock for " + pName + " (In stock: " + current + ", Required: " + counted + ")");
            }
        }

        // 2. Record StockOutCheck
        StockOutCheck check = StockOutCheck.builder()
                .salesOrder(so)
                .checkedBy(staffUser)
                .checkedDatetime(LocalDateTime.now(BANGKOK_ZONE))
                .result(StockOutCheckResult.STOCK_OUT)
                .rejectReason(null)
                .build();
        stockOutCheckRepository.save(check);

        // 3. Record Items & Deduct Stock
        int totalUnitsStockedOut = 0;
        for (SalesOrderItem item : items) {
            StaffCheckRequest.ItemCheck checkItem = checkMap.get(item.getSoItemId());
            int counted = (checkItem != null && checkItem.getCountedQty() != null && checkItem.getCountedQty() > 0)
                    ? checkItem.getCountedQty()
                    : item.getQuantity();
            boolean isCorrect = (checkItem == null || checkItem.getIsCorrectProduct() == null || checkItem.getIsCorrectProduct());

            StockOutCheckItem soci = StockOutCheckItem.builder()
                    .stockOutCheck(check)
                    .salesOrderItem(item)
                    .expectedQty(item.getQuantity())
                    .countedQty(counted)
                    .isCorrectProduct(isCorrect)
                    .build();
            stockOutCheckItemRepository.save(soci);

            if (item.getProduct() != null) {
                stockMovementService.adjustStock(item.getProduct(), -counted);
            }
            totalUnitsStockedOut += counted;
        }

        so.setStatus(SalesOrderStatus.STOCK_OUT);
        salesOrderRepository.save(so);

        String note = (req != null && req.getNotes() != null && !req.getNotes().isBlank()) ? " Note: " + req.getNotes().trim() : "";
        auditLogService.log(staffUser, com.sras.backend.enums.AuditAction.STOCK_OUT_CONFIRM, "SO", soId,
                "Stock Out confirmed for SO #" + soId + " (" + totalUnitsStockedOut + " units)." + note);
        log.info("SO #{} stocked out successfully by staff #{}", soId, staffUser.getUserId());
    }

    @Transactional
    public void rejectStockOut(Integer soId, User staffUser, StaffRejectRequest req) {
        SalesOrder so = salesOrderRepository.findById(soId)
                .orElseThrow(() -> new NotFoundException("Sales Order not found: " + soId));

        if (so.getStatus() != SalesOrderStatus.APPROVED) {
            throw new BusinessRuleException("Sales Order is not awaiting stock out (status=" + so.getStatus() + ")");
        }

        if (req == null || req.getReason() == null || req.getReason().trim().isBlank()) {
            throw new ValidationException("A reason is required to reject this order.");
        }
        String reason = req.getReason().trim();

        StockOutCheck check = StockOutCheck.builder()
                .salesOrder(so)
                .checkedBy(staffUser)
                .checkedDatetime(LocalDateTime.now(BANGKOK_ZONE))
                .result(StockOutCheckResult.REJECTED)
                .rejectReason(reason)
                .build();
        stockOutCheckRepository.save(check);

        so.setStatus(SalesOrderStatus.REJECTED_ON_STOCK_OUT);
        salesOrderRepository.save(so);

        auditLogService.log(staffUser, com.sras.backend.enums.AuditAction.STOCK_OUT_REJECT, "SO", soId,
                "Rejected Stock Out for SO #" + soId + ". Reason: " + reason);
        log.info("SO #{} rejected on stock out by staff #{}: {}", soId, staffUser.getUserId(), reason);
    }

    public StaffOrderDto mapSoToDto(SalesOrder so) {
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderSoId(so.getSoId());
        List<StaffOrderItemDto> itemDtos = items.stream().map(i -> StaffOrderItemDto.builder()
                .itemId(i.getSoItemId())
                .productId(i.getProduct() != null ? i.getProduct().getProductId() : null)
                .productName(i.getProduct() != null ? i.getProduct().getProductName() : "Product")
                .zoneName(i.getProduct() != null && i.getProduct().getZone() != null ? i.getProduct().getZone().getZoneName() : "Warehouse")
                .expectedQty(i.getQuantity())
                .unitPriceOrCost(i.getUnitPrice())
                .build()).collect(Collectors.toList());

        return StaffOrderDto.builder()
                .id(so.getSoId())
                .code("SO-" + so.getSoId())
                .reference("SO-" + String.format("%04d", so.getSoId()))
                .requester(so.getCreatedBy() != null ? so.getCreatedBy().getFullName() : "Admin")
                .partyName(so.getCustomer() != null ? so.getCustomer().getCustomerName() : "Customer")
                .date(so.getOrderDatetime() != null ? so.getOrderDatetime().format(DATE_FORMATTER) : "")
                .totalAmount(so.getTotalAmount())
                .status(so.getStatus() != null ? so.getStatus().name() : "")
                .items(itemDtos)
                .build();
    }
}
