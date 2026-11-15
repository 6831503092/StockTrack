package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.payload.SupplierPayload;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Supplier;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.SupplierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SupplierCreateExecutor extends BaseApprovalExecutor<SupplierPayload> {

    private final SupplierRepository supplierRepository;

    public SupplierCreateExecutor(SupplierRepository supplierRepository, ObjectMapper objectMapper) {
        super(objectMapper);
        this.supplierRepository = supplierRepository;
    }

    @Override
    public RequestType supports() {
        return RequestType.SUPPLIER_CREATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        SupplierPayload payload = parsePayload(request, SupplierPayload.class);
        String companyName = payload.companyName() != null ? payload.companyName().trim() : "";

        if (companyName.isEmpty()) {
            throw new ValidationException("Supplier company name is required in payload for SUPPLIER_CREATE");
        }

        Supplier supplier = supplierRepository.findByCompanyName(companyName).orElseGet(Supplier::new);
        supplier.setCompanyName(companyName);
        supplier.setCategory(payload.category() != null && !payload.category().isBlank() ? payload.category().trim() : null);
        supplier.setZoneCode(payload.zoneCode() != null && !payload.zoneCode().isBlank() ? payload.zoneCode().trim().toUpperCase() : null);
        supplier.setIsActive(true);

        Supplier saved = supplierRepository.save(supplier);
        log.info("SUPPLIER_CREATE executed: created/activated supplier #{} ({})", saved.getSupplierId(), saved.getCompanyName());
    }
}
