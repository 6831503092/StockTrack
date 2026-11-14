package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Supplier;
import com.sras.backend.enums.RequestType;
import com.sras.backend.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierDeactivateExecutor implements ApprovalExecutor {

    private final SupplierRepository supplierRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RequestType supports() {
        return RequestType.SUPPLIER_DEACTIVATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        try {
            int parsedSupplierId = request.getTargetId() != null ? request.getTargetId() : 0;
            if (parsedSupplierId <= 0 && request.getPayload() != null && !request.getPayload().isBlank()) {
                JsonNode node = objectMapper.readTree(request.getPayload());
                parsedSupplierId = node.path("supplierId").asInt(0);
            }

            final int supplierId = parsedSupplierId;
            if (supplierId <= 0) {
                throw new IllegalArgumentException("Invalid supplierId for SUPPLIER_DEACTIVATE: " + supplierId);
            }

            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + supplierId));

            supplier.setIsActive(false);
            supplierRepository.save(supplier);
            log.info("SUPPLIER_DEACTIVATE executed: deactivated supplier #{} ({})", supplier.getSupplierId(), supplier.getCompanyName());
        } catch (Exception e) {
            log.error("Failed to execute SUPPLIER_DEACTIVATE for request #{}", request.getRequestId(), e);
            throw new RuntimeException("Failed to execute SUPPLIER_DEACTIVATE: " + e.getMessage(), e);
        }
    }
}
