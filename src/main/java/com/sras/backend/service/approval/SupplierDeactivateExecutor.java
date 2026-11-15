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
public class SupplierDeactivateExecutor extends BaseApprovalExecutor<SupplierPayload> {

    private final SupplierRepository supplierRepository;

    public SupplierDeactivateExecutor(SupplierRepository supplierRepository, ObjectMapper objectMapper) {
        super(objectMapper);
        this.supplierRepository = supplierRepository;
    }

    @Override
    public RequestType supports() {
        return RequestType.SUPPLIER_DEACTIVATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        SupplierPayload payload = parsePayloadOptional(request, SupplierPayload.class);
        int supplierId = request.getTargetId() != null && request.getTargetId() > 0
                ? request.getTargetId()
                : (payload != null && payload.supplierId() != null ? payload.supplierId() : 0);

        if (supplierId <= 0) {
            throw new ValidationException("Valid supplierId is required for SUPPLIER_DEACTIVATE");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Supplier not found with ID: " + supplierId));

        supplier.setIsActive(false);
        supplierRepository.save(supplier);
        log.info("SUPPLIER_DEACTIVATE executed: deactivated supplier #{} ({})", supplier.getSupplierId(), supplier.getCompanyName());
    }
}
