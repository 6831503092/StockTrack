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
public class CustomerUpdateExecutor implements ApprovalExecutor {

    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RequestType supports() {
        return RequestType.CUSTOMER_UPDATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        if (request.getPayload() == null || request.getPayload().isBlank()) {
            throw new IllegalArgumentException("CUSTOMER_UPDATE payload is empty for request #" + request.getRequestId());
        }

        try {
            JsonNode node = objectMapper.readTree(request.getPayload());
            int parsedCustomerId = request.getTargetId() != null ? request.getTargetId() : 0;
            if (parsedCustomerId <= 0) {
                parsedCustomerId = node.path("customerId").asInt(0);
            }

            final int customerId = parsedCustomerId;
            if (customerId <= 0) {
                throw new IllegalArgumentException("Invalid customerId for CUSTOMER_UPDATE: " + supplierIdStr(customerId));
            }

            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));

            String customerName = node.path("customerName").asText("").trim();
            if (!customerName.isEmpty()) {
                customer.setCustomerName(customerName);
            }

            customerRepository.save(customer);
            log.info("CUSTOMER_UPDATE executed: updated customer #{} ({})", customer.getCustomerId(), customer.getCustomerName());
        } catch (Exception e) {
            log.error("Failed to execute CUSTOMER_UPDATE for request #{}", request.getRequestId(), e);
            throw new RuntimeException("Failed to execute CUSTOMER_UPDATE: " + e.getMessage(), e);
        }
    }

    private String supplierIdStr(int id) {
        return String.valueOf(id);
    }
}

