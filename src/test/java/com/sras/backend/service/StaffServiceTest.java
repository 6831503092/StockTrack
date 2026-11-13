package com.sras.backend.service;

import com.sras.backend.dto.StaffCheckRequest;
import com.sras.backend.dto.StaffOrderDto;
import com.sras.backend.dto.StaffTransferDto;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.entity.PurchaseOrderItem;
import com.sras.backend.entity.ReceivingCheck;
import com.sras.backend.entity.SalesOrder;
import com.sras.backend.entity.SalesOrderItem;
import com.sras.backend.entity.StockMovement;
import com.sras.backend.entity.StockOutCheck;
import com.sras.backend.entity.Supplier;
import com.sras.backend.entity.User;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.MovementStatus;
import com.sras.backend.enums.PurchaseOrderStatus;
import com.sras.backend.enums.ReceivingCheckResult;
import com.sras.backend.enums.Role;
import com.sras.backend.enums.SalesOrderStatus;
import com.sras.backend.enums.StockOutCheckResult;
import com.sras.backend.repository.AuditLogRepository;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.PurchaseOrderItemRepository;
import com.sras.backend.repository.PurchaseOrderRepository;
import com.sras.backend.repository.ReceivingCheckItemRepository;
import com.sras.backend.repository.ReceivingCheckRepository;
import com.sras.backend.repository.SalesOrderItemRepository;
import com.sras.backend.repository.SalesOrderRepository;
import com.sras.backend.repository.StockMovementRepository;
import com.sras.backend.repository.StockOutCheckItemRepository;
import com.sras.backend.repository.StockOutCheckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseOrderItemRepository purchaseOrderItemRepository;
    @Mock private ReceivingCheckRepository receivingCheckRepository;
    @Mock private ReceivingCheckItemRepository receivingCheckItemRepository;

    @Mock private SalesOrderRepository salesOrderRepository;
    @Mock private SalesOrderItemRepository salesOrderItemRepository;
    @Mock private StockOutCheckRepository stockOutCheckRepository;
    @Mock private StockOutCheckItemRepository stockOutCheckItemRepository;

    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private ProductRepository productRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private StaffService staffService;

    private User staffUser;
    private Product testProduct;
    private Zone zone1;
    private Zone zone2;

    @BeforeEach
    void setUp() {
        staffUser = new User();
        staffUser.setUserId(503080);
        staffUser.setFullName("Staff User");
        staffUser.setRole(Role.STAFF);

        zone1 = new Zone();
        zone1.setZoneId(1);
        zone1.setZoneCode("Z1");
        zone1.setZoneName("Zone 1");

        zone2 = new Zone();
        zone2.setZoneId(2);
        zone2.setZoneCode("Z2");
        zone2.setZoneName("Zone 2");

        testProduct = new Product();
        testProduct.setProductId(10001);
        testProduct.setProductName("Office Desk");
        testProduct.setCurrentStock(50);
        testProduct.setZone(zone1);
    }

    @Test
    @DisplayName("Staff can retrieve pending POs approved by manager")
    void testGetPendingReceives() {
        PurchaseOrder po = PurchaseOrder.builder()
                .poId(1)
                .orderDatetime(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(1000))
                .status(PurchaseOrderStatus.APPROVED)
                .supplier(Supplier.builder().companyName("Supplier A").build())
                .createdBy(staffUser)
                .build();

        when(purchaseOrderRepository.findByStatusOrderByOrderDatetimeDesc(PurchaseOrderStatus.APPROVED))
                .thenReturn(List.of(po));
        when(purchaseOrderItemRepository.findByPurchaseOrderPoId(1))
                .thenReturn(List.of(PurchaseOrderItem.builder()
                        .poItemId(10)
                        .product(testProduct)
                        .quantity(20)
                        .unitCost(BigDecimal.valueOf(50))
                        .build()));

        List<StaffOrderDto> list = staffService.getPendingReceives();
        assertEquals(1, list.size());
        assertEquals("PO-1", list.get(0).getCode());
        assertEquals(1, list.get(0).getItems().size());
        assertEquals("Office Desk", list.get(0).getItems().get(0).getProductName());
    }

    @Test
    @DisplayName("Staff acceptReceive increases product stock and marks PO as RECEIVED")
    void testAcceptReceive() {
        PurchaseOrder po = PurchaseOrder.builder()
                .poId(1)
                .status(PurchaseOrderStatus.APPROVED)
                .totalAmount(BigDecimal.valueOf(1000))
                .build();

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .poItemId(10)
                .product(testProduct)
                .quantity(20)
                .build();

        when(purchaseOrderRepository.findById(1)).thenReturn(Optional.of(po));
        when(purchaseOrderItemRepository.findByPurchaseOrderPoId(1)).thenReturn(List.of(item));

        staffService.acceptReceive(1, staffUser, StaffCheckRequest.builder().build());

        assertEquals(PurchaseOrderStatus.RECEIVED, po.getStatus());
        assertEquals(70, testProduct.getCurrentStock()); // 50 + 20
        verify(receivingCheckRepository).save(any(ReceivingCheck.class));
        verify(productRepository).save(testProduct);
        verify(purchaseOrderRepository).save(po);
    }

    @Test
    @DisplayName("Staff confirmStockOut decreases product stock and marks SO as STOCK_OUT")
    void testConfirmStockOut() {
        SalesOrder so = SalesOrder.builder()
                .soId(2)
                .status(SalesOrderStatus.APPROVED)
                .totalAmount(BigDecimal.valueOf(1500))
                .build();

        SalesOrderItem item = SalesOrderItem.builder()
                .soItemId(20)
                .product(testProduct)
                .quantity(15)
                .build();

        when(salesOrderRepository.findById(2)).thenReturn(Optional.of(so));
        when(salesOrderItemRepository.findBySalesOrderSoId(2)).thenReturn(List.of(item));

        staffService.confirmStockOut(2, staffUser, StaffCheckRequest.builder().build());

        assertEquals(SalesOrderStatus.STOCK_OUT, so.getStatus());
        assertEquals(35, testProduct.getCurrentStock()); // 50 - 15
        verify(stockOutCheckRepository).save(any(StockOutCheck.class));
        verify(productRepository).save(testProduct);
        verify(salesOrderRepository).save(so);
    }

    @Test
    @DisplayName("Staff verifyTransfer relocates product to destination zone")
    void testVerifyTransfer() {
        StockMovement movement = StockMovement.builder()
                .movementId(5)
                .product(testProduct)
                .fromZone(zone1)
                .toZone(zone2)
                .quantity(10)
                .status(MovementStatus.PENDING_VERIFICATION)
                .build();

        when(stockMovementRepository.findById(5)).thenReturn(Optional.of(movement));

        staffService.verifyTransfer(5, staffUser, StaffCheckRequest.builder().build());

        assertEquals(MovementStatus.COMPLETED, movement.getStatus());
        assertEquals(zone2, testProduct.getZone());
        verify(stockMovementRepository).save(movement);
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("Staff acceptReceive throws exception when received quantity does not match expected quantity")
    void testAcceptReceiveFailsWhenQuantityMismatch() {
        PurchaseOrder po = PurchaseOrder.builder()
                .poId(1)
                .status(PurchaseOrderStatus.APPROVED)
                .build();

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .poItemId(10)
                .product(testProduct)
                .quantity(20)
                .build();

        when(purchaseOrderRepository.findById(1)).thenReturn(Optional.of(po));
        when(purchaseOrderItemRepository.findByPurchaseOrderPoId(1)).thenReturn(List.of(item));

        StaffCheckRequest req = StaffCheckRequest.builder()
                .items(List.of(StaffCheckRequest.ItemCheck.builder()
                        .itemId(10)
                        .countedQty(15) // Expected 20, but only 15 counted
                        .isCorrectProduct(true)
                        .build()))
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> staffService.acceptReceive(1, staffUser, req)
        );
    }

    @Test
    @DisplayName("Staff rejectReceive throws exception when reason is missing")
    void testRejectReceiveFailsWhenReasonMissing() {
        PurchaseOrder po = PurchaseOrder.builder()
                .poId(1)
                .status(PurchaseOrderStatus.APPROVED)
                .build();

        when(purchaseOrderRepository.findById(1)).thenReturn(Optional.of(po));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> staffService.rejectReceive(1, staffUser, new com.sras.backend.dto.StaffRejectRequest(""))
        );
    }
}
