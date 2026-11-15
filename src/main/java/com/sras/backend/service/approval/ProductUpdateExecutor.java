package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.payload.ProductUpdatePayload;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductUpdateExecutor extends BaseApprovalExecutor<ProductUpdatePayload> {

    private final ProductRepository productRepository;

    public ProductUpdateExecutor(ProductRepository productRepository, ObjectMapper objectMapper) {
        super(objectMapper);
        this.productRepository = productRepository;
    }

    @Override
    public RequestType supports() {
        return RequestType.PRODUCT_UPDATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        ProductUpdatePayload payload = parsePayload(request, ProductUpdatePayload.class);
        int productId = payload.productId() != null && payload.productId() > 0
                ? payload.productId()
                : (request.getTargetId() != null ? request.getTargetId() : 0);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with ID: " + productId));

        if (payload.productName() != null && !payload.productName().isBlank()) {
            product.setProductName(payload.productName().trim());
        }
        if (payload.sellingPrice() != null) {
            product.setSellingPrice(payload.sellingPrice());
        }
        if (payload.defaultCost() != null) {
            product.setDefaultCost(payload.defaultCost());
        }
        if (payload.currentStock() != null) {
            product.setCurrentStock(payload.currentStock());
        }
        if (payload.minStockAlert() != null) {
            product.setMinStockAlert(payload.minStockAlert());
        }

        productRepository.save(product);
        log.info("PRODUCT_UPDATE executed: updated product #{} ({})", productId, product.getProductName());
    }
}
