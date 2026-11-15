package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.payload.CustomerPayload;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Customer;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomerUpdateExecutor extends BaseApprovalExecutor<CustomerPayload> {

    private final CustomerRepository customerRepository;

    public CustomerUpdateExecutor(CustomerRepository customerRepository, ObjectMapper objectMapper) {
        super(objectMapper);
        this.customerRepository = customerRepository;
    }

    @Override
    public RequestType supports() {
        return RequestType.CUSTOMER_UPDATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        CustomerPayload payload = parsePayload(request, CustomerPayload.class);
        int customerId = request.getTargetId() != null && request.getTargetId() > 0
                ? request.getTargetId()
                : (payload.customerId() != null ? payload.customerId() : 0);

        if (customerId <= 0) {
            throw new ValidationException("Valid customerId is required for CUSTOMER_UPDATE");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found with ID: " + customerId));

        if (payload.customerName() != null && !payload.customerName().isBlank()) {
            customer.setCustomerName(payload.customerName().trim());
        }

        customerRepository.save(customer);
        log.info("CUSTOMER_UPDATE executed: updated customer #{} ({})", customer.getCustomerId(), customer.getCustomerName());
    }
}
