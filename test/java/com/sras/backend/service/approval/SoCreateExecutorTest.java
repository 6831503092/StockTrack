package com.sras.backend.service.approval;

import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.SalesOrder;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.SalesOrderStatus;
import com.sras.backend.exception.BusinessRuleException;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.repository.SalesOrderRepository;
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
class SoCreateExecutorTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @InjectMocks
    private SoCreateExecutor executor;

    private ApprovalRequest request;
    private SalesOrder so;

    @BeforeEach
    void setUp() {
        request = ApprovalRequest.builder()
                .requestId(20)
                .requestType(RequestType.SO_CREATE)
                .targetId(201)
                .build();

        so = new SalesOrder();
        so.setSoId(201);
        so.setStatus(SalesOrderStatus.PENDING);
    }

    @Test
    @DisplayName("supports: returns SO_CREATE")
    void testSupports() {
        assertEquals(RequestType.SO_CREATE, executor.supports());
    }

    @Test
    @DisplayName("execute: successfully approves pending SO")
    void testExecute_Success() {
        when(salesOrderRepository.findById(201)).thenReturn(Optional.of(so));

        executor.execute(request);

        assertEquals(SalesOrderStatus.APPROVED, so.getStatus());
        verify(salesOrderRepository).save(so);
    }

    @Test
    @DisplayName("execute: throws NotFoundException when SO is not found")
    void testExecute_NotFound() {
        when(salesOrderRepository.findById(201)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> executor.execute(request));
    }

    @Test
    @DisplayName("execute: throws BusinessRuleException when SO is not in PENDING state")
    void testExecute_NotPending() {
        so.setStatus(SalesOrderStatus.APPROVED);
        when(salesOrderRepository.findById(201)).thenReturn(Optional.of(so));

        assertThrows(BusinessRuleException.class, () -> executor.execute(request));
    }
}
