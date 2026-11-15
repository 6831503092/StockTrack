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
public class CustomerDeactivateExecutor extends BaseApprovalExecutor<CustomerPayload> {

    private final CustomerRepository customerRepository;

    public CustomerDeactivateExecutor(CustomerRepository customerRepository, ObjectMapper objectMapper) {
        super(objectMapper);
        this.customerRepository = customerRepository;
    }

    @Override
    public RequestType supports() {
        return RequestType.CUSTOMER_DEACTIVATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        CustomerPayload payload = parsePayloadOptional(request, CustomerPayload.class);
        int customerId = request.getTargetId() != null && request.getTargetId() > 0
                ? request.getTargetId()
                : (payload != null && payload.customerId() != null ? payload.customerId() : 0);

        if (customerId <= 0) {
            throw new ValidationException("Valid customerId is required for CUSTOMER_DEACTIVATE");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found with ID: " + customerId));

        customer.setIsActive(false);
        customerRepository.save(customer);
        log.info("CUSTOMER_DEACTIVATE executed: deactivated customer #{} ({})", customer.getCustomerId(), customer.getCustomerName());
    }
}
