package com.sras.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.PurchaseOrderCreateRequest;
import com.sras.backend.dto.PurchaseOrderDto;
import com.sras.backend.dto.PurchaseOrderItemRequest;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.entity.PurchaseOrderItem;
import com.sras.backend.entity.Supplier;
import com.sras.backend.entity.User;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.PurchaseOrderStatus;
import com.sras.backend.enums.Role;
import com.sras.backend.repository.ApprovalRequestRepository;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.PurchaseOrderItemRepository;
import com.sras.backend.repository.PurchaseOrderRepository;
import com.sras.backend.repository.SupplierRepository;
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
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private User currentUser;
    private Supplier supplier;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setUserId(503092);
        currentUser.setFullName("Admin User");
        currentUser.setRole(Role.ADMIN);

        Zone z1 = new Zone();
        z1.setZoneCode("Z1");
        z1.setZoneName("Zone 1");

        supplier = Supplier.builder()
                .supplierId(1)
                .companyName("Thai Wood Supply Co.")
                .zoneCode("Z1")
                .category("Tables")
                .isActive(true)
                .build();

        product1 = new Product();
        product1.setProductId(10001);
        product1.setProductName("Wooden Desk");
        product1.setZone(z1);
        product1.setDefaultCost(BigDecimal.valueOf(1500.00));

        product2 = new Product();
        product2.setProductId(10002);
        product2.setProductName("Dining Table");
        product2.setZone(z1);
        product2.setDefaultCost(BigDecimal.valueOf(2500.00));
    }

    @Test
    @DisplayName("Successfully create Purchase Order with multiple items")
    void createPurchaseOrder_success() {
        when(supplierRepository.findByCompanyName("Thai Wood Supply Co.")).thenReturn(Optional.of(supplier));
        when(productRepository.findById(10001)).thenReturn(Optional.of(product1));
        when(productRepository.findById(10002)).thenReturn(Optional.of(product2));

        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> {
            ApprovalRequest ar = invocation.getArgument(0);
            ar.setRequestId(101);
            return ar;
        });

        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> {
            PurchaseOrder po = invocation.getArgument(0);
            po.setPoId(501);
            po.setOrderDatetime(LocalDateTime.now());
            return po;
        });

        when(purchaseOrderItemRepository.save(any(PurchaseOrderItem.class))).thenAnswer(invocation -> {
            PurchaseOrderItem poi = invocation.getArgument(0);
            poi.setPoItemId(999);
            return poi;
        });

        PurchaseOrderCreateRequest req = PurchaseOrderCreateRequest.builder()
                .supplierName("Thai Wood Supply Co.")
                .items(List.of(
                        PurchaseOrderItemRequest.builder()
                                .productId(10001)
                                .quantity(2)
                                .unitCost(BigDecimal.valueOf(1500.00))
                                .build(),
                        PurchaseOrderItemRequest.builder()
                                .productId(10002)
                                .quantity(3)
                                .unitCost(BigDecimal.valueOf(2500.00))
                                .build()
                ))
                .build();

        PurchaseOrderDto result = purchaseOrderService.createPurchaseOrder(req, currentUser);

        assertNotNull(result);
        assertEquals(501, result.getId());
        assertEquals("Thai Wood Supply Co.", result.getSupplier());
        assertEquals("Pending", result.getStatus());
        assertEquals(5, result.getTotalQty());
        // 2*1500 + 3*2500 = 3000 + 7500 = 10500
        assertEquals(BigDecimal.valueOf(10500.00), result.getTotalPrice());
        assertEquals(2, result.getItems().size());

        verify(approvalRequestRepository, times(2)).save(any(ApprovalRequest.class));
        verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
        verify(purchaseOrderItemRepository, times(2)).save(any(PurchaseOrderItem.class));
        verify(auditLogService).log(eq(currentUser), eq("PO_CREATE"), anyString());
    }

    @Test
    @DisplayName("Throw BAD_REQUEST if items list is empty")
    void createPurchaseOrder_emptyItems_throwsException() {
        when(supplierRepository.findByCompanyName("Thai Wood Supply Co.")).thenReturn(Optional.of(supplier));

        PurchaseOrderCreateRequest req = PurchaseOrderCreateRequest.builder()
                .supplierName("Thai Wood Supply Co.")
                .items(List.of())
                .build();

        assertThrows(ResponseStatusException.class, () ->
                purchaseOrderService.createPurchaseOrder(req, currentUser)
        );
    }

    @Test
    @DisplayName("Throw BAD_REQUEST if supplier is missing")
    void createPurchaseOrder_missingSupplier_throwsException() {
        PurchaseOrderCreateRequest req = PurchaseOrderCreateRequest.builder()
                .supplierName("Nonexistent Supplier")
                .items(List.of(
                        PurchaseOrderItemRequest.builder()
                                .productId(10001)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(supplierRepository.findByCompanyName("Nonexistent Supplier")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                purchaseOrderService.createPurchaseOrder(req, currentUser)
        );
    }
}
