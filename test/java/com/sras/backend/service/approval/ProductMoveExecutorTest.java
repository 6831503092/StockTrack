package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.StockMovement;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.MovementStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.StockMovementRepository;
import com.sras.backend.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductMoveExecutorTest {

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ProductMoveExecutor executor;
    private Zone sourceZone;
    private Zone targetZone;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        executor = new ProductMoveExecutor(stockMovementRepository, zoneRepository, productRepository, objectMapper);

        sourceZone = new Zone();
        sourceZone.setZoneId(1);
        sourceZone.setZoneCode("Z01");
        sourceZone.setZoneName("Source Zone");

        targetZone = new Zone();
        targetZone.setZoneId(2);
        targetZone.setZoneCode("Z02");
        targetZone.setZoneName("Target Zone");

        sampleProduct = new Product();
        sampleProduct.setProductId(10);
        sampleProduct.setProductName("Coffee Beans");
        sampleProduct.setCurrentStock(50);
        sampleProduct.setZone(sourceZone);
        sampleProduct.setIsActive(true);
    }

    @Test
    @DisplayName("supports: returns PRODUCT_MOVE")
    void testSupports() {
        assertEquals(RequestType.PRODUCT_MOVE, executor.supports());
    }

    @Test
    @DisplayName("execute: creates StockMovement awaiting verification on valid move request")
    void testExecute_Success() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(30)
                .requestType(RequestType.PRODUCT_MOVE)
                .payload("{\"sourceZoneId\":1,\"targetZoneId\":2,\"sourceZoneCode\":\"Z01\",\"targetZoneCode\":\"Z02\"}")
                .build();

        when(stockMovementRepository.findByRequestRequestId(30)).thenReturn(Optional.empty());
        when(zoneRepository.findById(1)).thenReturn(Optional.of(sourceZone));
        when(zoneRepository.findById(2)).thenReturn(Optional.of(targetZone));
        when(productRepository.findByZoneZoneIdAndIsActiveTrue(1)).thenReturn(List.of(sampleProduct));

        executor.execute(request);

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());

        StockMovement saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(MovementStatus.PENDING_VERIFICATION, saved.getStatus());
        assertEquals(sourceZone, saved.getFromZone());
        assertEquals(targetZone, saved.getToZone());
        assertEquals(sampleProduct, saved.getProduct());
        assertEquals(50, saved.getQuantity());
    }

    @Test
    @DisplayName("execute: throws NotFoundException when source zone is not found")
    void testExecute_SourceZoneNotFound() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(31)
                .requestType(RequestType.PRODUCT_MOVE)
                .payload("{\"sourceZoneId\":99,\"targetZoneId\":2}")
                .build();

        when(stockMovementRepository.findByRequestRequestId(31)).thenReturn(Optional.empty());
        when(zoneRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> executor.execute(request));
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("execute: throws NotFoundException when target zone is not found")
    void testExecute_TargetZoneNotFound() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(32)
                .requestType(RequestType.PRODUCT_MOVE)
                .payload("{\"sourceZoneId\":1,\"targetZoneId\":99}")
                .build();

        when(stockMovementRepository.findByRequestRequestId(32)).thenReturn(Optional.empty());
        when(zoneRepository.findById(1)).thenReturn(Optional.of(sourceZone));
        when(zoneRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> executor.execute(request));
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("execute: throws NotFoundException when no active product found in source zone (no DB-wide guessing)")
    void testExecute_NoProductInZone_ThrowsNotFound() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(33)
                .requestType(RequestType.PRODUCT_MOVE)
                .payload("{\"sourceZoneId\":1,\"targetZoneId\":2}")
                .build();

        when(stockMovementRepository.findByRequestRequestId(33)).thenReturn(Optional.empty());
        when(zoneRepository.findById(1)).thenReturn(Optional.of(sourceZone));
        when(zoneRepository.findById(2)).thenReturn(Optional.of(targetZone));
        when(productRepository.findByZoneZoneIdAndIsActiveTrue(1)).thenReturn(Collections.emptyList());

        assertThrows(NotFoundException.class, () -> executor.execute(request));
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("execute: skips execution if StockMovement already exists for request (idempotent)")
    void testExecute_AlreadyExists_Idempotent() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(34)
                .requestType(RequestType.PRODUCT_MOVE)
                .build();

        when(stockMovementRepository.findByRequestRequestId(34)).thenReturn(Optional.of(new StockMovement()));

        executor.execute(request);

        verify(stockMovementRepository, never()).save(any());
    }
}
