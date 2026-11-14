package com.sras.backend.controller;

import com.sras.backend.dto.ApiResponse;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.dto.PurchaseOrderCreateRequest;
import com.sras.backend.dto.PurchaseOrderDto;
import com.sras.backend.dto.SupplierDto;
import com.sras.backend.dto.SupplierRequest;
import com.sras.backend.entity.User;
import com.sras.backend.enums.Role;
import com.sras.backend.service.AuthService;
import com.sras.backend.service.PurchaseOrderService;
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
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final AuthService authService;

    @GetMapping("/suppliers")
    public ResponseEntity<ApiResponse<List<SupplierDto>>> getSuppliers(
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.resolveCurrentUser(session, xUserId);
        List<SupplierDto> suppliers = purchaseOrderService.getActiveSuppliers();
        return ResponseEntity.ok(ApiResponse.ok("Suppliers retrieved successfully", suppliers));
    }

    @PostMapping("/suppliers")
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> createSupplier(
            @RequestBody SupplierRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User user = authService.requireRole(session, xUserId, Role.ADMIN, Role.MANAGER, Role.OWNER);
        ApprovalRequestDto request = purchaseOrderService.requestSupplierCreate(req, user);
        return ResponseEntity.ok(ApiResponse.ok("Supplier creation request (#" + request.getRequestId() + ") submitted for Manager approval", request));
    }

    @PutMapping("/suppliers/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> updateSupplier(
            @PathVariable("id") Integer id,
            @RequestBody SupplierRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User user = authService.requireRole(session, xUserId, Role.ADMIN, Role.MANAGER, Role.OWNER);
        ApprovalRequestDto request = purchaseOrderService.requestSupplierUpdate(id, req, user);
        return ResponseEntity.ok(ApiResponse.ok("Supplier update request (#" + request.getRequestId() + ") submitted for Manager approval", request));
    }

    @DeleteMapping("/suppliers/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> deleteSupplier(
            @PathVariable("id") Integer id,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User user = authService.requireRole(session, xUserId, Role.ADMIN, Role.MANAGER, Role.OWNER);
        ApprovalRequestDto request = purchaseOrderService.requestSupplierDeactivate(id, user);
        return ResponseEntity.ok(ApiResponse.ok("Supplier deletion request (#" + request.getRequestId() + ") submitted for Manager approval", request));
    }

    @GetMapping("/purchase-orders")
    public ResponseEntity<ApiResponse<List<PurchaseOrderDto>>> getPurchaseOrders(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "date", required = false) String date,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.resolveCurrentUser(session, xUserId);
        List<PurchaseOrderDto> list = purchaseOrderService.getAllPurchaseOrders(search, status, date);
        return ResponseEntity.ok(ApiResponse.ok("Purchase orders retrieved successfully", list));
    }

    @GetMapping("/purchase-orders/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> getPurchaseOrderById(
            @PathVariable("id") Integer id,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.resolveCurrentUser(session, xUserId);
        PurchaseOrderDto dto = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok("Purchase order retrieved successfully", dto));
    }

    @PostMapping("/purchase-orders")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> createPurchaseOrder(
            @RequestBody PurchaseOrderCreateRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User currentUser = authService.resolveCurrentUser(session, xUserId);
        PurchaseOrderDto created = purchaseOrderService.createPurchaseOrder(req, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Purchase order created successfully", created));
    }
}
