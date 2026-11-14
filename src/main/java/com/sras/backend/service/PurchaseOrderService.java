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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private record ValidatedItem(Product product, int quantity, BigDecimal unitCost, BigDecimal subtotal) {}

    @Transactional(readOnly = true)
    public List<SupplierDto> getActiveSuppliers() {
        return supplierRepository.findByIsActiveTrue()
                .stream()
                .map(SupplierDto::fromEntity)
                .toList();
    }

    @Transactional
    public SupplierDto createSupplier(SupplierRequest req, User actor) {
        if (req == null || req.companyName() == null || req.companyName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name is required");
        }
        String name = req.companyName().trim();
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name cannot exceed 50 characters");
        }
        if (supplierRepository.findByCompanyName(name).filter(s -> Boolean.TRUE.equals(s.getIsActive())).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active supplier with this name already exists");
        }
        ensureCategoryNotDuplicate(req.category(), null);

        Supplier supplier = supplierRepository.findByCompanyName(name).orElseGet(Supplier::new);
        supplier.setCompanyName(name);
        supplier.setZoneCode(req.zoneCode() != null && !req.zoneCode().trim().isEmpty() ? req.zoneCode().trim().toUpperCase() : null);
        supplier.setCategory(req.category() != null && !req.category().trim().isEmpty() ? req.category().trim() : null);
        supplier.setIsActive(true);

        Supplier saved = supplierRepository.save(supplier);
        auditLogService.log(actor, "SUPPLIER_CREATE", "Created supplier #" + saved.getSupplierId() + " (" + saved.getCompanyName() + ")");
        return SupplierDto.fromEntity(saved);
    }

    @Transactional
    public SupplierDto updateSupplier(Integer id, SupplierRequest req, User actor) {
        if (req == null || req.companyName() == null || req.companyName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name is required");
        }
        String name = req.companyName().trim();
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name cannot exceed 50 characters");
        }

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found with ID: " + id));

        supplierRepository.findByCompanyName(name).ifPresent(existing -> {
            if (!existing.getSupplierId().equals(id) && Boolean.TRUE.equals(existing.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another active supplier already exists with name: " + name);
            }
        });
        ensureCategoryNotDuplicate(req.category(), id);

        supplier.setCompanyName(name);
        if (req.zoneCode() != null) {
            supplier.setZoneCode(req.zoneCode().trim().isEmpty() ? null : req.zoneCode().trim().toUpperCase());
        }
        if (req.category() != null) {
            supplier.setCategory(req.category().trim().isEmpty() ? null : req.category().trim());
        }
        Supplier saved = supplierRepository.save(supplier);
        auditLogService.log(actor, "SUPPLIER_UPDATE", "Updated supplier #" + saved.getSupplierId() + " (" + saved.getCompanyName() + ")");
        return SupplierDto.fromEntity(saved);
    }

    @Transactional
    public void deleteSupplier(Integer id, User actor) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found with ID: " + id));

        supplier.setIsActive(false);
        supplierRepository.save(supplier);
        auditLogService.log(actor, "SUPPLIER_DELETE", "Deactivated supplier #" + supplier.getSupplierId() + " (" + supplier.getCompanyName() + ")");
    }

    @Transactional
    public ApprovalRequestDto requestSupplierCreate(SupplierRequest req, User actor) {
        if (req == null || req.companyName() == null || req.companyName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name is required");
        }
        String name = req.companyName().trim();
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name cannot exceed 50 characters");
        }
        if (supplierRepository.findByCompanyName(name).filter(s -> Boolean.TRUE.equals(s.getIsActive())).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active supplier with this name already exists");
        }
        ensureCategoryNotDuplicate(req.category(), null);

        String zoneCode = req.zoneCode() != null && !req.zoneCode().trim().isEmpty() ? req.zoneCode().trim().toUpperCase() : "";
        String category = req.category() != null && !req.category().trim().isEmpty() ? req.category().trim() : "";

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("operation", "CREATE");
        payloadMap.put("companyName", name);
        payloadMap.put("category", category);
        payloadMap.put("zoneCode", zoneCode);
        payloadMap.put("reason", "Request to add new supplier: " + name +
                " (Category: " + (category.isEmpty() ? "-" : category) +
                ", Zone: " + (zoneCode.isEmpty() ? "None" : zoneCode) + ")");

        String payloadJson = serializeSupplierPayload(payloadMap, name);
        String detail = "Request to add new supplier: " + name +
                " (Category: " + (category.isEmpty() ? "-" : category) +
                ", Zone: " + (zoneCode.isEmpty() ? "None" : zoneCode) + ")";
        if (detail.length() > 255) {
            detail = detail.substring(0, 252) + "...";
        }

        ApprovalRequest saved = approvalRequestRepository.save(
                ApprovalRequestService.buildPendingRequest(
                        RequestType.SUPPLIER_CREATE, TargetType.SUPPLIER, null, payloadJson, detail, actor
                )
        );

        auditLogService.log(actor, "REQUEST_CREATE", "Submitted request to create supplier: " + name);
        return ApprovalRequestDto.fromEntity(saved);
    }

    @Transactional
    public ApprovalRequestDto requestSupplierUpdate(Integer id, SupplierRequest req, User actor) {
        if (req == null || req.companyName() == null || req.companyName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name is required");
        }
        String name = req.companyName().trim();
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name cannot exceed 50 characters");
        }

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found with ID: " + id));

        supplierRepository.findByCompanyName(name).ifPresent(existing -> {
            if (!existing.getSupplierId().equals(id) && Boolean.TRUE.equals(existing.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another active supplier already exists with name: " + name);
            }
        });

        if (approvalRequestRepository.existsByRequestTypeAndTargetIdAndStatus(
                RequestType.SUPPLIER_UPDATE, id, ApprovalStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending update request already exists for this supplier");
        }
        ensureCategoryNotDuplicate(req.category(), id);

        String zoneCode = req.zoneCode() != null && !req.zoneCode().trim().isEmpty() ? req.zoneCode().trim().toUpperCase() : "";
        String category = req.category() != null && !req.category().trim().isEmpty() ? req.category().trim() : "";

        String oldName = supplier.getCompanyName() != null ? supplier.getCompanyName().trim() : "";
        String oldCat = supplier.getCategory() != null ? supplier.getCategory().trim() : "";
        String oldZone = supplier.getZoneCode() != null ? supplier.getZoneCode().trim().toUpperCase() : "";
        if (name.equals(oldName) && category.equals(oldCat) && zoneCode.equals(oldZone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No changes detected for this supplier");
        }

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("operation", "UPDATE");
        payloadMap.put("supplierId", supplier.getSupplierId());
        payloadMap.put("companyName", name);
        payloadMap.put("oldCompanyName", supplier.getCompanyName());
        payloadMap.put("category", category);
        payloadMap.put("oldCategory", supplier.getCategory() != null ? supplier.getCategory() : "");
        payloadMap.put("zoneCode", zoneCode);
        payloadMap.put("oldZoneCode", supplier.getZoneCode() != null ? supplier.getZoneCode() : "");
        payloadMap.put("reason", "Request to update supplier #" + supplier.getSupplierId() +
                " (" + supplier.getCompanyName() + " -> " + name + ")");

        String payloadJson = serializeSupplierPayload(payloadMap, name);
        String detail = "Request to edit supplier #" + supplier.getSupplierId() + ": " +
                supplier.getCompanyName() + " -> " + name +
                " (Category: " + (category.isEmpty() ? "-" : category) +
                ", Zone: " + (zoneCode.isEmpty() ? "None" : zoneCode) + ")";
        if (detail.length() > 255) {
            detail = detail.substring(0, 252) + "...";
        }

        ApprovalRequest saved = approvalRequestRepository.save(
                ApprovalRequestService.buildPendingRequest(
                        RequestType.SUPPLIER_UPDATE, TargetType.SUPPLIER, supplier.getSupplierId(), payloadJson, detail, actor
                )
        );

        auditLogService.log(actor, "REQUEST_CREATE", "Submitted request to update supplier #" + supplier.getSupplierId() + " (" + name + ")");
        return ApprovalRequestDto.fromEntity(saved);
    }

    @Transactional
    public ApprovalRequestDto requestSupplierDeactivate(Integer id, User actor) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found with ID: " + id));

        if (approvalRequestRepository.existsByRequestTypeAndTargetIdAndStatus(
                RequestType.SUPPLIER_DEACTIVATE, id, ApprovalStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending deactivation request already exists for this supplier");
        }

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("operation", "DELETE");
        payloadMap.put("supplierId", supplier.getSupplierId());
        payloadMap.put("companyName", supplier.getCompanyName());
        payloadMap.put("category", supplier.getCategory() != null ? supplier.getCategory() : "");
        payloadMap.put("zoneCode", supplier.getZoneCode() != null ? supplier.getZoneCode() : "");
        payloadMap.put("reason", "Request to deactivate supplier #" + supplier.getSupplierId() + " (" + supplier.getCompanyName() + ")");

        String payloadJson = serializeSupplierPayload(payloadMap, supplier.getCompanyName());
        String detail = "Request to delete supplier #" + supplier.getSupplierId() + ": " + supplier.getCompanyName() +
                " (Category: " + (supplier.getCategory() != null ? supplier.getCategory() : "-") +
                ", Zone: " + (supplier.getZoneCode() != null ? supplier.getZoneCode() : "None") + ")";
        if (detail.length() > 255) {
            detail = detail.substring(0, 252) + "...";
        }

        ApprovalRequest saved = approvalRequestRepository.save(
                ApprovalRequestService.buildPendingRequest(
                        RequestType.SUPPLIER_DEACTIVATE, TargetType.SUPPLIER, supplier.getSupplierId(), payloadJson, detail, actor
                )
        );

        auditLogService.log(actor, "REQUEST_CREATE", "Submitted request to deactivate supplier #" + supplier.getSupplierId() + " (" + supplier.getCompanyName() + ")");
        return ApprovalRequestDto.fromEntity(saved);
    }

    private void ensureCategoryNotDuplicate(String category, Integer excludeSupplierId) {
        if (category == null || category.trim().isEmpty()) {
            return;
        }
        String normalized = category.trim();
        boolean existsInSuppliers = supplierRepository.findByIsActiveTrue().stream()
                .filter(s -> excludeSupplierId == null || !s.getSupplierId().equals(excludeSupplierId))
                .anyMatch(s -> s.getCategory() != null && s.getCategory().trim().equalsIgnoreCase(normalized));
        if (existsInSuppliers) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Products \"" + normalized + "\" already exists and cannot be created");
        }

        boolean existsInPendingCreates = approvalRequestRepository
                .findByRequestTypeAndStatus(RequestType.SUPPLIER_CREATE, ApprovalStatus.PENDING)
                .stream()
                .anyMatch(r -> {
                    if (r.getPayload() == null || r.getPayload().isBlank()) return false;
                    try {
                        String pendingCat = objectMapper.readTree(r.getPayload()).path("category").asText("").trim();
                        return !pendingCat.isEmpty() && pendingCat.equalsIgnoreCase(normalized);
                    } catch (Exception e) {
                        return false;
                    }
                });
        if (existsInPendingCreates) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Products \"" + normalized + "\" already exists in a pending request and cannot be created");
        }
    }

    private String serializeSupplierPayload(Map<String, Object> payloadMap, String fallbackName) {
        try {
            return objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            return "{\"companyName\":\"" + fallbackName + "\"}";
        }
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

        ApprovalRequest approvalRequest = approvalRequestRepository.save(
                ApprovalRequestService.buildPendingRequest(
                        RequestType.PO_CREATE, TargetType.PRODUCT, null, payloadJson, detailText, currentUser
                )
        );

        // 4. Create Purchase Order
        PurchaseOrder po = purchaseOrderRepository.save(PurchaseOrder.builder()
                .supplier(supplier)
                .createdBy(currentUser)
                .request(approvalRequest)
                .orderDatetime(LocalDateTime.now())
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
        auditLogService.log(currentUser, "PO_CREATE", "Created Purchase Order #" + po.getPoId() +
                " from " + supplier.getCompanyName() + " (Total: ฿" + formattedTotal + ")");

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
