package com.sras.backend.controller;

import com.sras.backend.dto.ApiResponse;
import com.sras.backend.dto.StaffCheckRequest;
import com.sras.backend.dto.StaffDashboardDto;
import com.sras.backend.dto.StaffOrderDto;
import com.sras.backend.dto.StaffRejectRequest;
import com.sras.backend.dto.StaffTransferDto;
import com.sras.backend.entity.User;
import com.sras.backend.enums.Role;
import com.sras.backend.service.AuthService;
import com.sras.backend.service.StaffService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;
    private final AuthService authService;

    // ==========================================
    // 1. RECEIVE WORKFLOW
    // ==========================================

    @GetMapping("/receives/pending")
    public ResponseEntity<ApiResponse<List<StaffOrderDto>>> getPendingReceives(
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.requireRole(session, xUserId, Role.STAFF, Role.MANAGER, Role.ADMIN);
        List<StaffOrderDto> list = staffService.getPendingReceives();
        return ResponseEntity.ok(ApiResponse.ok("Pending receiving orders retrieved", list));
    }

    @PostMapping("/receives/{poId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptReceive(
            @PathVariable Integer poId,
            @RequestBody(required = false) StaffCheckRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User staff = authService.requireRole(session, xUserId, Role.STAFF, Role.MANAGER, Role.ADMIN);
        staffService.acceptReceive(poId, staff, req);
        return ResponseEntity.ok(ApiResponse.ok("Goods received and inventory updated successfully"));
    }

    @PostMapping("/receives/{poId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectReceive(
            @PathVariable Integer poId,
            @RequestBody(required = false) StaffRejectRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User staff = authService.requireRole(session, xUserId, Role.STAFF, Role.MANAGER, Role.ADMIN);
        staffService.rejectReceive(poId, staff, req);
        return ResponseEntity.ok(ApiResponse.ok("Receiving order rejected"));
    }

    // ==========================================
    // 2. STOCK OUT WORKFLOW
    // ==========================================

    @GetMapping("/stock-out/pending")
    public ResponseEntity<ApiResponse<List<StaffOrderDto>>> getPendingStockOuts(
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.requireRole(session, xUserId, Role.STAFF, Role.MANAGER, Role.ADMIN);
        List<StaffOrderDto> list = staffService.getPendingStockOuts();
        return ResponseEntity.ok(ApiResponse.ok("Pending stock out orders retrieved", list));
    }

    @PostMapping("/stock-out/{soId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmStockOut(
            @PathVariable Integer soId,
            @RequestBody(required = false) StaffCheckRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User staff = authService.requireRole(session, xUserId, Role.STAFF, Role.MANAGER, Role.ADMIN);
        staffService.confirmStockOut(soId, staff, req);
        return ResponseEntity.ok(ApiResponse.ok("Stock out confirmed and inventory updated successfully"));
    }

    @PostMapping("/stock-out/{soId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectStockOut(
            @PathVariable Integer soId,
            @RequestBody(required = false) StaffRejectRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User staff = authService.requireRole(session, xUserId, Role.STAFF, Role.MANAGER, Role.ADMIN);
        staffService.rejectStockOut(soId, staff, req);
        return ResponseEntity.ok(ApiResponse.ok("Stock out order rejected"));
    }

    // ==========================================
    // 3. TRANSFER WORKFLOW
    // ==========================================

    @GetMapping("/transfers/pending")
    public ResponseEntity<ApiResponse<List<StaffTransferDto>>> getPendingTransfers(
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.requireRole(session, xUserId, Role.STAFF, Role.MANAGER, Role.ADMIN);
        List<StaffTransferDto> list = staffService.getPendingTransfers();
        return ResponseEntity.ok(ApiResponse.ok("Pending transfers retrieved", list));
    }

    @PostMapping("/transfers/{movementId}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyTransfer(
            @PathVariable Integer movementId,
            @RequestBody(required = false) StaffCheckRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User staff = authService.requireRole(session, xUserId, Role.STAFF, Role.MANAGER, Role.ADMIN);
        staffService.verifyTransfer(movementId, staff, req);
        return ResponseEntity.ok(ApiResponse.ok("Stock transfer verified and completed"));
    }

    @PostMapping("/transfers/{movementId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectTransfer(
            @PathVariable Integer movementId,
            @RequestBody(required = false) StaffRejectRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User staff = authService.requireRole(session, xUserId, Role.STAFF, Role.MANAGER, Role.ADMIN);
        staffService.rejectTransfer(movementId, staff, req);
        return ResponseEntity.ok(ApiResponse.ok("Stock transfer rejected"));
    }

    // ==========================================
    // 4. DASHBOARD & ACTIVITIES
    // ==========================================

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<StaffDashboardDto>> getDashboard(
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.requireRole(session, xUserId, Role.STAFF, Role.MANAGER, Role.ADMIN);
        StaffDashboardDto summary = staffService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.ok("Staff dashboard summary retrieved", summary));
    }
}
