package com.sras.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.dto.CustomerDto;
import com.sras.backend.dto.CustomerRequest;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Customer;
import com.sras.backend.entity.User;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.TargetType;
import com.sras.backend.repository.ApprovalRequestRepository;
import com.sras.backend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalRequestFactory approvalRequestFactory;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CustomerDto> getActiveCustomers() {
        return customerRepository.findByIsActiveTrue()
                .stream()
                .map(CustomerDto::fromEntity)
                .toList();
    }

    @Transactional
    public CustomerDto createCustomer(CustomerRequest req, User actor) {
        if (req == null || req.customerName() == null || req.customerName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name is required");
        }
        String name = req.customerName().trim();
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name cannot exceed 50 characters");
        }
        if (customerRepository.findByCustomerName(name).filter(c -> Boolean.TRUE.equals(c.getIsActive())).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active customer with this name already exists");
        }

        Customer customer = customerRepository.findByCustomerName(name).orElseGet(Customer::new);
        customer.setCustomerName(name);
        customer.setIsActive(true);

        Customer saved = customerRepository.save(customer);
        auditLogService.log(actor, com.sras.backend.enums.AuditAction.CUSTOMER_CREATE, "CUSTOMER", saved.getCustomerId(),
                "Created customer #" + saved.getCustomerId() + " (" + saved.getCustomerName() + ")");
        return CustomerDto.fromEntity(saved);
    }

    @Transactional
    public CustomerDto updateCustomer(Integer id, CustomerRequest req, User actor) {
        if (req == null || req.customerName() == null || req.customerName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name is required");
        }
        String name = req.customerName().trim();
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name cannot exceed 50 characters");
        }

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found with ID: " + id));

        if (customer.getCustomerName() != null && customer.getCustomerName().trim().equals(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No changes detected for this customer");
        }

        customerRepository.findByCustomerName(name).ifPresent(existing -> {
            if (!existing.getCustomerId().equals(id) && Boolean.TRUE.equals(existing.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another active customer already exists with name: " + name);
            }
        });

        customer.setCustomerName(name);
        Customer saved = customerRepository.save(customer);
        auditLogService.log(actor, com.sras.backend.enums.AuditAction.CUSTOMER_UPDATE, "CUSTOMER", saved.getCustomerId(),
                "Updated customer #" + saved.getCustomerId() + " (" + saved.getCustomerName() + ")");
        return CustomerDto.fromEntity(saved);
    }

    @Transactional
    public void deleteCustomer(Integer id, User actor) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found with ID: " + id));

        customer.setIsActive(false);
        customerRepository.save(customer);
        auditLogService.log(actor, com.sras.backend.enums.AuditAction.CUSTOMER_DELETE, "CUSTOMER", customer.getCustomerId(),
                "Deactivated customer #" + customer.getCustomerId() + " (" + customer.getCustomerName() + ")");
    }

    @Transactional
    public ApprovalRequestDto requestCustomerCreate(CustomerRequest req, User actor) {
        if (req == null || req.customerName() == null || req.customerName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name is required");
        }
        String name = req.customerName().trim();
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name cannot exceed 50 characters");
        }
        if (customerRepository.findByCustomerName(name).filter(c -> Boolean.TRUE.equals(c.getIsActive())).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active customer with this name already exists");
        }

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("operation", "CREATE");
        payloadMap.put("customerName", name);
        payloadMap.put("reason", "Request to add new customer: " + name);

        String payloadJson = serializeCustomerPayload(payloadMap, name);
        String detail = "Request to add new customer: " + name;

        ApprovalRequest saved = approvalRequestFactory.create(
                RequestType.CUSTOMER_CREATE, TargetType.CUSTOMER, null, payloadJson, detail, actor
        );

        auditLogService.log(actor, com.sras.backend.enums.AuditAction.REQUEST_SUBMIT, "CUSTOMER", saved.getRequestId(),
                "Submitted request to create customer: " + name);
        return ApprovalRequestDto.fromEntity(saved);
    }

    @Transactional
    public ApprovalRequestDto requestCustomerUpdate(Integer id, CustomerRequest req, User actor) {
        if (req == null || req.customerName() == null || req.customerName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name is required");
        }
        String name = req.customerName().trim();
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name cannot exceed 50 characters");
        }

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found with ID: " + id));

        if (customer.getCustomerName() != null && customer.getCustomerName().trim().equals(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No changes detected for this customer");
        }

        customerRepository.findByCustomerName(name).ifPresent(existing -> {
            if (!existing.getCustomerId().equals(id) && Boolean.TRUE.equals(existing.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another active customer already exists with name: " + name);
            }
        });

        if (approvalRequestRepository.existsByRequestTypeAndTargetIdAndStatus(
                RequestType.CUSTOMER_UPDATE, id, ApprovalStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending update request already exists for this customer");
        }

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("operation", "UPDATE");
        payloadMap.put("customerId", customer.getCustomerId());
        payloadMap.put("customerName", name);
        payloadMap.put("oldCustomerName", customer.getCustomerName());
        payloadMap.put("reason", "Request to update customer #" + customer.getCustomerId() +
                " (" + customer.getCustomerName() + " -> " + name + ")");

        String payloadJson = serializeCustomerPayload(payloadMap, name);
        String detail = "Request to edit customer #" + customer.getCustomerId() + ": " +
                customer.getCustomerName() + " -> " + name;

        ApprovalRequest saved = approvalRequestFactory.create(
                RequestType.CUSTOMER_UPDATE, TargetType.CUSTOMER, customer.getCustomerId(), payloadJson, detail, actor
        );

        auditLogService.log(actor, com.sras.backend.enums.AuditAction.REQUEST_SUBMIT, "CUSTOMER", saved.getRequestId(),
                "Submitted request to update customer #" + customer.getCustomerId() + " (" + name + ")");
        return ApprovalRequestDto.fromEntity(saved);
    }

    @Transactional
    public ApprovalRequestDto requestCustomerDeactivate(Integer id, User actor) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found with ID: " + id));

        if (approvalRequestRepository.existsByRequestTypeAndTargetIdAndStatus(
                RequestType.CUSTOMER_DEACTIVATE, id, ApprovalStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending deactivation request already exists for this customer");
        }

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("operation", "DELETE");
        payloadMap.put("customerId", customer.getCustomerId());
        payloadMap.put("customerName", customer.getCustomerName());
        payloadMap.put("reason", "Request to deactivate customer #" + customer.getCustomerId() + " (" + customer.getCustomerName() + ")");

        String payloadJson = serializeCustomerPayload(payloadMap, customer.getCustomerName());
        String detail = "Request to delete customer #" + customer.getCustomerId() + ": " + customer.getCustomerName();

        ApprovalRequest saved = approvalRequestFactory.create(
                RequestType.CUSTOMER_DEACTIVATE, TargetType.CUSTOMER, customer.getCustomerId(), payloadJson, detail, actor
        );

        auditLogService.log(actor, com.sras.backend.enums.AuditAction.REQUEST_SUBMIT, "CUSTOMER", saved.getRequestId(),
                "Submitted request to deactivate customer #" + customer.getCustomerId() + " (" + customer.getCustomerName() + ")");
        return ApprovalRequestDto.fromEntity(saved);
    }

    private String serializeCustomerPayload(Map<String, Object> payloadMap, String fallbackName) {
        try {
            return objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            return "{\"customerName\":\"" + fallbackName + "\"}";
        }
    }
}
