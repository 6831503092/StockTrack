package com.sras.backend.controller;

import com.sras.backend.dto.ApiResponse;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.dto.ProductDto;
import com.sras.backend.dto.ProductMoveRequest;
import com.sras.backend.dto.ProductUpdateRequest;
import com.sras.backend.dto.ZoneCreateRequest;
import com.sras.backend.dto.ZoneDto;
import com.sras.backend.dto.ZoneProductSummaryDto;
import com.sras.backend.dto.ZoneUpdateRequest;
import com.sras.backend.entity.User;
import com.sras.backend.enums.Role;
import com.sras.backend.service.AuthService;
import com.sras.backend.service.ZoneService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
public class ZoneController {

    private final ZoneService zoneService;
    private final AuthService authService;

    @GetMapping("/zones")
    public ResponseEntity<ApiResponse<List<ZoneDto>>> getAllZones(
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.resolveCurrentUser(session, xUserId);
        List<ZoneDto> zones = zoneService.getAllZones();
        return ResponseEntity.ok(ApiResponse.ok("Zones retrieved successfully", zones));
    }

    @PostMapping("/zones")
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> createZone(
            @RequestBody ZoneCreateRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User adminUser = authService.requireRole(session, xUserId, Role.ADMIN);
        ApprovalRequestDto request = zoneService.requestZoneCreate(req, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Zone creation request (#" + request.getRequestId() + ") submitted for Manager approval", request));
    }

    @PutMapping("/zones/{zoneCode}")
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> updateZone(
            @PathVariable("zoneCode") String zoneCode,
            @RequestBody ZoneUpdateRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User adminUser = authService.requireRole(session, xUserId, Role.ADMIN);
        ApprovalRequestDto request = zoneService.requestZoneUpdate(zoneCode, req, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Zone capacity update request (#" + request.getRequestId() + ") submitted for Manager approval", request));
    }

    @GetMapping("/zones/summary")
    public ResponseEntity<ApiResponse<ZoneProductSummaryDto>> getZoneProductSummary(
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.resolveCurrentUser(session, xUserId);
        ZoneProductSummaryDto summary = zoneService.getZoneProductSummary();
        return ResponseEntity.ok(ApiResponse.ok("Zone product summary retrieved successfully", summary));
    }

    @GetMapping("/zones/products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getZoneProducts(
            @RequestParam(value = "zone", required = false) String zoneCode,
            @RequestParam(value = "search", required = false) String search,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.resolveCurrentUser(session, xUserId);
        List<ProductDto> products = zoneService.getZoneProducts(zoneCode, search);
        return ResponseEntity.ok(ApiResponse.ok("Zone products retrieved successfully", products));
    }

    @PostMapping("/requests/product-move")
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> submitProductMove(
            @RequestBody ProductMoveRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User adminUser = authService.requireRole(session, xUserId, Role.ADMIN);
        ApprovalRequestDto result = zoneService.submitProductMoveRequest(req, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Product move request submitted for approval", result));
    }

    @PostMapping("/requests/product-update")
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> submitProductUpdate(
            @RequestBody ProductUpdateRequest req,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User adminUser = authService.requireRole(session, xUserId, Role.ADMIN);
        ApprovalRequestDto result = zoneService.submitProductUpdateRequest(req, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Product edit request submitted for approval", result));
    }
}
