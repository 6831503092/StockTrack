package com.sras.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.dto.ProductDto;
import com.sras.backend.dto.ProductMoveRequest;
import com.sras.backend.dto.ProductUpdateRequest;
import com.sras.backend.dto.ZoneDto;
import com.sras.backend.dto.ZoneProductSummaryDto;
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
        long activeZonesCount = zoneRepository.count();
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

        return ZoneProductSummaryDto.builder()
                .activeZonesCount(activeZonesCount)
                .storedUnits(storedUnits)
                .totalProducts(products.size())
                .lowStockCount(lowStockCount)
                .totalCapacity(activeZonesCount * 500)
                .build();
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
