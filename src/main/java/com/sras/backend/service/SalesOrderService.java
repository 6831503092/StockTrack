package com.sras.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.dto.CustomerDto;
import com.sras.backend.dto.CustomerRequest;
import com.sras.backend.dto.SalesOrderCreateRequest;
import com.sras.backend.dto.SalesOrderDto;
import com.sras.backend.dto.SalesOrderItemRequest;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Customer;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.SalesOrder;
import com.sras.backend.entity.SalesOrderItem;
import com.sras.backend.entity.User;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.SalesOrderStatus;
import com.sras.backend.enums.TargetType;
import com.sras.backend.repository.ApprovalRequestRepository;
import com.sras.backend.repository.CustomerRepository;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.SalesOrderItemRepository;
import com.sras.backend.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private record ValidatedSOItem(Product product, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}

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
        auditLogService.log(actor, "CUSTOMER_CREATE", "Created customer #" + saved.getCustomerId() + " (" + saved.getCustomerName() + ")");
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
        auditLogService.log(actor, "CUSTOMER_UPDATE", "Updated customer #" + saved.getCustomerId() + " (" + saved.getCustomerName() + ")");
        return CustomerDto.fromEntity(saved);
    }

    @Transactional
    public void deleteCustomer(Integer id, User actor) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found with ID: " + id));

        customer.setIsActive(false);
        customerRepository.save(customer);
        auditLogService.log(actor, "CUSTOMER_DELETE", "Deactivated customer #" + customer.getCustomerId() + " (" + customer.getCustomerName() + ")");
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
        if (detail.length() > 255) {
            detail = detail.substring(0, 252) + "...";
        }

        ApprovalRequest saved = approvalRequestRepository.save(
                ApprovalRequestService.buildPendingRequest(
                        RequestType.CUSTOMER_CREATE, TargetType.CUSTOMER, null, payloadJson, detail, actor
                )
        );

        auditLogService.log(actor, "REQUEST_CREATE", "Submitted request to create customer: " + name);
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
        if (detail.length() > 255) {
            detail = detail.substring(0, 252) + "...";
        }

        ApprovalRequest saved = approvalRequestRepository.save(
                ApprovalRequestService.buildPendingRequest(
                        RequestType.CUSTOMER_UPDATE, TargetType.CUSTOMER, customer.getCustomerId(), payloadJson, detail, actor
                )
        );

        auditLogService.log(actor, "REQUEST_CREATE", "Submitted request to update customer #" + customer.getCustomerId() + " (" + name + ")");
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
        if (detail.length() > 255) {
            detail = detail.substring(0, 252) + "...";
        }

        ApprovalRequest saved = approvalRequestRepository.save(
                ApprovalRequestService.buildPendingRequest(
                        RequestType.CUSTOMER_DEACTIVATE, TargetType.CUSTOMER, customer.getCustomerId(), payloadJson, detail, actor
                )
        );

        auditLogService.log(actor, "REQUEST_CREATE", "Submitted request to deactivate customer #" + customer.getCustomerId() + " (" + customer.getCustomerName() + ")");
        return ApprovalRequestDto.fromEntity(saved);
    }

    private String serializeCustomerPayload(Map<String, Object> payloadMap, String fallbackName) {
        try {
            return objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            return "{\"customerName\":\"" + fallbackName + "\"}";
        }
    }

    @Transactional(readOnly = true)
    public List<SalesOrderDto> getAllSalesOrders(String search, String status, String dateStr) {
        return salesOrderRepository.findAllByOrderByOrderDatetimeDesc()
                .stream()
                .map(so -> SalesOrderDto.fromEntity(so, salesOrderItemRepository.findBySalesOrderSoId(so.getSoId())))
                .filter(dto -> matchesSearch(dto, search))
                .filter(dto -> ApprovalRequestService.matchesStatusFilter(dto.getStatus(), status))
                .filter(dto -> ApprovalRequestService.matchesDateFilter(dto.getOrderDatetime(), dateStr))
                .toList();
    }

    @Transactional(readOnly = true)
    public SalesOrderDto getSalesOrderById(Integer id) {
        SalesOrder so = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sales order not found with ID: " + id));
        return SalesOrderDto.fromEntity(so, salesOrderItemRepository.findBySalesOrderSoId(so.getSoId()));
    }

    @Transactional
    public SalesOrderDto createSalesOrder(SalesOrderCreateRequest req, User currentUser) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated");
        }

        // 1. Resolve Customer
        Customer customer = resolveCustomer(req.getCustomerId(), req.getCustomerName());

        // 2. Validate Items
        List<SalesOrderItemRequest> itemRequests = req.getItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sales order must contain at least one item");
        }

        List<ValidatedSOItem> validatedItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (SalesOrderItemRequest itemReq : itemRequests) {
            if (itemReq == null) continue;

            int qty = (itemReq.getQuantity() != null) ? itemReq.getQuantity() : 0;
            if (qty <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item quantity must be greater than 0");
            }

            Product product = resolveProduct(itemReq.getProductId(), itemReq.getProductName());

            // Check stock availability for sales orders
            int currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
            if (currentStock < qty) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insufficient stock for " + product.getProductName() +
                        " (Current stock: " + currentStock + ", Requested: " + qty + ")");
            }

            BigDecimal unitPrice = itemReq.getUnitPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                unitPrice = product.getSellingPrice() != null ? product.getSellingPrice() : BigDecimal.ZERO;
            }

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            totalAmount = totalAmount.add(subtotal);
            totalQuantity += qty;

            validatedItems.add(new ValidatedSOItem(product, qty, unitPrice, subtotal));
        }

        if (validatedItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sales order must contain at least one valid item");
        }

        // 3. Create Approval Request
        String formattedTotal = new DecimalFormat("#,##0.00").format(totalAmount);
        String detailText = "Sales Order for " + customer.getCustomerName() +
                " (" + totalQuantity + " items, Total: ฿" + formattedTotal + ")";

        List<Map<String, Object>> payloadItems = validatedItems.stream().map(vi -> {
            Map<String, Object> itMap = new HashMap<>();
            itMap.put("productId", vi.product().getProductId());
            itMap.put("productName", vi.product().getProductName());
            itMap.put("zoneCode", vi.product().getZone() != null ? vi.product().getZone().getZoneCode() : "");
            itMap.put("quantity", vi.quantity());
            itMap.put("unitPrice", vi.unitPrice());
            itMap.put("subtotal", vi.subtotal());
            return itMap;
        }).toList();

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("customerId", customer.getCustomerId());
        payloadMap.put("customerName", customer.getCustomerName());
        payloadMap.put("totalAmount", totalAmount);
        payloadMap.put("totalPrice", totalAmount);
        payloadMap.put("totalQuantity", totalQuantity);
        payloadMap.put("totalQty", totalQuantity);
        payloadMap.put("items", payloadItems);
        if (req.getNote() != null && !req.getNote().isBlank()) {
            payloadMap.put("note", req.getNote().trim());
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            payloadJson = "{\"customerName\":\"" + customer.getCustomerName() + "\"}";
        }

        ApprovalRequest approvalRequest = approvalRequestRepository.save(
                ApprovalRequestService.buildPendingRequest(
                        RequestType.SO_CREATE, TargetType.CUSTOMER, null, payloadJson, detailText, currentUser
                )
        );

        // 4. Create Sales Order
        SalesOrder so = salesOrderRepository.save(SalesOrder.builder()
                .customer(customer)
                .createdBy(currentUser)
                .request(approvalRequest)
                .orderDatetime(LocalDateTime.now())
                .totalAmount(totalAmount)
                .status(SalesOrderStatus.PENDING)
                .build());

        approvalRequest.setTargetId(so.getSoId());
        approvalRequestRepository.save(approvalRequest);

        // 5. Create Sales Order Items
        List<SalesOrderItem> savedItems = validatedItems.stream()
                .map(vi -> {
                    BigDecimal costSnapshot = vi.product().getDefaultCost() != null ? vi.product().getDefaultCost() : BigDecimal.ZERO;
                    return salesOrderItemRepository.save(SalesOrderItem.builder()
                            .salesOrder(so)
                            .product(vi.product())
                            .quantity(vi.quantity())
                            .unitCostSnapshot(costSnapshot)
                            .unitPrice(vi.unitPrice())
                            .subtotal(vi.subtotal())
                            .build());
                })
                .toList();

        // 6. Audit Log
        auditLogService.log(currentUser, "SO_CREATE", "Created Sales Order #" + so.getSoId() +
                " for " + customer.getCustomerName() + " (Total: ฿" + formattedTotal + ")");

        return SalesOrderDto.fromEntity(so, savedItems);
    }

    private Customer resolveCustomer(Integer customerId, String customerName) {
        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;
        if (customer == null && customerName != null && !customerName.isBlank()) {
            customer = customerRepository.findByCustomerName(customerName.trim()).orElse(null);
        }
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid customer is required");
        }
        return customer;
    }

    private Product resolveProduct(Integer productId, String productName) {
        Product product = productId != null ? productRepository.findById(productId).orElse(null) : null;
        if (product == null && productName != null && !productName.isBlank()) {
            product = productRepository.findByProductNameIgnoreCase(productName.trim()).orElse(null);
        }
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product not found: " + (productName != null ? productName : productId));
        }
        return product;
    }

    private boolean matchesSearch(SalesOrderDto dto, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String q = search.trim().toLowerCase();
        String idStr = dto.getId() != null ? String.valueOf(dto.getId()) : "";
        String custStr = dto.getCustomer() != null ? dto.getCustomer().toLowerCase() : "";
        String dateStr = dto.getDate() != null ? dto.getDate() : "";
        String userStr = dto.getCreatedBy() != null ? dto.getCreatedBy().toLowerCase() : "";

        return idStr.contains(q) || custStr.contains(q) || dateStr.contains(q) || userStr.contains(q);
    }
}
