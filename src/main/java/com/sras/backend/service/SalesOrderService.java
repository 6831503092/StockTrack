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
    private final ApprovalRequestFactory approvalRequestFactory;
    private final CustomerService customerService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private record ValidatedSOItem(Product product, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}

    // Delegate customer methods to CustomerService for backward compatibility
    @Transactional(readOnly = true)
    public List<CustomerDto> getActiveCustomers() {
        return customerService.getActiveCustomers();
    }

    @Transactional
    public CustomerDto createCustomer(CustomerRequest req, User actor) {
        return customerService.createCustomer(req, actor);
    }

    @Transactional
    public CustomerDto updateCustomer(Integer id, CustomerRequest req, User actor) {
        return customerService.updateCustomer(id, req, actor);
    }

    @Transactional
    public void deleteCustomer(Integer id, User actor) {
        customerService.deleteCustomer(id, actor);
    }

    @Transactional
    public ApprovalRequestDto requestCustomerCreate(CustomerRequest req, User actor) {
        return customerService.requestCustomerCreate(req, actor);
    }

    @Transactional
    public ApprovalRequestDto requestCustomerUpdate(Integer id, CustomerRequest req, User actor) {
        return customerService.requestCustomerUpdate(id, req, actor);
    }

    @Transactional
    public ApprovalRequestDto requestCustomerDeactivate(Integer id, User actor) {
        return customerService.requestCustomerDeactivate(id, actor);
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

        ApprovalRequest approvalRequest = approvalRequestFactory.create(
                RequestType.SO_CREATE, TargetType.CUSTOMER, null, payloadJson, detailText, currentUser
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
        auditLogService.log(currentUser, com.sras.backend.enums.AuditAction.SO_CREATE, "SO", so.getSoId(),
                "Created Sales Order #" + so.getSoId() + " for " + customer.getCustomerName() + " (Total: ฿" + formattedTotal + ")");

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
