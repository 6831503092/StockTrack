package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Customer;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.CustomerRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerExecutorsTest {

    @Mock
    private CustomerRepository customerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CustomerCreateExecutor createExecutor;
    private CustomerUpdateExecutor updateExecutor;
    private CustomerDeactivateExecutor deactivateExecutor;
    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        createExecutor = new CustomerCreateExecutor(customerRepository, objectMapper);
        updateExecutor = new CustomerUpdateExecutor(customerRepository, objectMapper);
        deactivateExecutor = new CustomerDeactivateExecutor(customerRepository, objectMapper);

        sampleCustomer = new Customer();
        sampleCustomer.setCustomerId(10);
        sampleCustomer.setCustomerName("Acme Corp");
        sampleCustomer.setIsActive(true);
    }

    @Test
    @DisplayName("CUSTOMER_CREATE: creates and activates customer")
    void testCreate_Success() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(80)
                .requestType(RequestType.CUSTOMER_CREATE)
                .payload("{\"customerName\":\"Beta Ltd\"}")
                .build();

        when(customerRepository.findByCustomerName("Beta Ltd")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        createExecutor.execute(request);

        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("CUSTOMER_CREATE: throws ValidationException when customer name is blank")
    void testCreate_BlankName() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(81)
                .requestType(RequestType.CUSTOMER_CREATE)
                .payload("{\"customerName\":\"\"}")
                .build();

        assertThrows(ValidationException.class, () -> createExecutor.execute(request));
    }

    @Test
    @DisplayName("CUSTOMER_UPDATE: updates existing customer name")
    void testUpdate_Success() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(82)
                .requestType(RequestType.CUSTOMER_UPDATE)
                .targetId(10)
                .payload("{\"customerId\":10,\"customerName\":\"Acme Worldwide\"}")
                .build();

        when(customerRepository.findById(10)).thenReturn(Optional.of(sampleCustomer));

        updateExecutor.execute(request);

        assertEquals("Acme Worldwide", sampleCustomer.getCustomerName());
        verify(customerRepository).save(sampleCustomer);
    }

    @Test
    @DisplayName("CUSTOMER_UPDATE: throws NotFoundException when customer not found")
    void testUpdate_NotFound() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(83)
                .requestType(RequestType.CUSTOMER_UPDATE)
                .targetId(999)
                .payload("{\"customerId\":999,\"customerName\":\"Ghost\"}")
                .build();

        when(customerRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> updateExecutor.execute(request));
    }

    @Test
    @DisplayName("CUSTOMER_DEACTIVATE: sets isActive to false")
    void testDeactivate_Success() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(84)
                .requestType(RequestType.CUSTOMER_DEACTIVATE)
                .targetId(10)
                .build();

        when(customerRepository.findById(10)).thenReturn(Optional.of(sampleCustomer));

        deactivateExecutor.execute(request);

        assertFalse(sampleCustomer.getIsActive());
        verify(customerRepository).save(sampleCustomer);
    }
}
