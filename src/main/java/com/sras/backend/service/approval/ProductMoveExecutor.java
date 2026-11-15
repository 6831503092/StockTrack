package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.payload.ProductMovePayload;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.StockMovement;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.MovementStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.StockMovementRepository;
import com.sras.backend.repository.ZoneRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ProductMoveExecutor extends BaseApprovalExecutor<ProductMovePayload> {

    private final StockMovementRepository stockMovementRepository;
    private final ZoneRepository zoneRepository;
    private final ProductRepository productRepository;

    public ProductMoveExecutor(
            StockMovementRepository stockMovementRepository,
            ZoneRepository zoneRepository,
            ProductRepository productRepository,
            ObjectMapper objectMapper
    ) {
        super(objectMapper);
        this.stockMovementRepository = stockMovementRepository;
        this.zoneRepository = zoneRepository;
        this.productRepository = productRepository;
    }

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

        ProductMovePayload payload = parsePayload(request, ProductMovePayload.class);

        // Resolve source zone
        Zone sourceZone = null;
        if (payload.sourceZoneId() != null && payload.sourceZoneId() > 0) {
            sourceZone = zoneRepository.findById(payload.sourceZoneId()).orElse(null);
        }
        if (sourceZone == null && request.getTargetId() != null && request.getTargetId() > 0) {
            sourceZone = zoneRepository.findById(request.getTargetId()).orElse(null);
        }
        if (sourceZone == null && payload.sourceZoneCode() != null && !payload.sourceZoneCode().isBlank()) {
            sourceZone = zoneRepository.findByZoneCode(payload.sourceZoneCode().trim()).orElse(null);
        }
        if (sourceZone == null) {
            throw new NotFoundException("Source zone not found for request #" + request.getRequestId());
        }

        // Resolve target zone
        Zone targetZone = null;
        if (payload.targetZoneId() != null && payload.targetZoneId() > 0) {
            targetZone = zoneRepository.findById(payload.targetZoneId()).orElse(null);
        }
        if (targetZone == null && payload.targetZoneCode() != null && !payload.targetZoneCode().isBlank()) {
            targetZone = zoneRepository.findByZoneCode(payload.targetZoneCode().trim()).orElse(null);
        }
        if (targetZone == null) {
            throw new NotFoundException("Target zone not found for request #" + request.getRequestId());
        }

        // Resolve primary product (no guessing first active product in DB)
        List<Product> products = productRepository.findByZoneZoneIdAndIsActiveTrue(sourceZone.getZoneId());
        Product primaryProduct = (products != null && !products.isEmpty()) ? products.get(0) : null;

        if (primaryProduct == null && payload.productId() != null && payload.productId() > 0) {
            primaryProduct = productRepository.findById(payload.productId()).orElse(null);
        }
        if (primaryProduct == null && payload.sourceProducts() != null && !payload.sourceProducts().isEmpty()) {
            Integer pId = payload.sourceProducts().get(0).productId();
            if (pId != null && pId > 0) {
                primaryProduct = productRepository.findById(pId).orElse(null);
            }
        }

        if (primaryProduct == null) {
            throw new NotFoundException("No active product found in source zone " + sourceZone.getZoneCode()
                    + " for request #" + request.getRequestId() + ". Cannot create stock movement.");
        }

        int totalQty = (products != null && !products.isEmpty())
                ? products.stream().mapToInt(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0).sum()
                : 1;

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
    }
}
