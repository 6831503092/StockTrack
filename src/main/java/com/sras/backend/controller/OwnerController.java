package com.sras.backend.controller;

import com.sras.backend.dto.ApiResponse;
import com.sras.backend.dto.OwnerActivityDto;
import com.sras.backend.dto.OwnerDashboardDto;
import com.sras.backend.enums.Role;
import com.sras.backend.security.RequireRoles;
import com.sras.backend.service.OwnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
@RequireRoles({Role.OWNER, Role.ADMIN, Role.MANAGER})
public class OwnerController {

    private final OwnerService ownerService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<OwnerDashboardDto>> getDashboard() {
        OwnerDashboardDto dto = ownerService.getDashboard();
        return ResponseEntity.ok(ApiResponse.ok("Owner dashboard retrieved successfully", dto));
    }

    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<List<OwnerActivityDto>>> getActivities(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "date", required = false) String date
    ) {
        List<OwnerActivityDto> activities = ownerService.getActivities(type, search, date);
        return ResponseEntity.ok(ApiResponse.ok("Owner activities retrieved successfully", activities));
    }
}
