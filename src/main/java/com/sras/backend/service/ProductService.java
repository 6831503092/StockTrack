package com.sras.backend.service;

import com.sras.backend.dto.ProductDto;
import com.sras.backend.dto.ProductUpdateRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.User;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public ProductDto getProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with ID: " + id));
        return ProductDto.fromEntity(product);
    }

    @Transactional
    public ProductDto updateProduct(Integer id, ProductUpdateRequest req, User managerUser) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with ID: " + id));

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
        auditLogService.log(managerUser, com.sras.backend.enums.AuditAction.PRODUCT_UPDATE, "PRODUCT", saved.getProductId(),
                "Updated product #" + saved.getProductId() + " (" + saved.getProductName() + ")");

        return ProductDto.fromEntity(saved);
    }
}
