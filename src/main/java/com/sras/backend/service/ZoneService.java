package com.sras.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.dto.ProductDto;
import com.sras.backend.dto.ProductMoveRequest;
import com.sras.backend.dto.ProductUpdateRequest;
import com.sras.backend.dto.ZoneCreateRequest;
import com.sras.backend.dto.ZoneDto;
import com.sras.backend.dto.ZoneProductSummaryDto;
import com.sras.backend.dto.ZoneUpdateRequest;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.User;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.TargetType;
import com.sras.backend.repository.ApprovalRequestRepository;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final ProductRepository productRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ZoneDto> getAllZones() {
        List<Zone> zones = zoneRepository.findAll();
        List<Product> products = productRepository.findByIsActiveTrue();

        Map<Integer, Long> countMap = products.stream()
                .filter(p -> p.getZone() != null)
                .collect(Collectors.groupingBy(p -> p.getZone().getZoneId(), Collectors.counting()));

        Map<Integer, Long> unitsMap = products.stream()
                .filter(p -> p.getZone() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getZone().getZoneId(),
                        Collectors.summingLong(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0L)
                ));

        return zones.stream()
                .sorted(Comparator.comparing(Zone::getZoneCode))
                .map(z -> ZoneDto.fromEntity(
                        z,
                        countMap.getOrDefault(z.getZoneId(), 0L),
                        unitsMap.getOrDefault(z.getZoneId(), 0L)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ZoneProductSummaryDto getZoneProductSummary() {
        List<Zone> allZones = zoneRepository.findAll();
        long activeZonesCount = allZones.size();
        List<Product> products = productRepository.findByIsActiveTrue();

        long storedUnits = 0;
        long lowStockCount = 0;

        for (Product p : products) {
            int stock = p.getCurrentStock() != null ? p.getCurrentStock() : 0;
            int minStock = p.getMinStockAlert() != null ? p.getMinStockAlert() : 20;
            storedUnits += stock;
            if (stock <= minStock) {
                lowStockCount++;
            }
        }

        long totalCapacity = allZones.stream()
                .mapToLong(z -> z.getCapacity() != null ? z.getCapacity() : 500L)
                .sum();

        return ZoneProductSummaryDto.builder()
                .activeZonesCount(activeZonesCount)
                .storedUnits(storedUnits)
                .totalProducts(products.size())
                .lowStockCount(lowStockCount)
                .totalCapacity(totalCapacity)
                .build();
    }

    @Transactional
    public ZoneDto createZone(ZoneCreateRequest req, User actor) {
        if (req == null || req.zoneCode() == null || req.zoneCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone code is required");
        }

        String code = req.zoneCode().trim().toUpperCase();
        if (code.matches("^\\d+$")) {
            code = "Z" + code;
        }

        String name = (req.zoneName() != null && !req.zoneName().trim().isEmpty())
                ? req.zoneName().trim()
                : ("Zone " + (code.startsWith("Z") ? code.substring(1) : code));

        if (!code.matches("^[A-Z0-9]{2,5}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone code must be 2-5 alphanumeric characters (e.g. Z5)");
        }
        if (name.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone name cannot exceed 30 characters");
        }
        if (zoneRepository.existsByZoneCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Zone code already exists: " + code);
        }
        if (zoneRepository.existsByZoneName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Zone name already exists: " + name);
        }

        int capacity = (req.capacity() != null && req.capacity() > 0) ? req.capacity() : 500;

        Zone zone = new Zone();
        zone.setZoneCode(code);
        zone.setZoneName(name);
        zone.setCapacity(capacity);

        Zone saved = zoneRepository.save(zone);
        auditLogService.log(actor, "ZONE_CREATE", "Created zone " + saved.getZoneCode() + " (" + saved.getZoneName() + ", Capacity: " + capacity + ")");
        return ZoneDto.fromEntity(saved, 0L, 0L);
    }

    @Transactional
    public ApprovalRequestDto requestZoneCreate(ZoneCreateRequest req, User actor) {
        if (req == null || req.zoneCode() == null || req.zoneCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone code is required");
        }

        String code = req.zoneCode().trim().toUpperCase();
        if (code.matches("^\\d+$")) {
            code = "Z" + code;
        }

        String name = (req.zoneName() != null && !req.zoneName().trim().isEmpty())
                ? req.zoneName().trim()
                : ("Zone " + (code.startsWith("Z") ? code.substring(1) : code));

        if (!code.matches("^[A-Z0-9]{2,5}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone code must be 2-5 alphanumeric characters (e.g. Z5)");
        }
        if (name.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone name cannot exceed 30 characters");
        }
        if (zoneRepository.existsByZoneCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Zone code already exists: " + code);
        }
        if (zoneRepository.existsByZoneName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Zone name already exists: " + name);
        }

        List<ApprovalRequest> pendingCreates = approvalRequestRepository.findByRequestTypeAndStatus(
                RequestType.ZONE_CREATE, ApprovalStatus.PENDING);
        for (ApprovalRequest pending : pendingCreates) {
            if (pending.getPayload() != null && pending.getPayload().contains("\"zoneCode\":\"" + code + "\"")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A pending request to create zone " + code + " already exists.");
            }
        }

        int capacity = (req.capacity() != null && req.capacity() > 0) ? req.capacity() : 500;

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("zoneCode", code);
        payloadMap.put("zoneName", name);
        payloadMap.put("capacity", capacity);
        payloadMap.put("reason", "Request to create new warehouse zone " + code + " (" + name + ") with capacity of " + capacity + " units.");

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize zone create payload");
        }

        String detail = "Create new zone: " + code + " (" + name + ") | Capacity: " + capacity + " Units";

        ApprovalRequest saved = approvalRequestRepository.save(
                ApprovalRequestService.buildPendingRequest(
                        RequestType.ZONE_CREATE, TargetType.ZONE, null, payloadJson, detail, actor
                )
        );

        auditLogService.log(actor, "REQUEST_SUBMIT",
                "Requested creation of zone " + code + " (" + name + ", Capacity: " + capacity + " Units)");

        return ApprovalRequestDto.fromEntity(saved);
    }

    @Transactional
    public ZoneDto updateZone(String zoneCode, ZoneUpdateRequest req, User actor) {
        if (zoneCode == null || zoneCode.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone code is required");
        }
        String code = zoneCode.trim().toUpperCase();
        Zone zone = zoneRepository.findByZoneCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone not found: " + code));

        if (req != null) {
            if (req.zoneName() != null && !req.zoneName().trim().isEmpty()) {
                String name = req.zoneName().trim();
                if (name.length() > 30) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone name cannot exceed 30 characters");
                }
                zoneRepository.findByZoneName(name).ifPresent(existing -> {
                    if (!existing.getZoneId().equals(zone.getZoneId())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Zone name already in use: " + name);
                    }
                });
                zone.setZoneName(name);
            }

            if (req.capacity() != null) {
                if (req.capacity() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone capacity must be greater than 0");
                }
                zone.setCapacity(req.capacity());
            }
        }

        Zone saved = zoneRepository.save(zone);

        List<Product> products = productRepository.findByIsActiveTrue();
        long productCount = products.stream().filter(p -> p.getZone() != null && p.getZone().getZoneId().equals(saved.getZoneId())).count();
        long totalUnits = products.stream().filter(p -> p.getZone() != null && p.getZone().getZoneId().equals(saved.getZoneId()))
                .mapToLong(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0L).sum();

        auditLogService.log(actor, "ZONE_UPDATE", "Updated zone " + saved.getZoneCode() + " (Capacity: " + saved.getCapacity() + ", Name: " + saved.getZoneName() + ")");
        return ZoneDto.fromEntity(saved, productCount, totalUnits);
    }

    @Transactional
    public ApprovalRequestDto requestZoneUpdate(String zoneCode, ZoneUpdateRequest req, User actor) {
        if (zoneCode == null || zoneCode.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone code is required");
        }
        String code = zoneCode.trim().toUpperCase();
        Zone zone = zoneRepository.findByZoneCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone not found: " + code));

        String newName = zone.getZoneName();
        if (req != null && req.zoneName() != null && !req.zoneName().trim().isEmpty()) {
            newName = req.zoneName().trim();
            if (newName.length() > 30) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone name cannot exceed 30 characters");
            }
            String finalName = newName;
            zoneRepository.findByZoneName(finalName).ifPresent(existing -> {
                if (!existing.getZoneId().equals(zone.getZoneId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Zone name already in use: " + finalName);
                }
            });
        }

        int oldCapacity = zone.getCapacity() != null ? zone.getCapacity() : 500;
        int newCapacity = oldCapacity;
        if (req != null && req.capacity() != null) {
            if (req.capacity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone capacity must be greater than 0");
            }
            newCapacity = req.capacity();
        }

        if (Objects.equals(zone.getZoneName(), newName) && oldCapacity == newCapacity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No changes detected. Please modify capacity before sending a request.");
        }

        if (approvalRequestRepository.existsByRequestTypeAndTargetIdAndStatus(
                RequestType.ZONE_UPDATE, zone.getZoneId(), ApprovalStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A pending capacity update request already exists for zone " + zone.getZoneCode() + ".");
        }

        List<Product> zoneProducts = zone.getZoneId() != null
                ? productRepository.findByZoneZoneIdAndIsActiveTrue(zone.getZoneId())
                : List.of();
        int storedUnits = zoneProducts != null
                ? zoneProducts.stream().mapToInt(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0).sum()
                : 0;

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("zoneId", zone.getZoneId());
        payloadMap.put("zoneCode", zone.getZoneCode());
        payloadMap.put("zoneName", newName);
        payloadMap.put("oldZoneName", zone.getZoneName());
        payloadMap.put("capacity", newCapacity);
        payloadMap.put("oldCapacity", oldCapacity);
        payloadMap.put("storedUnits", storedUnits);
        payloadMap.put("reason", "Request to update capacity of " + zone.getZoneCode() + " (" + newName + ") from " + oldCapacity + " to " + newCapacity + " units.");

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize zone update payload");
        }

        String detail = "Edit Zone Capacity: " + zone.getZoneCode() + " (" + newName + ") | " + oldCapacity + " -> " + newCapacity + " Units";

        ApprovalRequest saved = approvalRequestRepository.save(
                ApprovalRequestService.buildPendingRequest(
                        RequestType.ZONE_UPDATE, TargetType.ZONE, zone.getZoneId(), payloadJson, detail, actor
                )
        );

        auditLogService.log(actor, "REQUEST_SUBMIT",
                "Requested capacity update for zone " + zone.getZoneCode() + " (" + oldCapacity + " -> " + newCapacity + " Units)");

        return ApprovalRequestDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getZoneProducts(String zoneCode, String search) {
        String q = search != null ? search.trim().toLowerCase() : "";
        String zFilter = zoneCode != null ? zoneCode.trim() : "";

        return productRepository.findByIsActiveTrue().stream()
                .filter(p -> zFilter.isEmpty() || zFilter.equalsIgnoreCase("ALL") ||
                        (p.getZone() != null && p.getZone().getZoneCode().equalsIgnoreCase(zFilter)))
                .filter(p -> {
                    if (q.isEmpty()) return true;
                    boolean matchId = p.getProductId() != null && String.valueOf(p.getProductId()).contains(q);
                    boolean matchName = p.getProductName() != null && p.getProductName().toLowerCase().contains(q);
                    boolean matchZone = p.getZone() != null && (
                            p.getZone().getZoneCode().toLowerCase().contains(q) ||
                            p.getZone().getZoneName().toLowerCase().contains(q)
                    );
                    return matchId || matchName || matchZone;
                })
                .sorted(Comparator.comparing(Product::getProductId))
                .map(ProductDto::fromEntity)
                .toList();
    }

    @Transactional
    public ApprovalRequestDto submitProductMoveRequest(ProductMoveRequest req, User adminUser) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid move request");
        }

        Zone sourceZone = null;
        List<Product> sourceProducts = new ArrayList<>();

        if (req.sourceZoneCode() != null && !req.sourceZoneCode().isBlank()) {
            sourceZone = zoneRepository.findByZoneCode(req.sourceZoneCode().trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source zone not found: " + req.sourceZoneCode()));
            sourceProducts = productRepository.findByZoneZoneIdAndIsActiveTrue(sourceZone.getZoneId());
        } else if (req.productIds() != null && !req.productIds().isEmpty()) {
            sourceProducts = productRepository.findAllById(req.productIds());
            if (!sourceProducts.isEmpty() && sourceProducts.get(0).getZone() != null) {
                sourceZone = sourceProducts.get(0).getZone();
            }
        }

        if (sourceZone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please specify a valid source zone or products to move");
        }
        if (sourceProducts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active products found in " + sourceZone.getZoneName());
        }

        Zone targetZone = null;
        if (req.targetZoneCode() != null && !req.targetZoneCode().isBlank()) {
            targetZone = zoneRepository.findByZoneCode(req.targetZoneCode().trim()).orElse(null);
        } else if (req.targetZoneId() != null) {
            targetZone = zoneRepository.findById(req.targetZoneId()).orElse(null);
        }

        if (targetZone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target zone not found");
        }
        if (sourceZone.getZoneId().equals(targetZone.getZoneId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Source and target zone cannot be the same (" + targetZone.getZoneCode() + ")");
        }

        List<Product> targetProducts = targetZone.getZoneId() != null
                ? productRepository.findByZoneZoneIdAndIsActiveTrue(targetZone.getZoneId())
                : List.of();
        if (targetProducts == null) {
            targetProducts = List.of();
        }

        boolean isSwap = !targetProducts.isEmpty();

        List<Map<String, Object>> sourceDetails = toProductMoveItems(sourceProducts, sourceZone.getZoneCode(), targetZone.getZoneCode());
        List<Map<String, Object>> targetDetails = toProductMoveItems(targetProducts, targetZone.getZoneCode(), sourceZone.getZoneCode());

        String reason = req.reason() != null && !req.reason().isBlank()
                ? req.reason().trim()
                : (isSwap
                    ? "Swap zones " + sourceZone.getZoneCode() + " and " + targetZone.getZoneCode()
                    : "Transfer entire Zone " + sourceZone.getZoneCode() + " to " + targetZone.getZoneCode());

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("operation", isSwap ? "ZONE_SWAP" : "ZONE_MOVE");
        payloadMap.put("sourceZoneId", sourceZone.getZoneId());
        payloadMap.put("sourceZoneCode", sourceZone.getZoneCode());
        payloadMap.put("sourceZoneName", sourceZone.getZoneName());
        payloadMap.put("targetZoneId", targetZone.getZoneId());
        payloadMap.put("targetZoneCode", targetZone.getZoneCode());
        payloadMap.put("targetZoneName", targetZone.getZoneName());
        payloadMap.put("sourceProducts", sourceDetails);
        payloadMap.put("targetProducts", targetDetails);
        payloadMap.put("products", sourceDetails);
        payloadMap.put("reason", reason);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize request payload");
        }

        String detail = isSwap
                ? "Swap Zone " + sourceZone.getZoneCode() + " (" + sourceProducts.size() + " items) <-> Zone " + targetZone.getZoneCode() + " (" + targetProducts.size() + " items)"
                : "Move entire Zone " + sourceZone.getZoneCode() + " (" + sourceProducts.size() + " items) -> Zone " + targetZone.getZoneCode();
        if (detail.length() > 255) {
            detail = detail.substring(0, 252) + "...";
        }

        ApprovalRequest saved = approvalRequestRepository.save(
                ApprovalRequestService.buildPendingRequest(
                        RequestType.PRODUCT_MOVE, TargetType.ZONE, sourceZone.getZoneId(), payloadJson, detail, adminUser
                )
        );

        auditLogService.log(adminUser, "REQUEST_SUBMIT",
                (isSwap ? "Requested swap between Zone " : "Requested move of Zone ")
                + sourceZone.getZoneCode() + " and Zone " + targetZone.getZoneCode());

        return ApprovalRequestDto.fromEntity(saved);
    }

    @Transactional
    public ApprovalRequestDto submitProductUpdateRequest(ProductUpdateRequest req, User adminUser) {
        if (req == null || req.productId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product edit request");
        }

        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + req.productId()));

        String newName = (req.productName() != null && !req.productName().isBlank())
                ? req.productName().trim()
                : product.getProductName();
        BigDecimal newPrice = req.sellingPrice() != null ? req.sellingPrice() : product.getSellingPrice();
        Integer newStock = req.currentStock() != null ? req.currentStock() : product.getCurrentStock();

        boolean nameChanged = !Objects.equals(
                product.getProductName() != null ? product.getProductName().trim() : "",
                newName
        );
        boolean priceChanged = (newPrice != null && product.getSellingPrice() != null)
                ? newPrice.compareTo(product.getSellingPrice()) != 0
                : !Objects.equals(newPrice, product.getSellingPrice());
        boolean stockChanged = !Objects.equals(newStock, product.getCurrentStock());

        if (!nameChanged && !priceChanged && !stockChanged) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No changes detected. Please modify at least one field.");
        }

        if (approvalRequestRepository.existsByRequestTypeAndTargetIdAndStatus(
                RequestType.PRODUCT_UPDATE, product.getProductId(), ApprovalStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending edit request already exists for this product.");
        }

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("productId", product.getProductId());
        payloadMap.put("productName", newName);
        payloadMap.put("oldProductName", product.getProductName());
        payloadMap.put("sellingPrice", newPrice);
        payloadMap.put("oldSellingPrice", product.getSellingPrice());
        payloadMap.put("currentStock", newStock);
        payloadMap.put("oldCurrentStock", product.getCurrentStock());
        payloadMap.put("defaultCost", product.getDefaultCost());
        payloadMap.put("minStockAlert", product.getMinStockAlert());
        payloadMap.put("zoneCode", product.getZone() != null ? product.getZone().getZoneCode() : "-");

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            payloadJson = "{}";
        }

        String detail = "Edit Product #" + product.getProductId() + " (" + newName + ")";

        ApprovalRequest saved = approvalRequestRepository.save(
                ApprovalRequestService.buildPendingRequest(
                        RequestType.PRODUCT_UPDATE, TargetType.PRODUCT, product.getProductId(), payloadJson, detail, adminUser
                )
        );

        auditLogService.log(adminUser, "REQUEST_SUBMIT",
                "Requested edit for Product #" + product.getProductId() + " (" + newName + ")");

        return ApprovalRequestDto.fromEntity(saved);
    }

    private List<Map<String, Object>> toProductMoveItems(List<Product> products, String fromZoneCode, String toZoneCode) {
        return products.stream().map(p -> {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productId", p.getProductId());
            itemMap.put("productName", p.getProductName());
            itemMap.put("fromZoneCode", fromZoneCode);
            itemMap.put("toZoneCode", toZoneCode);
            itemMap.put("stock", p.getCurrentStock());
            return itemMap;
        }).toList();
    }
}
