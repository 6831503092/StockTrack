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
public class SupplierCreateExecutor implements ApprovalExecutor {

    private final SupplierRepository supplierRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RequestType supports() {
        return RequestType.SUPPLIER_CREATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        if (request.getPayload() == null || request.getPayload().isBlank()) {
            throw new IllegalArgumentException("SUPPLIER_CREATE payload is empty for request #" + request.getRequestId());
        }

        try {
            JsonNode node = objectMapper.readTree(request.getPayload());
            String companyName = node.path("companyName").asText("").trim();
            String category = node.path("category").asText("").trim();
            String zoneCode = node.path("zoneCode").asText("").trim().toUpperCase();

            if (companyName.isEmpty()) {
                throw new IllegalArgumentException("Supplier company name is required in payload");
            }

            Supplier supplier = supplierRepository.findByCompanyName(companyName).orElseGet(Supplier::new);
            supplier.setCompanyName(companyName);
            supplier.setCategory(category.isEmpty() ? null : category);
            supplier.setZoneCode(zoneCode.isEmpty() ? null : zoneCode);
            supplier.setIsActive(true);

            Supplier saved = supplierRepository.save(supplier);
            log.info("SUPPLIER_CREATE executed: created/activated supplier #{} ({})", saved.getSupplierId(), saved.getCompanyName());
        } catch (Exception e) {
            log.error("Failed to execute SUPPLIER_CREATE for request #{}", request.getRequestId(), e);
            throw new RuntimeException("Failed to execute SUPPLIER_CREATE: " + e.getMessage(), e);
        }
    }
}
