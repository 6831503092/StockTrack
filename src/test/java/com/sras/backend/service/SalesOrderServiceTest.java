package com.sras.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.SalesOrderCreateRequest;
import com.sras.backend.dto.SalesOrderDto;
import com.sras.backend.dto.SalesOrderItemRequest;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Customer;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.SalesOrder;
import com.sras.backend.entity.SalesOrderItem;
import com.sras.backend.entity.User;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.Role;
import com.sras.backend.enums.SalesOrderStatus;
import com.sras.backend.repository.ApprovalRequestRepository;
import com.sras.backend.repository.CustomerRepository;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.SalesOrderItemRepository;
import com.sras.backend.repository.SalesOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderServiceTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SalesOrderService salesOrderService;

    private User currentUser;
    private Customer customer;
    private Product product1;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setUserId(503092);
        currentUser.setFullName("Admin User");
        currentUser.setRole(Role.ADMIN);

        Zone z1 = new Zone();
        z1.setZoneCode("Z1");
        z1.setZoneName("Zone 1");

        customer = Customer.builder()
                .customerId(1)
                .customerName("RWTcomp.")
                .isActive(true)
                .build();

        product1 = new Product();
        product1.setProductId(17292);
        product1.setProductName("Meeting Table");
        product1.setZone(z1);
        product1.setDefaultCost(BigDecimal.valueOf(4000.00));
        product1.setSellingPrice(BigDecimal.valueOf(6000.00));
        product1.setCurrentStock(50);
        product1.setIsActive(true);
    }

    @Test
    @DisplayName("Successfully create Sales Order and submit approval request")
    void testCreateSalesOrder_Success() {
        when(customerRepository.findByCustomerName("RWTcomp.")).thenReturn(Optional.of(customer));
        when(productRepository.findById(17292)).thenReturn(Optional.of(product1));

        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(inv -> {
            ApprovalRequest req = inv.getArgument(0);
            req.setRequestId(101);
            return req;
        });

        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> {
            SalesOrder so = inv.getArgument(0);
            so.setSoId(5001);
            return so;
        });

        when(salesOrderItemRepository.save(any(SalesOrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        SalesOrderCreateRequest request = SalesOrderCreateRequest.builder()
                .customerName("RWTcomp.")
                .items(List.of(
                        SalesOrderItemRequest.builder()
                                .productId(17292)
                                .productName("Meeting Table")
                                .quantity(5)
                                .unitPrice(BigDecimal.valueOf(6000.00))
                                .build()
                ))
                .build();

        SalesOrderDto result = salesOrderService.createSalesOrder(request, currentUser);

        assertNotNull(result);
        assertEquals(5001, result.getId());
        assertEquals("RWTcomp.", result.getCustomer());
        assertEquals(5, result.getTotalQty());
        assertEquals(new BigDecimal("30000.00"), result.getTotalPrice());
        assertEquals("Pending", result.getStatus());

        verify(approvalRequestRepository, times(1)).save(any(ApprovalRequest.class));
        verify(salesOrderRepository, times(1)).save(any(SalesOrder.class));
        verify(salesOrderItemRepository, times(1)).save(any(SalesOrderItem.class));
        verify(auditLogService, times(1)).log(eq(currentUser), eq("SO_CREATE"), anyString());
    }

    @Test
    @DisplayName("Fail when requested quantity exceeds available stock")
    void testCreateSalesOrder_InsufficientStock() {
        when(customerRepository.findByCustomerName("RWTcomp.")).thenReturn(Optional.of(customer));
        when(productRepository.findById(17292)).thenReturn(Optional.of(product1)); // currentStock = 50

        SalesOrderCreateRequest request = SalesOrderCreateRequest.builder()
                .customerName("RWTcomp.")
                .items(List.of(
                        SalesOrderItemRequest.builder()
                                .productId(17292)
                                .productName("Meeting Table")
                                .quantity(100) // Exceeds 50
                                .unitPrice(BigDecimal.valueOf(6000.00))
                                .build()
                ))
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                salesOrderService.createSalesOrder(request, currentUser)
        );
        assertTrue(ex.getReason().contains("Insufficient stock"));
    }

    @Test
    @DisplayName("Fail when customer is missing")
    void testCreateSalesOrder_CustomerNotFound() {
        when(customerRepository.findByCustomerName("Unknown Corp")).thenReturn(Optional.empty());

        SalesOrderCreateRequest request = SalesOrderCreateRequest.builder()
                .customerName("Unknown Corp")
                .items(List.of(
                        SalesOrderItemRequest.builder()
                                .productId(17292)
                                .quantity(2)
                                .build()
                ))
                .build();

        assertThrows(ResponseStatusException.class, () ->
                salesOrderService.createSalesOrder(request, currentUser)
        );
    }
}
