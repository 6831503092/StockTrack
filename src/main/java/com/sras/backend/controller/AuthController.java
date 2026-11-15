package com.sras.backend.controller;

import com.sras.backend.dto.ApiResponse;
import com.sras.backend.dto.LoginRequest;
import com.sras.backend.dto.UserDto;
import com.sras.backend.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@RequestBody LoginRequest req, HttpSession session) {
        UserDto user = authService.login(req, session);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", user));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpSession session) {
        authService.logout(session);
        return ResponseEntity.ok(ApiResponse.ok("Logout successful"));
    }

    @GetMapping({"/auth/me", "/me"})
    public ResponseEntity<ApiResponse<Void>> getCurrentUser(
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        UserDto user = authService.getCurrentUserDto(session, xUserId);
        return ResponseEntity.ok(ApiResponse.ok("User authenticated", user));
    }
}
