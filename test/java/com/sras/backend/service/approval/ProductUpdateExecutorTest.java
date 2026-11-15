package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Product;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductUpdateExecutorTest {

    @Mock
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ProductUpdateExecutor executor;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        executor = new ProductUpdateExecutor(productRepository, objectMapper);

        sampleProduct = new Product();
        sampleProduct.setProductId(5);
        sampleProduct.setProductName("Old Name");
        sampleProduct.setSellingPrice(new BigDecimal("100.00"));
        sampleProduct.setDefaultCost(new BigDecimal("60.00"));
        sampleProduct.setCurrentStock(10);
        sampleProduct.setMinStockAlert(5);
    }

    @Test
    @DisplayName("supports: returns PRODUCT_UPDATE")
    void testSupports() {
        assertEquals(RequestType.PRODUCT_UPDATE, executor.supports());
    }

    @Test
    @DisplayName("execute: successfully updates product fields from payload")
    void testExecute_Success() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(40)
                .requestType(RequestType.PRODUCT_UPDATE)
                .targetId(5)
                .payload("{\"productId\":5,\"productName\":\"New Name\",\"sellingPrice\":150.00,\"defaultCost\":80.00,\"currentStock\":25,\"minStockAlert\":10}")
                .build();

        when(productRepository.findById(5)).thenReturn(Optional.of(sampleProduct));

        executor.execute(request);

        assertEquals("New Name", sampleProduct.getProductName());
        assertEquals(new BigDecimal("150.00"), sampleProduct.getSellingPrice());
        assertEquals(new BigDecimal("80.00"), sampleProduct.getDefaultCost());
        assertEquals(25, sampleProduct.getCurrentStock());
        assertEquals(10, sampleProduct.getMinStockAlert());
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("execute: throws NotFoundException when product is not found")
    void testExecute_NotFound() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(41)
                .requestType(RequestType.PRODUCT_UPDATE)
                .targetId(99)
                .payload("{\"productId\":99,\"productName\":\"Ghost\"}")
                .build();

        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> executor.execute(request));
    }

    @Test
    @DisplayName("execute: throws ValidationException when payload is null/empty")
    void testExecute_MissingPayload() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(42)
                .requestType(RequestType.PRODUCT_UPDATE)
                .targetId(5)
                .payload("")
                .build();

        assertThrows(ValidationException.class, () -> executor.execute(request));
    }
}
