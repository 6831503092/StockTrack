package com.sras.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.dto.PurchaseOrderCreateRequest;
import com.sras.backend.dto.PurchaseOrderDto;
import com.sras.backend.dto.PurchaseOrderItemRequest;
import com.sras.backend.dto.SupplierDto;
import com.sras.backend.dto.SupplierRequest;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.entity.PurchaseOrderItem;
import com.sras.backend.entity.Supplier;
import com.sras.backend.entity.User;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final ZoneId BANGKOK_ZONE = ZoneId.of("Asia/Bangkok");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalRequestFactory approvalRequestFactory;
    private final SupplierService supplierService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private record ValidatedItem(Product product, int quantity, BigDecimal unitCost, BigDecimal subtotal) {}

    // Delegate supplier methods to SupplierService for backward compatibility
    @Transactional(readOnly = true)
    public List<SupplierDto> getActiveSuppliers() {
        return supplierService.getActiveSuppliers();
    }

    @Transactional
    public SupplierDto createSupplier(SupplierRequest req, User actor) {
        return supplierService.createSupplier(req, actor);
    }

    @Transactional
    public SupplierDto updateSupplier(Integer id, SupplierRequest req, User actor) {
        return supplierService.updateSupplier(id, req, actor);
    }

    @Transactional
    public void deleteSupplier(Integer id, User actor) {
        supplierService.deleteSupplier(id, actor);
    }

    @Transactional
    public ApprovalRequestDto requestSupplierCreate(SupplierRequest req, User actor) {
        return supplierService.requestSupplierCreate(req, actor);
    }

    @Transactional
    public ApprovalRequestDto requestSupplierUpdate(Integer id, SupplierRequest req, User actor) {
        return supplierService.requestSupplierUpdate(id, req, actor);
    }

    @Transactional
    public ApprovalRequestDto requestSupplierDeactivate(Integer id, User actor) {
        return supplierService.requestSupplierDeactivate(id, actor);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderDto> getAllPurchaseOrders(String search, String status, String dateStr) {
        return purchaseOrderRepository.findAllByOrderByOrderDatetimeDesc()
                .stream()
                .map(po -> PurchaseOrderDto.fromEntity(po, purchaseOrderItemRepository.findByPurchaseOrderPoId(po.getPoId())))
                .filter(dto -> matchesSearch(dto, search))
                .filter(dto -> ApprovalRequestService.matchesStatusFilter(dto.getStatus(), status))
                .filter(dto -> ApprovalRequestService.matchesDateFilter(dto.getOrderDatetime(), dateStr))
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrderById(Integer id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order not found with ID: " + id));
        return PurchaseOrderDto.fromEntity(po, purchaseOrderItemRepository.findByPurchaseOrderPoId(po.getPoId()));
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
        Supplier supplier = resolveSupplier(req.getSupplierId(), req.getSupplierName());

        // 2. Validate Items
        List<PurchaseOrderItemRequest> itemRequests = req.getItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purchase order must contain at least one item");
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

            Product product = resolveProduct(itemReq.getProductId(), itemReq.getProductName());

            BigDecimal unitCost = itemReq.getUnitCost();
            if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) <= 0) {
                unitCost = product.getDefaultCost() != null ? product.getDefaultCost() : BigDecimal.ZERO;
            }

            BigDecimal subtotal = unitCost.multiply(BigDecimal.valueOf(qty));
            totalAmount = totalAmount.add(subtotal);
            totalQuantity += qty;

            validatedItems.add(new ValidatedItem(product, qty, unitCost, subtotal));
        }

        if (validatedItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purchase order must contain at least one valid item");
        }

        // Ensure products match supplier's assigned zone
        if (supplier.getZoneCode() != null && !supplier.getZoneCode().isBlank()) {
            for (ValidatedItem vi : validatedItems) {
                if (vi.product().getZone() != null && !supplier.getZoneCode().equalsIgnoreCase(vi.product().getZone().getZoneCode())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Product '" + vi.product().getProductName() + "' is in " + vi.product().getZone().getZoneCode() +
                            ", but supplier '" + supplier.getCompanyName() + "' only supplies " + supplier.getZoneCode() +
                            (supplier.getCategory() != null ? " (" + supplier.getCategory() + ")" : ""));
                }
            }
        }

        // 3. Create Approval Request
        String formattedTotal = new DecimalFormat("#,##0.00").format(totalAmount);
        String detailText = "Purchase Order for " + supplier.getCompanyName() +
                " (" + totalQuantity + " items, Total: ฿" + formattedTotal + ")";

        List<Map<String, Object>> payloadItems = validatedItems.stream().map(vi -> {
            Map<String, Object> itMap = new HashMap<>();
            itMap.put("productId", vi.product().getProductId());
            itMap.put("productName", vi.product().getProductName());
            itMap.put("quantity", vi.quantity());
            itMap.put("unitCost", vi.unitCost());
            itMap.put("subtotal", vi.subtotal());
            return itMap;
        }).toList();

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

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            payloadJson = "{\"supplierName\":\"" + supplier.getCompanyName() + "\"}";
        }

        ApprovalRequest approvalRequest = approvalRequestFactory.create(
                RequestType.PO_CREATE, TargetType.PRODUCT, null, payloadJson, detailText, currentUser
        );

        // 4. Create Purchase Order
        PurchaseOrder po = purchaseOrderRepository.save(PurchaseOrder.builder()
                .supplier(supplier)
                .createdBy(currentUser)
                .request(approvalRequest)
                .orderDatetime(LocalDateTime.now(BANGKOK_ZONE))
                .totalAmount(totalAmount)
                .status(PurchaseOrderStatus.PENDING)
                .build());

        approvalRequest.setTargetId(po.getPoId());
        approvalRequestRepository.save(approvalRequest);

        // 5. Create Purchase Order Items
        List<PurchaseOrderItem> savedItems = validatedItems.stream()
                .map(vi -> purchaseOrderItemRepository.save(PurchaseOrderItem.builder()
                        .purchaseOrder(po)
                        .product(vi.product())
                        .quantity(vi.quantity())
                        .unitCost(vi.unitCost())
                        .subtotal(vi.subtotal())
                        .build()))
                .toList();

        // 6. Audit Log
        auditLogService.log(currentUser, com.sras.backend.enums.AuditAction.PO_CREATE, "PO", po.getPoId(),
                "Created Purchase Order #" + po.getPoId() + " from " + supplier.getCompanyName() + " (Total: ฿" + formattedTotal + ")");

        return PurchaseOrderDto.fromEntity(po, savedItems);
    }

    private Supplier resolveSupplier(Integer supplierId, String supplierName) {
        Supplier supplier = supplierId != null ? supplierRepository.findById(supplierId).orElse(null) : null;
        if (supplier == null && supplierName != null && !supplierName.isBlank()) {
            supplier = supplierRepository.findByCompanyName(supplierName.trim()).orElse(null);
        }
        if (supplier == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid supplier is required");
        }
        return supplier;
    }

    private Product resolveProduct(Integer productId, String productName) {
        Product product = productId != null ? productRepository.findById(productId).orElse(null) : null;
        if (product == null && productName != null && !productName.isBlank()) {
            product = productRepository.findByProductNameIgnoreCase(productName.trim()).orElse(null);
        }
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product not found: " + (productName != null ? productName : productId));
        }
        return product;
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
}
