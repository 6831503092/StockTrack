package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Supplier;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierExecutorsTest {

    @Mock
    private SupplierRepository supplierRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SupplierCreateExecutor createExecutor;
    private SupplierUpdateExecutor updateExecutor;
    private SupplierDeactivateExecutor deactivateExecutor;
    private Supplier sampleSupplier;

    @BeforeEach
    void setUp() {
        createExecutor = new SupplierCreateExecutor(supplierRepository, objectMapper);
        updateExecutor = new SupplierUpdateExecutor(supplierRepository, objectMapper);
        deactivateExecutor = new SupplierDeactivateExecutor(supplierRepository, objectMapper);

        sampleSupplier = new Supplier();
        sampleSupplier.setSupplierId(20);
        sampleSupplier.setCompanyName("Supply Co");
        sampleSupplier.setCategory("Packaging");
        sampleSupplier.setZoneCode("Z01");
        sampleSupplier.setIsActive(true);
    }

    @Test
    @DisplayName("SUPPLIER_CREATE: creates new active supplier")
    void testCreate_Success() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(90)
                .requestType(RequestType.SUPPLIER_CREATE)
                .payload("{\"companyName\":\"Packers Inc\",\"category\":\"Logistics\",\"zoneCode\":\"Z02\"}")
                .build();

        when(supplierRepository.findByCompanyName("Packers Inc")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        createExecutor.execute(request);

        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    @DisplayName("SUPPLIER_CREATE: throws ValidationException when companyName is blank")
    void testCreate_BlankName() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(91)
                .requestType(RequestType.SUPPLIER_CREATE)
                .payload("{\"companyName\":\"\"}")
                .build();

        assertThrows(ValidationException.class, () -> createExecutor.execute(request));
    }

    @Test
    @DisplayName("SUPPLIER_UPDATE: updates company name, category, zoneCode")
    void testUpdate_Success() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(92)
                .requestType(RequestType.SUPPLIER_UPDATE)
                .targetId(20)
                .payload("{\"supplierId\":20,\"companyName\":\"Supply Co Updated\",\"category\":\"Raw Materials\",\"zoneCode\":\"Z03\"}")
                .build();

        when(supplierRepository.findById(20)).thenReturn(Optional.of(sampleSupplier));

        updateExecutor.execute(request);

        assertEquals("Supply Co Updated", sampleSupplier.getCompanyName());
        assertEquals("Raw Materials", sampleSupplier.getCategory());
        assertEquals("Z03", sampleSupplier.getZoneCode());
        verify(supplierRepository).save(sampleSupplier);
    }

    @Test
    @DisplayName("SUPPLIER_UPDATE: throws NotFoundException when supplier not found")
    void testUpdate_NotFound() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(93)
                .requestType(RequestType.SUPPLIER_UPDATE)
                .targetId(999)
                .payload("{\"supplierId\":999}")
                .build();

        when(supplierRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> updateExecutor.execute(request));
    }

    @Test
    @DisplayName("SUPPLIER_DEACTIVATE: sets isActive to false")
    void testDeactivate_Success() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(94)
                .requestType(RequestType.SUPPLIER_DEACTIVATE)
                .targetId(20)
                .build();

        when(supplierRepository.findById(20)).thenReturn(Optional.of(sampleSupplier));

        deactivateExecutor.execute(request);

        assertFalse(sampleSupplier.getIsActive());
        verify(supplierRepository).save(sampleSupplier);
    }
}
