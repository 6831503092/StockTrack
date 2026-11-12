package com.sras.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.CustomerDto;
import com.sras.backend.dto.SalesOrderCreateRequest;
import com.sras.backend.dto.SalesOrderDto;
import com.sras.backend.dto.SalesOrderItemDto;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public List<CustomerDto> getActiveCustomers() {
        return customerRepository.findByIsActiveTrue()
                .stream()
                .map(CustomerDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SalesOrderDto> getAllSalesOrders(String search, String status, String dateStr) {
        List<SalesOrder> sos = salesOrderRepository.findAllByOrderByOrderDatetimeDesc();

        return sos.stream()
                .map(so -> {
                    List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderSoId(so.getSoId());
                    return SalesOrderDto.fromEntity(so, items);
                })
                .filter(dto -> matchesSearch(dto, search))
                .filter(dto -> matchesStatus(dto, status))
                .filter(dto -> matchesDate(dto, dateStr))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalesOrderDto getSalesOrderById(Integer id) {
        SalesOrder so = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sales order not found with ID: " + id));
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderSoId(so.getSoId());
        return SalesOrderDto.fromEntity(so, items);
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
        Customer customer = null;
        if (req.getCustomerId() != null) {
            customer = customerRepository.findById(req.getCustomerId()).orElse(null);
        }
        if (customer == null && req.getCustomerName() != null && !req.getCustomerName().isBlank()) {
            customer = customerRepository.findByCustomerName(req.getCustomerName().trim()).orElse(null);
        }
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid customer is required");
        }

        // 2. Validate Items
        List<SalesOrderItemRequest> itemRequests = req.getItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sales order must contain at least one item");
        }

        class ValidatedSOItem {
            Product product;
            int quantity;
            BigDecimal unitPrice;
            BigDecimal subtotal;
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

            Product product = null;
            if (itemReq.getProductId() != null) {
                product = productRepository.findById(itemReq.getProductId()).orElse(null);
            }
            if (product == null && itemReq.getProductName() != null && !itemReq.getProductName().isBlank()) {
                product = productRepository.findByProductNameIgnoreCase(itemReq.getProductName().trim())
                        .orElse(null);
            }
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product not found: " + (itemReq.getProductName() != null ? itemReq.getProductName() : itemReq.getProductId()));
            }

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

            ValidatedSOItem vi = new ValidatedSOItem();
            vi.product = product;
            vi.quantity = qty;
            vi.unitPrice = unitPrice;
            vi.subtotal = subtotal;
            validatedItems.add(vi);
        }

        if (validatedItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sales order must contain at least one valid item");
        }

        // 3. Create Approval Request
        DecimalFormat currencyFmt = new DecimalFormat("#,##0.00");
        String formattedTotal = currencyFmt.format(totalAmount);
        String detailText = "Sales Order for " + customer.getCustomerName() +
                " (" + totalQuantity + " items, Total: ฿" + formattedTotal + ")";

        List<Map<String, Object>> payloadItems = new ArrayList<>();
        for (ValidatedSOItem vi : validatedItems) {
            Map<String, Object> itMap = new HashMap<>();
            itMap.put("productId", vi.product.getProductId());
            itMap.put("productName", vi.product.getProductName());
            itMap.put("zoneCode", vi.product.getZone() != null ? vi.product.getZone().getZoneCode() : "");
            itMap.put("quantity", vi.quantity);
            itMap.put("unitPrice", vi.unitPrice);
            itMap.put("subtotal", vi.subtotal);
            payloadItems.add(itMap);
        }

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

        String payloadJson = "{}";
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            payloadJson = "{\"customerName\":\"" + customer.getCustomerName() + "\"}";
        }

        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType(RequestType.SO_CREATE)
                .targetType(TargetType.CUSTOMER)
                .payload(payloadJson)
                .detail(detailText)
                .status(ApprovalStatus.PENDING)
                .requestedBy(currentUser)
                .requestedDatetime(LocalDateTime.now())
                .build();
        approvalRequest = approvalRequestRepository.save(approvalRequest);

        // 4. Create Sales Order
        SalesOrder so = SalesOrder.builder()
                .customer(customer)
                .createdBy(currentUser)
                .request(approvalRequest)
                .orderDatetime(LocalDateTime.now())
                .totalAmount(totalAmount)
                .status(SalesOrderStatus.PENDING)
                .build();
        so = salesOrderRepository.save(so);

        approvalRequest.setTargetId(so.getSoId());
        approvalRequestRepository.save(approvalRequest);

        // 5. Create Sales Order Items
        List<SalesOrderItem> savedItems = new ArrayList<>();
        for (ValidatedSOItem vi : validatedItems) {
            BigDecimal costSnapshot = vi.product.getDefaultCost() != null ? vi.product.getDefaultCost() : BigDecimal.ZERO;
            SalesOrderItem soi = SalesOrderItem.builder()
                    .salesOrder(so)
                    .product(vi.product)
                    .quantity(vi.quantity)
                    .unitCostSnapshot(costSnapshot)
                    .unitPrice(vi.unitPrice)
                    .subtotal(vi.subtotal)
                    .build();
            savedItems.add(salesOrderItemRepository.save(soi));
        }

        // 6. Audit Log
        auditLogService.log(currentUser, "SO_CREATE", "Created Sales Order #" + so.getSoId() +
                " for " + customer.getCustomerName() + " (Total: ฿" + formattedTotal + ")");

        return SalesOrderDto.fromEntity(so, savedItems);
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

    private boolean matchesStatus(SalesOrderDto dto, String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return true;
        }
        String s = status.trim().toUpperCase();
        String soStatus = dto.getStatus() != null ? dto.getStatus().toUpperCase() : "";
        return soStatus.equals(s);
    }

    private boolean matchesDate(SalesOrderDto dto, String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return true;
        }
        if (dto.getOrderDatetime() == null) {
            return false;
        }
        try {
            String trimmed = dateStr.trim();
            LocalDate filterDate;
            if (trimmed.contains("/")) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d/M/yyyy");
                filterDate = LocalDate.parse(trimmed, dtf);
            } else {
                filterDate = LocalDate.parse(trimmed);
            }
            return dto.getOrderDatetime().toLocalDate().isEqual(filterDate);
        } catch (DateTimeParseException e) {
            return true;
        }
    }
}
