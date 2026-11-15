package com.sras.backend.service.approval;

import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.enums.PurchaseOrderStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.BusinessRuleException;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PoCreateExecutorTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @InjectMocks
    private PoCreateExecutor executor;

    private ApprovalRequest request;
    private PurchaseOrder po;

    @BeforeEach
    void setUp() {
        request = ApprovalRequest.builder()
                .requestId(10)
                .requestType(RequestType.PO_CREATE)
                .targetId(101)
                .build();

        po = new PurchaseOrder();
        po.setPoId(101);
        po.setStatus(PurchaseOrderStatus.PENDING);
    }

    @Test
    @DisplayName("supports: returns PO_CREATE")
    void testSupports() {
        assertEquals(RequestType.PO_CREATE, executor.supports());
    }

    @Test
    @DisplayName("execute: successfully approves pending PO")
    void testExecute_Success() {
        when(purchaseOrderRepository.findById(101)).thenReturn(Optional.of(po));

        executor.execute(request);

        assertEquals(PurchaseOrderStatus.APPROVED, po.getStatus());
        verify(purchaseOrderRepository).save(po);
    }

    @Test
    @DisplayName("execute: throws NotFoundException when PO is not found")
    void testExecute_NotFound() {
        when(purchaseOrderRepository.findById(101)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> executor.execute(request));
    }

    @Test
    @DisplayName("execute: throws BusinessRuleException when PO is not PENDING")
    void testExecute_NotPending() {
        po.setStatus(PurchaseOrderStatus.APPROVED);
        when(purchaseOrderRepository.findById(101)).thenReturn(Optional.of(po));

        assertThrows(BusinessRuleException.class, () -> executor.execute(request));
    }
}
