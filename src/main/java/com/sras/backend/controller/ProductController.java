package com.sras.backend.controller;

import com.sras.backend.dto.ApiResponse;
import com.sras.backend.dto.ProductDto;
import com.sras.backend.dto.ProductUpdateRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.User;
import com.sras.backend.enums.Role;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.service.AuditLogService;
import com.sras.backend.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(
            @PathVariable("id") Integer id,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.resolveCurrentUser(session, xUserId);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with ID: " + id));
        return ResponseEntity.ok(ApiResponse.ok("Product retrieved successfully", ProductDto.fromEntity(product)));
    }

    @PutMapping("/{id:[0-9]+}")
    @Transactional
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable("id") Integer id,
            @RequestBody ProductUpdateRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User managerUser = authService.requireRole(session, xUserId, Role.MANAGER, Role.OWNER, Role.ADMIN);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with ID: " + id));

        if (req != null) {
            if (req.productName() != null && !req.productName().isBlank()) {
                product.setProductName(req.productName().trim());
            }
            if (req.defaultCost() != null && req.defaultCost().compareTo(BigDecimal.ZERO) >= 0) {
                product.setDefaultCost(req.defaultCost());
            }
            if (req.sellingPrice() != null && req.sellingPrice().compareTo(BigDecimal.ZERO) >= 0) {
                product.setSellingPrice(req.sellingPrice());
            }
            if (req.currentStock() != null && req.currentStock() >= 0) {
                product.setCurrentStock(req.currentStock());
            }
            if (req.minStockAlert() != null && req.minStockAlert() >= 0) {
                product.setMinStockAlert(req.minStockAlert());
            }
        }

        Product saved = productRepository.save(product);
        auditLogService.log(managerUser, "PRODUCT_UPDATE",
                "Updated product #" + saved.getProductId() + " (" + saved.getProductName() + ")");

        return ResponseEntity.ok(ApiResponse.ok("Product updated successfully", ProductDto.fromEntity(saved)));
    }
}
