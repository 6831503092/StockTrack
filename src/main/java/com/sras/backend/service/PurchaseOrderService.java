package com.sras.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.PurchaseOrderCreateRequest;
import com.sras.backend.dto.PurchaseOrderDto;
import com.sras.backend.dto.PurchaseOrderItemDto;
import com.sras.backend.dto.PurchaseOrderItemRequest;
import com.sras.backend.dto.SupplierDto;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.entity.PurchaseOrderItem;
import com.sras.backend.entity.Supplier;
import com.sras.backend.entity.User;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.PurchaseOrderStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.TargetType;
import com.sras.backend.repository.ApprovalRequestRepository;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.PurchaseOrderItemRepository;
import com.sras.backend.repository.PurchaseOrderRepository;
import com.sras.backend.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<SupplierDto> getActiveSuppliers() {
        return supplierRepository.findByIsActiveTrue()
                .stream()
                .map(SupplierDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderDto> getAllPurchaseOrders(String search, String status, String dateStr) {
        List<PurchaseOrder> pos = purchaseOrderRepository.findAllByOrderByOrderDatetimeDesc();

        return pos.stream()
                .map(po -> {
                    List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderPoId(po.getPoId());
                    return PurchaseOrderDto.fromEntity(po, items);
                })
                .filter(dto -> matchesSearch(dto, search))
                .filter(dto -> matchesStatus(dto, status))
                .filter(dto -> matchesDate(dto, dateStr))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrderById(Integer id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order not found with ID: " + id));
        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderPoId(po.getPoId());
        return PurchaseOrderDto.fromEntity(po, items);
    }

    @Transactional
    public PurchaseOrderDto createPurchaseOrder(PurchaseOrderCreateRequest req, User currentUser) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated");
        }

        // 1. Resolve Supplier
        Supplier supplier = null;
        if (req.getSupplierId() != null) {
            supplier = supplierRepository.findById(req.getSupplierId()).orElse(null);
        }
        if (supplier == null && req.getSupplierName() != null && !req.getSupplierName().isBlank()) {
            supplier = supplierRepository.findByCompanyName(req.getSupplierName().trim()).orElse(null);
        }
        if (supplier == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid supplier is required");
        }

        // 2. Validate Items
        List<PurchaseOrderItemRequest> itemRequests = req.getItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purchase order must contain at least one item");
        }

        class ValidatedItem {
            Product product;
            int quantity;
            BigDecimal unitCost;
            BigDecimal subtotal;
        }

        List<ValidatedItem> validatedItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (PurchaseOrderItemRequest itemReq : itemRequests) {
            if (itemReq == null) continue;

            int qty = (itemReq.getQuantity() != null) ? itemReq.getQuantity() : 0;
            if (qty <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item quantity must be greater than 0");
            }

            Product product = null;
            if (itemReq.getProductId() != null) {
                product = productRepository.findById(itemReq.getProductId()).orElse(null);
            }
            if (product == null && itemReq.getProductName() != null && !itemReq.getProductName().isBlank()) {
                product = productRepository.findByProductNameIgnoreCase(itemReq.getProductName().trim())
                        .orElse(null);
            }
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product not found: " + (itemReq.getProductName() != null ? itemReq.getProductName() : itemReq.getProductId()));
            }

            BigDecimal unitCost = itemReq.getUnitCost();
            if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) <= 0) {
                unitCost = product.getDefaultCost() != null ? product.getDefaultCost() : BigDecimal.ZERO;
            }

            BigDecimal subtotal = unitCost.multiply(BigDecimal.valueOf(qty));
            totalAmount = totalAmount.add(subtotal);
            totalQuantity += qty;

            ValidatedItem vi = new ValidatedItem();
            vi.product = product;
            vi.quantity = qty;
            vi.unitCost = unitCost;
            vi.subtotal = subtotal;
            validatedItems.add(vi);
        }

        if (validatedItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purchase order must contain at least one valid item");
        }

        // Ensure products match supplier's assigned zone
        if (supplier.getZoneCode() != null && !supplier.getZoneCode().isBlank()) {
            for (ValidatedItem vi : validatedItems) {
                if (vi.product.getZone() != null && !supplier.getZoneCode().equalsIgnoreCase(vi.product.getZone().getZoneCode())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Product '" + vi.product.getProductName() + "' is in " + vi.product.getZone().getZoneCode() +
                            ", but supplier '" + supplier.getCompanyName() + "' only supplies " + supplier.getZoneCode() +
                            (supplier.getCategory() != null ? " (" + supplier.getCategory() + ")" : ""));
                }
            }
        }

        // 3. Create Approval Request
        DecimalFormat currencyFmt = new DecimalFormat("#,##0.00");
        String formattedTotal = currencyFmt.format(totalAmount);
        String detailText = "Purchase Order for " + supplier.getCompanyName() +
                " (" + totalQuantity + " items, Total: ฿" + formattedTotal + ")";

        List<Map<String, Object>> payloadItems = new ArrayList<>();
        for (ValidatedItem vi : validatedItems) {
            Map<String, Object> itMap = new HashMap<>();
            itMap.put("productId", vi.product.getProductId());
            itMap.put("productName", vi.product.getProductName());
            itMap.put("quantity", vi.quantity);
            itMap.put("unitCost", vi.unitCost);
            itMap.put("subtotal", vi.subtotal);
            payloadItems.add(itMap);
        }

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("supplierId", supplier.getSupplierId());
        payloadMap.put("supplierName", supplier.getCompanyName());
        payloadMap.put("totalAmount", totalAmount);
        payloadMap.put("totalPrice", totalAmount);
        payloadMap.put("totalQuantity", totalQuantity);
        payloadMap.put("totalQty", totalQuantity);
        payloadMap.put("items", payloadItems);
        if (req.getNote() != null && !req.getNote().isBlank()) {
            payloadMap.put("note", req.getNote().trim());
        }

        String payloadJson = "{}";
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            // fallback
            payloadJson = "{\"supplierName\":\"" + supplier.getCompanyName() + "\"}";
        }

        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType(RequestType.PO_CREATE)
                .targetType(TargetType.PRODUCT)
                .payload(payloadJson)
                .detail(detailText)
                .status(ApprovalStatus.PENDING)
                .requestedBy(currentUser)
                .requestedDatetime(LocalDateTime.now())
                .build();
        approvalRequest = approvalRequestRepository.save(approvalRequest);

        // 4. Create Purchase Order
        PurchaseOrder po = PurchaseOrder.builder()
                .supplier(supplier)
                .createdBy(currentUser)
                .request(approvalRequest)
                .orderDatetime(LocalDateTime.now())
                .totalAmount(totalAmount)
                .status(PurchaseOrderStatus.PENDING)
                .build();
        po = purchaseOrderRepository.save(po);

        // Update targetId on ApprovalRequest to point to po_id
        approvalRequest.setTargetId(po.getPoId());
        approvalRequestRepository.save(approvalRequest);

        // 5. Create Purchase Order Items
        List<PurchaseOrderItem> savedItems = new ArrayList<>();
        for (ValidatedItem vi : validatedItems) {
            PurchaseOrderItem poi = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .product(vi.product)
                    .quantity(vi.quantity)
                    .unitCost(vi.unitCost)
                    .subtotal(vi.subtotal)
                    .build();
            savedItems.add(purchaseOrderItemRepository.save(poi));
        }

        // 6. Audit Log
        auditLogService.log(currentUser, "PO_CREATE", "Created Purchase Order #" + po.getPoId() +
                " from " + supplier.getCompanyName() + " (Total: ฿" + formattedTotal + ")");

        return PurchaseOrderDto.fromEntity(po, savedItems);
    }

    private boolean matchesSearch(PurchaseOrderDto dto, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String q = search.trim().toLowerCase();
        String idStr = dto.getId() != null ? String.valueOf(dto.getId()) : "";
        String supplierStr = dto.getSupplier() != null ? dto.getSupplier().toLowerCase() : "";
        String dateStr = dto.getDate() != null ? dto.getDate() : "";
        String userStr = dto.getCreatedBy() != null ? dto.getCreatedBy().toLowerCase() : "";

        return idStr.contains(q) || supplierStr.contains(q) || dateStr.contains(q) || userStr.contains(q);
    }

    private boolean matchesStatus(PurchaseOrderDto dto, String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return true;
        }
        String s = status.trim().toUpperCase();
        String poStatus = dto.getStatus() != null ? dto.getStatus().toUpperCase() : "";
        return poStatus.equals(s);
    }

    private boolean matchesDate(PurchaseOrderDto dto, String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return true;
        }
        if (dto.getOrderDatetime() == null) {
            return false;
        }
        try {
            String trimmed = dateStr.trim();
            LocalDate filterDate;
            if (trimmed.contains("/")) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d/M/yyyy");
                filterDate = LocalDate.parse(trimmed, dtf);
            } else {
                filterDate = LocalDate.parse(trimmed);
            }
            return dto.getOrderDatetime().toLocalDate().isEqual(filterDate);
        } catch (DateTimeParseException e) {
            return true;
        }
    }
}
