package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.enums.RequestType;
import com.sras.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductUpdateExecutor implements ApprovalExecutor {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RequestType supports() {
        return RequestType.PRODUCT_UPDATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        if (request.getPayload() == null || request.getPayload().isBlank()) {
            log.warn("PRODUCT_UPDATE payload is empty for request #{}", request.getRequestId());
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(request.getPayload());
            int productId = node.path("productId").asInt(request.getTargetId() != null ? request.getTargetId() : 0);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

            if (node.hasNonNull("productName") && !node.get("productName").asText().isBlank()) {
                product.setProductName(node.get("productName").asText().trim());
            }
            if (node.hasNonNull("sellingPrice")) {
                product.setSellingPrice(new BigDecimal(node.get("sellingPrice").asText()));
            }
            if (node.hasNonNull("defaultCost")) {
                product.setDefaultCost(new BigDecimal(node.get("defaultCost").asText()));
            }
            if (node.hasNonNull("currentStock")) {
                product.setCurrentStock(node.get("currentStock").asInt());
            }
            if (node.hasNonNull("minStockAlert")) {
                product.setMinStockAlert(node.get("minStockAlert").asInt());
            }

            productRepository.save(product);
            log.info("PRODUCT_UPDATE executed: updated product #{} ({})", productId, product.getProductName());
        } catch (Exception e) {
            log.error("Failed to execute PRODUCT_UPDATE for request #{}", request.getRequestId(), e);
            throw new RuntimeException("Failed to execute product update: " + e.getMessage(), e);
        }
    }
}
