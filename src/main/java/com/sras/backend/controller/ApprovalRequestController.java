package com.sras.backend.controller;

import com.sras.backend.dto.ApiResponse;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.entity.User;
import com.sras.backend.service.ApprovalRequestService;
import com.sras.backend.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class ApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;
    private final AuthService authService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ApprovalRequestDto>>> getMyRequests(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "status", required = false) String status,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User currentUser = authService.resolveCurrentUser(session, xUserId);
        List<ApprovalRequestDto> requests = approvalRequestService.getMyRequests(currentUser, search, date, status);
        return ResponseEntity.ok(ApiResponse.ok("My requests retrieved successfully", requests));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> getRequestById(
            @PathVariable("id") Integer id,
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        User currentUser = authService.resolveCurrentUser(session, xUserId);
        ApprovalRequestDto request = approvalRequestService.getRequestById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Request retrieved successfully", request));
    }
}
