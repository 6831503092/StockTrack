package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.StockMovement;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.MovementStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.StockMovementRepository;
import com.sras.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMoveExecutor implements ApprovalExecutor {

    private final StockMovementRepository stockMovementRepository;
    private final ZoneRepository zoneRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RequestType supports() {
        return RequestType.PRODUCT_MOVE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        if (stockMovementRepository.findByRequestRequestId(request.getRequestId()).isPresent()) {
            log.info("Stock movement already exists for request #{}", request.getRequestId());
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(request.getPayload());
            int sourceZoneId = node.path("sourceZoneId").asInt(request.getTargetId() != null ? request.getTargetId() : 0);
            int targetZoneId = node.path("targetZoneId").asInt(0);

            Zone sourceZone = zoneRepository.findById(sourceZoneId).orElse(null);
            Zone targetZone = zoneRepository.findById(targetZoneId).orElse(null);

            if (sourceZone == null || targetZone == null) {
                log.warn("Cannot create StockMovement: Zone not found (source={}, target={})", sourceZoneId, targetZoneId);
                return;
            }

            List<Product> products = productRepository.findByZoneZoneIdAndIsActiveTrue(sourceZoneId);
            Product primaryProduct = products.isEmpty() ? null : products.get(0);
            if (primaryProduct == null) {
                log.warn("No products to move from zone {}", sourceZoneId);
                return;
            }

            int totalQty = products.stream()
                    .mapToInt(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0)
                    .sum();

            StockMovement movement = StockMovement.builder()
                    .request(request)
                    .product(primaryProduct)
                    .fromZone(sourceZone)
                    .toZone(targetZone)
                    .quantity(Math.max(totalQty, 1))
                    .status(MovementStatus.PENDING_VERIFICATION)
                    .build();

            stockMovementRepository.save(movement);
            log.info("PRODUCT_MOVE executed: created StockMovement #{} for request #{} awaiting staff verification",
                    movement.getMovementId(), request.getRequestId());
        } catch (Exception e) {
            log.error("Failed to execute PRODUCT_MOVE for request #{}", request.getRequestId(), e);
            throw new RuntimeException("Failed to execute product move: " + e.getMessage(), e);
        }
    }
}
