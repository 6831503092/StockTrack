package com.sras.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.dto.ProductDto;
import com.sras.backend.dto.ProductMoveRequest;
import com.sras.backend.dto.ZoneDto;
import com.sras.backend.dto.ZoneProductSummaryDto;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.User;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.Role;
import com.sras.backend.enums.TargetType;
import com.sras.backend.repository.ApprovalRequestRepository;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.ZoneRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ZoneService zoneService;

    private Zone zone1;
    private Zone zone2;
    private Product product1;
    private Product product2;
    private User adminUser;

    @BeforeEach
    void setUp() {
        zone1 = new Zone();
        zone1.setZoneId(1);
        zone1.setZoneCode("Z1");
        zone1.setZoneName("Zone 1");

        zone2 = new Zone();
        zone2.setZoneId(2);
        zone2.setZoneCode("Z2");
        zone2.setZoneName("Zone 2");

        product1 = new Product();
        product1.setProductId(1);
        product1.setProductName("Office Desk");
        product1.setDefaultCost(BigDecimal.valueOf(1000));
        product1.setSellingPrice(BigDecimal.valueOf(1500));
        product1.setCurrentStock(50);
        product1.setMinStockAlert(20);
        product1.setZone(zone1);
        product1.setIsActive(true);

        product2 = new Product();
        product2.setProductId(2);
        product2.setProductName("Gaming Chair");
        product2.setDefaultCost(BigDecimal.valueOf(2000));
        product2.setSellingPrice(BigDecimal.valueOf(3000));
        product2.setCurrentStock(15); // Low stock!
        product2.setMinStockAlert(20);
        product2.setZone(zone2);
        product2.setIsActive(true);

        adminUser = new User();
        adminUser.setUserId(503092);
        adminUser.setFullName("admin");
        adminUser.setRole(Role.ADMIN);
    }

    @Test
    @DisplayName("getZoneProductSummary: returns correct aggregated statistics")
    void testGetZoneProductSummary() {
        when(zoneRepository.count()).thenReturn(4L);
        when(productRepository.findByIsActiveTrue()).thenReturn(List.of(product1, product2));

        ZoneProductSummaryDto summary = zoneService.getZoneProductSummary();

        assertNotNull(summary);
        assertEquals(4, summary.getActiveZonesCount());
        assertEquals(2, summary.getTotalProducts());
        assertEquals(65, summary.getStoredUnits());
        assertEquals(1, summary.getLowStockCount()); // product2 stock 15 <= 20
    }

    @Test
    @DisplayName("getZoneProducts: filters products correctly by zone and search")
    void testGetZoneProducts() {
        when(productRepository.findByIsActiveTrue()).thenReturn(List.of(product1, product2));

        List<ProductDto> filtered = zoneService.getZoneProducts("Z1", "Desk");

        assertEquals(1, filtered.size());
        assertEquals("Office Desk", filtered.get(0).getName());
        assertEquals("Z1", filtered.get(0).getLocation());
        assertEquals("In Stock", filtered.get(0).getStatus());
    }

    @Test
    @DisplayName("submitProductMoveRequest: creates pending approval request")
    void testSubmitProductMoveRequest_Success() {
        when(zoneRepository.findByZoneCode("Z2")).thenReturn(Optional.of(zone2));
        when(productRepository.findAllById(List.of(1))).thenReturn(List.of(product1));

        ApprovalRequest savedMock = ApprovalRequest.builder()
                .requestId(10)
                .requestType(RequestType.PRODUCT_MOVE)
                .targetType(TargetType.PRODUCT)
                .targetId(1)
                .detail("Move 1 product(s) to Z2: Office Desk (#1)")
                .status(ApprovalStatus.PENDING)
                .requestedBy(adminUser)
                .requestedDatetime(LocalDateTime.now())
                .build();

        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenReturn(savedMock);

        ProductMoveRequest req = new ProductMoveRequest("Z2", null, List.of(1), "Reorganize");
        ApprovalRequestDto result = zoneService.submitProductMoveRequest(req, adminUser);

        assertNotNull(result);
        assertEquals(10, result.getRequestId());
        assertEquals("PRODUCT_MOVE", result.getRequestType());
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    @DisplayName("submitProductMoveRequest: successfully creates zone swap request when target has products")
    void testSubmitProductMoveRequest_WholeZoneSwap() {
        when(zoneRepository.findByZoneCode("Z1")).thenReturn(Optional.of(zone1));
        when(zoneRepository.findByZoneCode("Z2")).thenReturn(Optional.of(zone2));
        when(productRepository.findByZoneZoneIdAndIsActiveTrue(1)).thenReturn(List.of(product1));
        when(productRepository.findByZoneZoneIdAndIsActiveTrue(2)).thenReturn(List.of(product2));

        ApprovalRequest savedMock = ApprovalRequest.builder()
                .requestId(20)
                .requestType(RequestType.PRODUCT_MOVE)
                .targetType(TargetType.ZONE)
                .targetId(1)
                .detail("Swap Zone Z1 (1 items) <-> Zone Z2 (1 items)")
                .status(ApprovalStatus.PENDING)
                .requestedBy(adminUser)
                .requestedDatetime(LocalDateTime.now())
                .build();

        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenReturn(savedMock);

        ProductMoveRequest req = new ProductMoveRequest("Z1", "Z2", null, null, "Swap Z1 and Z2");
        ApprovalRequestDto result = zoneService.submitProductMoveRequest(req, adminUser);

        assertNotNull(result);
        assertEquals(20, result.getRequestId());
        assertEquals("PRODUCT_MOVE", result.getRequestType());
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    @DisplayName("submitProductMoveRequest: throws error when source and target zone are identical")
    void testSubmitProductMoveRequest_SameZoneThrows() {
        when(zoneRepository.findByZoneCode("Z1")).thenReturn(Optional.of(zone1));
        when(productRepository.findByZoneZoneIdAndIsActiveTrue(1)).thenReturn(List.of(product1));

        ProductMoveRequest req = new ProductMoveRequest("Z1", "Z1", null, null, "Same zone");

        assertThrows(ResponseStatusException.class, () -> zoneService.submitProductMoveRequest(req, adminUser));
    }

    @Test
    @DisplayName("submitProductMoveRequest: throws error when products list is empty")
    void testSubmitProductMoveRequest_EmptyList() {
        ProductMoveRequest req = new ProductMoveRequest("Z2", null, List.of(), "Reorganize");

        assertThrows(ResponseStatusException.class, () -> zoneService.submitProductMoveRequest(req, adminUser));
    }
}
