package com.sras.backend.controller;

import com.sras.backend.entity.User;
import com.sras.backend.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public record LoginRequest(String userId, String username, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req, HttpSession session) {
        Map<String, Object> body = new LinkedHashMap<>();

        String identifier = (req != null && req.userId() != null && !req.userId().isBlank())
                ? req.userId().trim()
                : (req != null && req.username() != null ? req.username().trim() : "");

        if (req == null || identifier.isBlank() || req.password() == null || req.password().isBlank()) {
            body.put("success", false);
            body.put("message", "กรุณากรอกข้อมูลให้ครบถ้วน");
            return ResponseEntity.badRequest().body(body);
        }

        Optional<User> userOpt = findMatchingUser(identifier);
        if (userOpt.isEmpty() || !passwordEncoder.matches(req.password(), userOpt.get().getPasswordHash())) {
            body.put("success", false);
            body.put("message", "Invalid ID or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        User user = userOpt.get();
        session.setAttribute("USER_ID", user.getUserId());
        session.setAttribute("USER_ROLE", user.getRole().name());

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("userId", user.getUserId());
        userInfo.put("username", user.getUsername());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("role", user.getRole().name());

        body.put("success", true);
        body.put("message", "Login successful");
        body.put("user", userInfo);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true));
    }

    private Optional<User> findMatchingUser(String identifier) {
        try {
            Integer parsedId = Integer.valueOf(identifier);
            Optional<User> byId = userRepository.findByUserIdAndIsActiveTrue(parsedId);
            if (byId.isPresent()) {
                return byId;
            }
        } catch (NumberFormatException ignored) {
            // Not a numeric ID; fall through to username lookup
        }
        return userRepository.findByUsernameAndIsActiveTrue(identifier);
    }
}

