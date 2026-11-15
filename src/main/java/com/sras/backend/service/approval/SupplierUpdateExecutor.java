package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.payload.SupplierPayload;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Supplier;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.SupplierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SupplierUpdateExecutor extends BaseApprovalExecutor<SupplierPayload> {

    private final SupplierRepository supplierRepository;

    public SupplierUpdateExecutor(SupplierRepository supplierRepository, ObjectMapper objectMapper) {
        super(objectMapper);
        this.supplierRepository = supplierRepository;
    }

    @Override
    public RequestType supports() {
        return RequestType.SUPPLIER_UPDATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        SupplierPayload payload = parsePayloadOptional(request, SupplierPayload.class);
        int supplierId = request.getTargetId() != null && request.getTargetId() > 0
                ? request.getTargetId()
                : (payload != null && payload.supplierId() != null ? payload.supplierId() : 0);

        if (supplierId <= 0) {
            throw new ValidationException("Valid supplierId is required for SUPPLIER_UPDATE");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Supplier not found with ID: " + supplierId));

        if (payload != null) {
            if (payload.companyName() != null && !payload.companyName().isBlank()) {
                supplier.setCompanyName(payload.companyName().trim());
            }
            if (payload.category() != null) {
                supplier.setCategory(payload.category().trim());
            }
            if (payload.zoneCode() != null) {
                supplier.setZoneCode(payload.zoneCode().trim().toUpperCase());
            }
        }

        supplierRepository.save(supplier);
        log.info("SUPPLIER_UPDATE executed: updated supplier #{} ({})", supplier.getSupplierId(), supplier.getCompanyName());
    }
}
