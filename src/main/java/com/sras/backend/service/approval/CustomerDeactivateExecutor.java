package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Customer;
import com.sras.backend.enums.RequestType;
import com.sras.backend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerDeactivateExecutor implements ApprovalExecutor {

    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RequestType supports() {
        return RequestType.CUSTOMER_DEACTIVATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        try {
            int parsedCustomerId = request.getTargetId() != null ? request.getTargetId() : 0;
            if (parsedCustomerId <= 0 && request.getPayload() != null && !request.getPayload().isBlank()) {
                JsonNode node = objectMapper.readTree(request.getPayload());
                parsedCustomerId = node.path("customerId").asInt(0);
            }

            final int customerId = parsedCustomerId;
            if (customerId <= 0) {
                throw new IllegalArgumentException("Invalid customerId for CUSTOMER_DEACTIVATE: " + customerId);
            }

            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));

            customer.setIsActive(false);
            customerRepository.save(customer);
            log.info("CUSTOMER_DEACTIVATE executed: deactivated customer #{} ({})", customer.getCustomerId(), customer.getCustomerName());
        } catch (Exception e) {
            log.error("Failed to execute CUSTOMER_DEACTIVATE for request #{}", request.getRequestId(), e);
            throw new RuntimeException("Failed to execute CUSTOMER_DEACTIVATE: " + e.getMessage(), e);
        }
    }
}

