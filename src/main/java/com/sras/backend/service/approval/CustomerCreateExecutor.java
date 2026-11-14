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
public class CustomerCreateExecutor implements ApprovalExecutor {

    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RequestType supports() {
        return RequestType.CUSTOMER_CREATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        if (request.getPayload() == null || request.getPayload().isBlank()) {
            throw new IllegalArgumentException("CUSTOMER_CREATE payload is empty for request #" + request.getRequestId());
        }

        try {
            JsonNode node = objectMapper.readTree(request.getPayload());
            String customerName = node.path("customerName").asText("").trim();

            if (customerName.isEmpty()) {
                throw new IllegalArgumentException("Customer name is required in payload");
            }

            Customer customer = customerRepository.findByCustomerName(customerName).orElseGet(Customer::new);
            customer.setCustomerName(customerName);
            customer.setIsActive(true);

            Customer saved = customerRepository.save(customer);
            log.info("CUSTOMER_CREATE executed: created/activated customer #{} ({})", saved.getCustomerId(), saved.getCustomerName());
        } catch (Exception e) {
            log.error("Failed to execute CUSTOMER_CREATE for request #{}", request.getRequestId(), e);
            throw new RuntimeException("Failed to execute CUSTOMER_CREATE: " + e.getMessage(), e);
        }
    }
}

