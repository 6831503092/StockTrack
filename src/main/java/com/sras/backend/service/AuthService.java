package com.sras.backend.service;

import com.sras.backend.dto.LoginRequest;
import com.sras.backend.dto.UserDto;
import com.sras.backend.entity.User;
import com.sras.backend.enums.Role;
import com.sras.backend.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public static final String SESSION_USER_ID = "USER_ID";
    public static final String SESSION_USER_ROLE = "USER_ROLE";

    public UserDto login(LoginRequest req, HttpSession session) {
        if (req == null || req.getIdentifier().isBlank() || req.password() == null || req.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "กรุณากรอกข้อมูลให้ครบถ้วน");
        }

        User user = findMatchingUser(req.getIdentifier())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid ID or password"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid ID or password");
        }

        if (session != null) {
            session.setAttribute(SESSION_USER_ID, user.getUserId());
            session.setAttribute(SESSION_USER_ROLE, user.getRole().name());
        }

        return UserDto.fromEntity(user);
    }

    public void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    public UserDto getCurrentUserDto(HttpSession session, String xUserIdHeader) {
        User user = resolveCurrentUser(session, xUserIdHeader);
        return UserDto.fromEntity(user);
    }

    public User resolveCurrentUser(HttpSession session, String xUserIdHeader) {
        Integer resolvedUserId = null;

        if (session != null && session.getAttribute(SESSION_USER_ID) != null) {
            resolvedUserId = (Integer) session.getAttribute(SESSION_USER_ID);
        } else if (xUserIdHeader != null && !xUserIdHeader.isBlank()) {
            try {
                resolvedUserId = Integer.valueOf(xUserIdHeader.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (resolvedUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in to continue");
        }

        return userRepository.findByUserIdAndIsActiveTrue(resolvedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is inactive or not found"));
    }

    public User requireRole(HttpSession session, String xUserIdHeader, Role... allowedRoles) {
        User user = resolveCurrentUser(session, xUserIdHeader);
        boolean hasRole = Arrays.stream(allowedRoles).anyMatch(r -> r == user.getRole());
        if (!hasRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: insufficient permissions");
        }
        return user;
    }

    private Optional<User> findMatchingUser(String identifier) {
        try {
            Integer parsedId = Integer.valueOf(identifier);
            Optional<User> byId = userRepository.findByUserIdAndIsActiveTrue(parsedId);
            if (byId.isPresent()) {
                return byId;
            }
        } catch (NumberFormatException ignored) {
            // Identifier is not a numeric User ID, proceed to full name lookup
        }
        return userRepository.findByFullNameIgnoreCaseAndIsActiveTrue(identifier.trim());
    }
}
