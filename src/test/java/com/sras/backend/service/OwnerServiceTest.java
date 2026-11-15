package com.sras.backend.service;

import com.sras.backend.dto.OwnerActivityDto;
import com.sras.backend.dto.OwnerDashboardDto;
import com.sras.backend.entity.AuditLog;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.User;
import com.sras.backend.enums.Role;
import com.sras.backend.repository.AuditLogRepository;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.SalesOrderItemRepository;
import com.sras.backend.repository.SalesOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private OwnerService ownerService;

    @Test
    @DisplayName("getDashboard should calculate inventory and return business metrics")
    void getDashboard_returnsAggregatedMetrics() {
        Product p1 = new Product();
        p1.setProductId(1);
        p1.setProductName("Desk");
        p1.setSellingPrice(BigDecimal.valueOf(1000));
        p1.setCurrentStock(10);

        Product p2 = new Product();
        p2.setProductId(2);
        p2.setProductName("Chair");
        p2.setSellingPrice(BigDecimal.valueOf(500));
        p2.setCurrentStock(20);

        when(productRepository.findAll()).thenReturn(List.of(p1, p2));
        when(salesOrderRepository.findAll()).thenReturn(Collections.emptyList());

        OwnerDashboardDto dto = ownerService.getDashboard();

        assertNotNull(dto);
        assertEquals("฿20,000", dto.getInventoryValue());
        assertNotNull(dto.getTotalRevenue());
        assertNotNull(dto.getTotalProfit());
        assertFalse(dto.getHotSellers().isEmpty());
        assertFalse(dto.getTopImports().isEmpty());
    }

    @Test
    @DisplayName("getActivities should return formatted flow logs with filtering")
    void getActivities_returnsFormattedLogs() {
        User staff = new User();
        staff.setUserId(503080);
        staff.setFullName("Staff Member");
        staff.setRole(Role.STAFF);

        AuditLog log1 = AuditLog.builder()
                .logId(1)
                .user(staff)
                .role(Role.STAFF)
                .actionType("RECEIVE_ACCEPT")
                .detail("Accepted Receive for PO #1001 into warehouse")
                .actionDatetime(LocalDateTime.now())
                .build();

        when(auditLogRepository.findAllByOrderByActionDatetimeDesc()).thenReturn(List.of(log1));

        List<OwnerActivityDto> all = ownerService.getActivities(null, null, null);
        assertNotNull(all);
        assertEquals(1, all.size());
        assertEquals("Receive Items", all.get(0).getActivityName());
        assertEquals("import", all.get(0).getType());
        assertEquals("PO-1001", all.get(0).getReference());
        assertEquals("Completed", all.get(0).getStatus());

        // Test filter by type
        List<OwnerActivityDto> imports = ownerService.getActivities("import", null, null);
        assertEquals(1, imports.size());

        List<OwnerActivityDto> sales = ownerService.getActivities("sale", null, null);
        assertEquals(0, sales.size());
    }

    @Test
    @DisplayName("getActivities should use targetType and targetId directly without regex")
    void getActivities_usesTargetTypeAndTargetIdDirectly() {
        User admin = new User();
        admin.setUserId(503092);
        admin.setFullName("Admin User");
        admin.setRole(Role.ADMIN);

        AuditLog logWithTarget = AuditLog.builder()
                .logId(99)
                .user(admin)
                .role(Role.ADMIN)
                .actionType("CUSTOMER_CREATE")
                .targetType("CUSTOMER")
                .targetId("42")
                .detail("Created customer without any regex pattern in detail text")
                .actionDatetime(LocalDateTime.now())
                .build();

        when(auditLogRepository.findAllByOrderByActionDatetimeDesc()).thenReturn(List.of(logWithTarget));

        List<OwnerActivityDto> list = ownerService.getActivities(null, null, null);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Customer Created", list.get(0).getActivityName());
        assertEquals("sale", list.get(0).getType());
        assertEquals("CUST-42", list.get(0).getReference());
        assertEquals("Completed", list.get(0).getStatus());
    }
}
