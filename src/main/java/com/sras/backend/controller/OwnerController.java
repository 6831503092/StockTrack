package com.sras.backend.controller;

import com.sras.backend.dto.ApiResponse;
import com.sras.backend.dto.OwnerActivityDto;
import com.sras.backend.dto.OwnerDashboardDto;
import com.sras.backend.enums.Role;
import com.sras.backend.service.AuthService;
import com.sras.backend.service.OwnerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;
    private final AuthService authService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<OwnerDashboardDto>> getDashboard(
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.requireRole(session, xUserId, Role.OWNER, Role.ADMIN, Role.MANAGER);
        OwnerDashboardDto dto = ownerService.getDashboard();
        return ResponseEntity.ok(ApiResponse.ok("Owner dashboard retrieved successfully", dto));
    }

    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<List<OwnerActivityDto>>> getActivities(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "date", required = false) String date,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.requireRole(session, xUserId, Role.OWNER, Role.ADMIN, Role.MANAGER);
        List<OwnerActivityDto> activities = ownerService.getActivities(type, search, date);
        return ResponseEntity.ok(ApiResponse.ok("Owner activities retrieved successfully", activities));
    }
}
