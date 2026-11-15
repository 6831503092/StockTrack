package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.payload.CustomerPayload;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Customer;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomerCreateExecutor extends BaseApprovalExecutor<CustomerPayload> {

    private final CustomerRepository customerRepository;

    public CustomerCreateExecutor(CustomerRepository customerRepository, ObjectMapper objectMapper) {
        super(objectMapper);
        this.customerRepository = customerRepository;
    }

    @Override
    public RequestType supports() {
        return RequestType.CUSTOMER_CREATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        CustomerPayload payload = parsePayload(request, CustomerPayload.class);
        String customerName = payload.customerName() != null ? payload.customerName().trim() : "";

        if (customerName.isEmpty()) {
            throw new ValidationException("Customer name is required in payload for CUSTOMER_CREATE");
        }

        Customer customer = customerRepository.findByCustomerName(customerName).orElseGet(Customer::new);
        customer.setCustomerName(customerName);
        customer.setIsActive(true);

        Customer saved = customerRepository.save(customer);
        log.info("CUSTOMER_CREATE executed: created/activated customer #{} ({})", saved.getCustomerId(), saved.getCustomerName());
    }
}
