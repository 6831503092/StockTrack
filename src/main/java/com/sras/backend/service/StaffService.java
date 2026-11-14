package com.sras.backend.service;

import com.sras.backend.dto.StaffActivityItemDto;
import com.sras.backend.dto.StaffCheckRequest;
import com.sras.backend.dto.StaffDashboardDto;
import com.sras.backend.dto.StaffOrderDto;
import com.sras.backend.dto.StaffOrderItemDto;
import com.sras.backend.dto.StaffRejectRequest;
import com.sras.backend.dto.StaffTransferDto;
import com.sras.backend.entity.AuditLog;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.entity.PurchaseOrderItem;
import com.sras.backend.entity.ReceivingCheck;
import com.sras.backend.entity.ReceivingCheckItem;
import com.sras.backend.entity.SalesOrder;
import com.sras.backend.entity.SalesOrderItem;
import com.sras.backend.entity.StockMovement;
import com.sras.backend.entity.StockOutCheck;
import com.sras.backend.entity.StockOutCheckItem;
import com.sras.backend.entity.User;
import com.sras.backend.enums.MovementStatus;
import com.sras.backend.enums.PurchaseOrderStatus;
import com.sras.backend.enums.ReceivingCheckResult;
import com.sras.backend.enums.SalesOrderStatus;
import com.sras.backend.enums.StockOutCheckResult;
import com.sras.backend.repository.AuditLogRepository;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.PurchaseOrderItemRepository;
import com.sras.backend.repository.PurchaseOrderRepository;
import com.sras.backend.repository.ReceivingCheckItemRepository;
import com.sras.backend.repository.ReceivingCheckRepository;
import com.sras.backend.repository.SalesOrderItemRepository;
import com.sras.backend.repository.SalesOrderRepository;
import com.sras.backend.repository.StockMovementRepository;
import com.sras.backend.repository.StockOutCheckItemRepository;
import com.sras.backend.repository.StockOutCheckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final ReceivingCheckRepository receivingCheckRepository;
    private final ReceivingCheckItemRepository receivingCheckItemRepository;

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final StockOutCheckRepository stockOutCheckRepository;
    private final StockOutCheckItemRepository stockOutCheckItemRepository;

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    // ==========================================
    // 1. RECEIVE WORKFLOW (Inbound PO)
    // ==========================================

    @Transactional(readOnly = true)
    public List<StaffOrderDto> getPendingReceives() {
        List<PurchaseOrder> approvedPos = purchaseOrderRepository.findByStatusOrderByOrderDatetimeDesc(PurchaseOrderStatus.APPROVED);
        return approvedPos.stream().map(this::mapPoToDto).collect(Collectors.toList());
    }

    @Transactional
    public void acceptReceive(Integer poId, User staffUser, StaffCheckRequest req) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase Order not found: " + poId));

        if (po.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purchase Order is not awaiting receiving (status=" + po.getStatus() + ")");
        }

        ReceivingCheck check = ReceivingCheck.builder()
                .purchaseOrder(po)
                .checkedBy(staffUser)
                .checkedDatetime(LocalDateTime.now())
                .result(ReceivingCheckResult.ACCEPTED)
                .rejectReason(null)
                .build();
        receivingCheckRepository.save(check);

        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderPoId(poId);
        Map<Integer, StaffCheckRequest.ItemCheck> checkMap = (req != null && req.getItems() != null)
                ? req.getItems().stream().filter(i -> i.getItemId() != null).collect(Collectors.toMap(StaffCheckRequest.ItemCheck::getItemId, i -> i, (a, b) -> a))
                : Collections.emptyMap();

        // Validate that all items are received in full (exact quantity, no deficit, no excess)
        for (PurchaseOrderItem item : items) {
            StaffCheckRequest.ItemCheck checkItem = checkMap.get(item.getPoItemId());
            int counted = (checkItem != null && checkItem.getCountedQty() != null)
                    ? checkItem.getCountedQty()
                    : item.getQuantity();
            boolean isCorrect = (checkItem == null || checkItem.getIsCorrectProduct() == null || checkItem.getIsCorrectProduct());

            if (!isCorrect) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot accept receipt: product verification marked incorrect for " +
                                (item.getProduct() != null ? item.getProduct().getProductName() : ("Item #" + item.getPoItemId())) +
                                ". Please reject the delivery with a specified reason.");
            }

            if (counted != item.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Received quantity must match expected quantity exactly (no missing or excess goods allowed) for " +
                                (item.getProduct() != null ? item.getProduct().getProductName() : ("Item #" + item.getPoItemId())) +
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

            // Update product stock
            Product product = item.getProduct();
            if (product != null) {
                int current = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
                product.setCurrentStock(current + counted);
                productRepository.save(product);
            }
            totalUnitsReceived += counted;
        }

        po.setStatus(PurchaseOrderStatus.RECEIVED);
        purchaseOrderRepository.save(po);

        String note = (req != null && req.getNotes() != null && !req.getNotes().isBlank()) ? " Note: " + req.getNotes().trim() : "";
        auditLogService.log(staffUser, "RECEIVE_ACCEPT",
                "Received PO #" + poId + " (" + totalUnitsReceived + " units)." + note);
        log.info("PO #{} received successfully by staff #{}", poId, staffUser.getUserId());
    }

    @Transactional
    public void rejectReceive(Integer poId, User staffUser, StaffRejectRequest req) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase Order not found: " + poId));

        if (po.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purchase Order is not awaiting receiving (status=" + po.getStatus() + ")");
        }

        if (req == null || req.getReason() == null || req.getReason().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required to reject this order.");
        }
        String reason = req.getReason().trim();

        ReceivingCheck check = ReceivingCheck.builder()
                .purchaseOrder(po)
                .checkedBy(staffUser)
                .checkedDatetime(LocalDateTime.now())
                .result(ReceivingCheckResult.REJECTED)
                .rejectReason(reason)
                .build();
        receivingCheckRepository.save(check);

        po.setStatus(PurchaseOrderStatus.REJECTED_ON_RECEIPT);
        purchaseOrderRepository.save(po);

        auditLogService.log(staffUser, "RECEIVE_REJECT",
                "Rejected receiving for PO #" + poId + ". Reason: " + reason);
        log.info("PO #{} rejected on receipt by staff #{}: {}", poId, staffUser.getUserId(), reason);
    }

    // ==========================================
    // 2. STOCK OUT WORKFLOW (Outbound SO)
    // ==========================================

    @Transactional(readOnly = true)
    public List<StaffOrderDto> getPendingStockOuts() {
        List<SalesOrder> approvedSos = salesOrderRepository.findByStatusOrderByOrderDatetimeDesc(SalesOrderStatus.APPROVED);
        return approvedSos.stream().map(this::mapSoToDto).collect(Collectors.toList());
    }

    @Transactional
    public void confirmStockOut(Integer soId, User staffUser, StaffCheckRequest req) {
        SalesOrder so = salesOrderRepository.findById(soId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sales Order not found: " + soId));

        if (so.getStatus() != SalesOrderStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sales Order is not awaiting stock out (status=" + so.getStatus() + ")");
        }

        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderSoId(soId);
        Map<Integer, StaffCheckRequest.ItemCheck> checkMap = (req != null && req.getItems() != null)
                ? req.getItems().stream().filter(i -> i.getItemId() != null).collect(Collectors.toMap(StaffCheckRequest.ItemCheck::getItemId, i -> i, (a, b) -> a))
                : Collections.emptyMap();

        // 1. Verify stock availability for all items
        for (SalesOrderItem item : items) {
            StaffCheckRequest.ItemCheck checkItem = checkMap.get(item.getSoItemId());
            int counted = (checkItem != null && checkItem.getCountedQty() != null && checkItem.getCountedQty() > 0)
                    ? checkItem.getCountedQty()
                    : item.getQuantity();

            Product product = item.getProduct();
            int current = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
            if (current < counted) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insufficient stock for " + product.getProductName() + " (In stock: " + current + ", Required: " + counted + ")");
            }
        }

        // 2. Record StockOutCheck
        StockOutCheck check = StockOutCheck.builder()
                .salesOrder(so)
                .checkedBy(staffUser)
                .checkedDatetime(LocalDateTime.now())
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

            Product product = item.getProduct();
            product.setCurrentStock(product.getCurrentStock() - counted);
            productRepository.save(product);
            totalUnitsStockedOut += counted;
        }

        so.setStatus(SalesOrderStatus.STOCK_OUT);
        salesOrderRepository.save(so);

        String note = (req != null && req.getNotes() != null && !req.getNotes().isBlank()) ? " Note: " + req.getNotes().trim() : "";
        auditLogService.log(staffUser, "STOCK_OUT_CONFIRM",
                "Stock Out confirmed for SO #" + soId + " (" + totalUnitsStockedOut + " units)." + note);
        log.info("SO #{} stocked out successfully by staff #{}", soId, staffUser.getUserId());
    }

    @Transactional
    public void rejectStockOut(Integer soId, User staffUser, StaffRejectRequest req) {
        SalesOrder so = salesOrderRepository.findById(soId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sales Order not found: " + soId));

        if (so.getStatus() != SalesOrderStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sales Order is not awaiting stock out (status=" + so.getStatus() + ")");
        }

        if (req == null || req.getReason() == null || req.getReason().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required to reject this order.");
        }
        String reason = req.getReason().trim();

        StockOutCheck check = StockOutCheck.builder()
                .salesOrder(so)
                .checkedBy(staffUser)
                .checkedDatetime(LocalDateTime.now())
                .result(StockOutCheckResult.REJECTED)
                .rejectReason(reason)
                .build();
        stockOutCheckRepository.save(check);

        so.setStatus(SalesOrderStatus.REJECTED_ON_STOCK_OUT);
        salesOrderRepository.save(so);

        auditLogService.log(staffUser, "STOCK_OUT_REJECT",
                "Rejected Stock Out for SO #" + soId + ". Reason: " + reason);
        log.info("SO #{} rejected on stock out by staff #{}: {}", soId, staffUser.getUserId(), reason);
    }

    // ==========================================
    // 3. TRANSFER WORKFLOW (Zone Stock Movement)
    // ==========================================

    @Transactional(readOnly = true)
    public List<StaffTransferDto> getPendingTransfers() {
        List<StockMovement> movements = stockMovementRepository.findByStatus(MovementStatus.PENDING_VERIFICATION);
        return movements.stream().map(this::mapMovementToDto).collect(Collectors.toList());
    }

    @Transactional
    public void verifyTransfer(Integer movementId, User staffUser, StaffCheckRequest req) {
        StockMovement movement = stockMovementRepository.findById(movementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock movement not found: " + movementId));

        if (movement.getStatus() != MovementStatus.PENDING_VERIFICATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock movement is not pending verification (status=" + movement.getStatus() + ")");
        }

        int counted = movement.getQuantity();
        if (req != null && req.getItems() != null && !req.getItems().isEmpty() && req.getItems().get(0).getCountedQty() != null) {
            counted = req.getItems().get(0).getCountedQty();
        }

        movement.setStatus(MovementStatus.COMPLETED);
        movement.setVerifiedBy(staffUser);
        movement.setVerifiedDatetime(LocalDateTime.now());
        movement.setCountedQty(counted);
        stockMovementRepository.save(movement);

        // Move/swap all products between fromZone and toZone
        if (movement.getFromZone() != null && movement.getToZone() != null) {
            List<Product> fromProducts = productRepository.findByZoneZoneIdAndIsActiveTrue(movement.getFromZone().getZoneId());
            List<Product> toProducts = productRepository.findByZoneZoneIdAndIsActiveTrue(movement.getToZone().getZoneId());

            if (fromProducts != null && !fromProducts.isEmpty()) {
                for (Product p : fromProducts) {
                    p.setZone(movement.getToZone());
                }
                productRepository.saveAll(fromProducts);
            }
            if (toProducts != null && !toProducts.isEmpty()) {
                for (Product p : toProducts) {
                    p.setZone(movement.getFromZone());
                }
                productRepository.saveAll(toProducts);
            }
        } else if (movement.getProduct() != null && movement.getToZone() != null) {
            Product product = movement.getProduct();
            product.setZone(movement.getToZone());
            productRepository.save(product);
        }

        auditLogService.log(staffUser, "TRANSFER_VERIFY",
                "Verified stock transfer #" + movementId +
                        " between " + movement.getFromZone().getZoneName() + " and " + movement.getToZone().getZoneName());
        log.info("Stock movement #{} verified by staff #{}", movementId, staffUser.getUserId());
    }

    @Transactional
    public void rejectTransfer(Integer movementId, User staffUser, StaffRejectRequest req) {
        StockMovement movement = stockMovementRepository.findById(movementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock movement not found: " + movementId));

        if (movement.getStatus() != MovementStatus.PENDING_VERIFICATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock movement is not pending verification (status=" + movement.getStatus() + ")");
        }

        if (req == null || req.getReason() == null || req.getReason().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required to reject this transfer.");
        }
        String reason = req.getReason().trim();

        movement.setStatus(MovementStatus.REJECTED);
        movement.setVerifiedBy(staffUser);
        movement.setVerifiedDatetime(LocalDateTime.now());
        movement.setRejectReason(reason);
        stockMovementRepository.save(movement);

        auditLogService.log(staffUser, "TRANSFER_REJECT",
                "Rejected stock transfer #" + movementId + ". Reason: " + reason);
        log.info("Stock movement #{} rejected by staff #{}: {}", movementId, staffUser.getUserId(), reason);
    }

    // ==========================================
    // 4. DASHBOARD & ACTIVITIES
    // ==========================================

    @Transactional(readOnly = true)
    public StaffDashboardDto getDashboardSummary() {
        long receiptCount = receivingCheckRepository.findAll().stream()
                .filter(r -> r.getResult() == ReceivingCheckResult.ACCEPTED).count();
        long stockOutCount = stockOutCheckRepository.findAll().stream()
                .filter(s -> s.getResult() == StockOutCheckResult.STOCK_OUT).count();
        long transferCount = stockMovementRepository.findAll().stream()
                .filter(m -> m.getStatus() == MovementStatus.COMPLETED).count();
        long totalCount = receiptCount + stockOutCount + transferCount;

        List<StaffActivityItemDto> activities = getRecentStaffActivities(15);

        return StaffDashboardDto.builder()
                .receiptCount(receiptCount)
                .stockOutCount(stockOutCount)
                .transferCount(transferCount)
                .totalCount(totalCount)
                .recentActivities(activities)
                .build();
    }

    private List<StaffActivityItemDto> getRecentStaffActivities(int limit) {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByActionDatetimeDesc();
        List<StaffActivityItemDto> result = new ArrayList<>();

        for (AuditLog al : logs) {
            if (result.size() >= limit) break;
            String action = al.getActionType();
            if (action == null) continue;

            String operation = null;
            String status = "Completed";

            if (action.startsWith("RECEIVE_")) {
                operation = "Receive";
                if (action.endsWith("REJECT")) status = "Rejected";
            } else if (action.startsWith("STOCK_OUT_")) {
                operation = "Stock Out";
                if (action.endsWith("REJECT")) status = "Rejected";
            } else if (action.startsWith("TRANSFER_")) {
                operation = "Transfer";
                if (action.endsWith("REJECT")) status = "Rejected";
            }

            if (operation != null) {
                result.add(StaffActivityItemDto.builder()
                        .date(al.getActionDatetime() != null ? al.getActionDatetime().format(DATETIME_FORMATTER) : "—")
                        .operation(operation)
                        .product(al.getDetail() != null ? al.getDetail() : operation)
                        .quantity(1)
                        .status(status)
                        .reference(action)
                        .build());
            }
        }
        return result;
    }

    // ==========================================
    // HELPER MAPPERS
    // ==========================================

    private StaffOrderDto mapPoToDto(PurchaseOrder po) {
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

    private StaffOrderDto mapSoToDto(SalesOrder so) {
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

    private StaffTransferDto mapMovementToDto(StockMovement m) {
        String reqRef = m.getRequest() != null ? "REQ-" + String.format("%04d", m.getRequest().getRequestId()) : "TR-" + m.getMovementId();
        String requestedBy = (m.getRequest() != null && m.getRequest().getRequestedBy() != null)
                ? m.getRequest().getRequestedBy().getFullName()
                : "Admin";
        String date = m.getRequest() != null && m.getRequest().getRequestedDatetime() != null
                ? m.getRequest().getRequestedDatetime().format(DATE_FORMATTER)
                : LocalDateTime.now().format(DATE_FORMATTER);

        return StaffTransferDto.builder()
                .movementId(m.getMovementId())
                .code("TR-" + m.getMovementId())
                .reference(reqRef)
                .requestedBy(requestedBy)
                .date(date)
                .productId(m.getProduct() != null ? m.getProduct().getProductId() : null)
                .productName(m.getProduct() != null ? m.getProduct().getProductName() : "Product")
                .fromZoneId(m.getFromZone() != null ? m.getFromZone().getZoneId() : null)
                .fromZoneName(m.getFromZone() != null ? m.getFromZone().getZoneName() : "Source Zone")
                .toZoneId(m.getToZone() != null ? m.getToZone().getZoneId() : null)
                .toZoneName(m.getToZone() != null ? m.getToZone().getZoneName() : "Target Zone")
                .quantity(m.getQuantity())
                .status(m.getStatus() != null ? m.getStatus().name() : "")
                .build();
    }
}
