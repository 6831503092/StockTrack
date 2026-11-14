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
public class SupplierUpdateExecutor implements ApprovalExecutor {

    private final SupplierRepository supplierRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RequestType supports() {
        return RequestType.SUPPLIER_UPDATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        try {
            int parsedSupplierId = request.getTargetId() != null ? request.getTargetId() : 0;
            String companyName = null;
            String category = null;
            String zoneCode = null;

            if (request.getPayload() != null && !request.getPayload().isBlank()) {
                JsonNode node = objectMapper.readTree(request.getPayload());
                if (parsedSupplierId <= 0 && node.has("supplierId")) {
                    parsedSupplierId = node.path("supplierId").asInt(0);
                }
                if (node.hasNonNull("companyName")) {
                    companyName = node.path("companyName").asText("").trim();
                }
                if (node.hasNonNull("category")) {
                    category = node.path("category").asText("").trim();
                }
                if (node.hasNonNull("zoneCode")) {
                    zoneCode = node.path("zoneCode").asText("").trim();
                }
            }

            final int supplierId = parsedSupplierId;
            if (supplierId <= 0) {
                throw new IllegalArgumentException("Invalid supplierId for SUPPLIER_UPDATE: " + supplierId);
            }

            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + supplierId));

            if (companyName != null && !companyName.isEmpty()) {
                supplier.setCompanyName(companyName);
            }
            if (category != null) {
                supplier.setCategory(category.isEmpty() ? null : category);
            }
            if (zoneCode != null) {
                supplier.setZoneCode(zoneCode.isEmpty() ? null : zoneCode.toUpperCase());
            }

            supplierRepository.save(supplier);
            log.info("SUPPLIER_UPDATE executed: updated supplier #{} ({})", supplier.getSupplierId(), supplier.getCompanyName());
        } catch (Exception e) {
            log.error("Failed to execute SUPPLIER_UPDATE for request #{}", request.getRequestId(), e);
            throw new RuntimeException("Failed to execute SUPPLIER_UPDATE: " + e.getMessage(), e);
        }
    }
}
