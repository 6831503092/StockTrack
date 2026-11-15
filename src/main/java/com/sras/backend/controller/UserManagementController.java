package com.sras.backend.controller;

import com.sras.backend.dto.ApiResponse;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.dto.RoleChangeRequest;
import com.sras.backend.dto.UserCreateRequest;
import com.sras.backend.dto.UserDto;
import com.sras.backend.entity.User;
import com.sras.backend.enums.Role;
import com.sras.backend.security.CurrentUser;
import com.sras.backend.security.RequireRoles;
import com.sras.backend.service.AuthService;
import com.sras.backend.service.UserManagementService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final AuthService authService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsers(
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "search", required = false) String search,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        authService.requireRole(session, xUserId, Role.ADMIN);
        List<UserDto> users = userManagementService.getUsers(role, search);
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully", users));
    }

    @PostMapping("/requests/user-create")
    @RequireRoles(Role.ADMIN)
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> createUserRequest(
            @RequestBody UserCreateRequest req,
            @CurrentUser User adminUser
    ) {
        ApprovalRequestDto result = userManagementService.submitUserCreateRequest(req, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("User creation request submitted for approval", result));
    }

    @PostMapping("/requests/role-change")
    @RequireRoles(Role.ADMIN)
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> roleChangeRequest(
            @RequestBody RoleChangeRequest req,
            @CurrentUser User adminUser
    ) {
        ApprovalRequestDto result = userManagementService.submitRoleChangeRequest(req, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Role change request submitted for approval", result));
    }
}
