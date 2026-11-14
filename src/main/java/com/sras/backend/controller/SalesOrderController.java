package com.sras.backend.controller;

import com.sras.backend.dto.ApiResponse;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.dto.CustomerDto;
import com.sras.backend.dto.CustomerRequest;
import com.sras.backend.dto.SalesOrderCreateRequest;
import com.sras.backend.dto.SalesOrderDto;
import com.sras.backend.entity.User;
import com.sras.backend.enums.Role;
import com.sras.backend.service.AuthService;
import com.sras.backend.service.SalesOrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;
    private final AuthService authService;

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<List<CustomerDto>>> getCustomers(
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.resolveCurrentUser(session, xUserId);
        List<CustomerDto> customers = salesOrderService.getActiveCustomers();
        return ResponseEntity.ok(ApiResponse.ok("Customers retrieved successfully", customers));
    }

    @PostMapping("/customers")
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> createCustomer(
            @RequestBody CustomerRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User user = authService.requireRole(session, xUserId, Role.ADMIN, Role.MANAGER, Role.OWNER);
        ApprovalRequestDto request = salesOrderService.requestCustomerCreate(req, user);
        return ResponseEntity.ok(ApiResponse.ok("Customer creation request (#" + request.getRequestId() + ") submitted for Manager approval", request));
    }

    @PutMapping("/customers/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> updateCustomer(
            @PathVariable("id") Integer id,
            @RequestBody CustomerRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User user = authService.requireRole(session, xUserId, Role.ADMIN, Role.MANAGER, Role.OWNER);
        ApprovalRequestDto request = salesOrderService.requestCustomerUpdate(id, req, user);
        return ResponseEntity.ok(ApiResponse.ok("Customer update request (#" + request.getRequestId() + ") submitted for Manager approval", request));
    }

    @DeleteMapping("/customers/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> deleteCustomer(
            @PathVariable("id") Integer id,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User user = authService.requireRole(session, xUserId, Role.ADMIN, Role.MANAGER, Role.OWNER);
        ApprovalRequestDto request = salesOrderService.requestCustomerDeactivate(id, user);
        return ResponseEntity.ok(ApiResponse.ok("Customer deletion request (#" + request.getRequestId() + ") submitted for Manager approval", request));
    }

    @GetMapping("/sales-orders")
    public ResponseEntity<ApiResponse<List<SalesOrderDto>>> getSalesOrders(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "date", required = false) String date,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.resolveCurrentUser(session, xUserId);
        List<SalesOrderDto> list = salesOrderService.getAllSalesOrders(search, status, date);
        return ResponseEntity.ok(ApiResponse.ok("Sales orders retrieved successfully", list));
    }

    @GetMapping("/sales-orders/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<SalesOrderDto>> getSalesOrderById(
            @PathVariable("id") Integer id,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.resolveCurrentUser(session, xUserId);
        SalesOrderDto dto = salesOrderService.getSalesOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok("Sales order retrieved successfully", dto));
    }

    @PostMapping("/sales-orders")
    public ResponseEntity<ApiResponse<SalesOrderDto>> createSalesOrder(
            @RequestBody SalesOrderCreateRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User currentUser = authService.resolveCurrentUser(session, xUserId);
        SalesOrderDto created = salesOrderService.createSalesOrder(req, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Sales order created successfully", created));
    }
}
