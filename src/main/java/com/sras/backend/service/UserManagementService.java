package com.sras.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.dto.RoleChangeRequest;
import com.sras.backend.dto.UserCreateRequest;
import com.sras.backend.dto.UserDto;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.User;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.Role;
import com.sras.backend.enums.TargetType;
import com.sras.backend.repository.ApprovalRequestRepository;
import com.sras.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<UserDto> getUsers(String roleFilter, String searchQuery) {
        List<User> allUsers = userRepository.findAll();

        return allUsers.stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .filter(u -> u.getRole() != Role.OWNER)
                .filter(u -> {
                    if (roleFilter == null || roleFilter.isBlank() || roleFilter.equalsIgnoreCase("ALL")) {
                        return true;
                    }
                    return u.getRole() != null && u.getRole().name().equalsIgnoreCase(roleFilter.trim());
                })
                .filter(u -> {
                    if (searchQuery == null || searchQuery.isBlank()) {
                        return true;
                    }
                    String q = searchQuery.toLowerCase().trim();
                    boolean matchId = u.getUserId() != null && String.valueOf(u.getUserId()).contains(q);
                    boolean matchFullName = u.getFullName() != null && u.getFullName().toLowerCase().contains(q);
                    return matchId || matchFullName;
                })
                .sorted(Comparator.comparing(User::getUserId))
                .map(UserDto::fromEntity)
                .toList();
    }

    @Transactional
    public ApprovalRequestDto submitUserCreateRequest(UserCreateRequest req, User adminUser) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        String fullName = req.fullName() != null ? req.fullName().trim() : "";
        String password = req.password() != null ? req.password().trim() : "";
        String roleStr = req.role() != null ? req.role().trim() : "";
        String reason = req.reason() != null ? req.reason().trim() : "";

        if (fullName.isEmpty() || fullName.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Full name is required and cannot exceed 50 characters");
        }
        if (password.length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 4 characters");
        }
        Role targetRole;
        try {
            targetRole = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role specified: " + roleStr);
        }

        if (reason.isEmpty()) {
            reason = "Account creation for new user (" + targetRole.name() + ")";
        }

        if (targetRole == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create an account with role OWNER");
        }

        // Generate unique 6-digit User ID starting with 503 (503000 - 503999)
        Integer assignedUserId = generateUniqueUserId(req.userId());
        String userIdentifier = String.valueOf(assignedUserId);

        String passwordHash = passwordEncoder.encode(password);

        java.util.Map<String, Object> payloadMap = new java.util.HashMap<>();
        payloadMap.put("userId", assignedUserId);
        payloadMap.put("numericUserId", assignedUserId);
        payloadMap.put("fullName", fullName);
        payloadMap.put("role", targetRole.name());
        payloadMap.put("reason", reason);
        payloadMap.put("passwordHash", passwordHash);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize request payload");
        }

        String detail = "Create new user: " + fullName + " (" + assignedUserId + ", Role: " + targetRole.name() + "). Reason: " + reason;
        if (detail.length() > 255) {
            detail = detail.substring(0, 255);
        }

        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType(RequestType.USER_CREATE)
                .targetType(TargetType.USER)
                .targetId(assignedUserId)
                .payload(payloadJson)
                .detail(detail)
                .status(ApprovalStatus.PENDING)
                .requestedBy(adminUser)
                .requestedDatetime(LocalDateTime.now())
                .build();

        ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);
        auditLogService.log(adminUser, "REQUEST_SUBMIT",
                "Requested creation of user: " + fullName + " (ID: " + assignedUserId + ", Role: " + targetRole.name() + "). Reason: " + reason);

        return ApprovalRequestDto.fromEntity(saved);
    }

    private Integer generateUniqueUserId(String requestedId) {
        if (requestedId != null && !requestedId.isBlank()) {
            try {
                int id = Integer.parseInt(requestedId.trim());
                if (id >= 503000 && id <= 503999 && isUserIdAvailable(id)) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int attempts = 0; attempts < 1000; attempts++) {
            int candidate = 503000 + random.nextInt(1000);
            if (isUserIdAvailable(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No available 503xxx user ID slots left");
    }

    private boolean isUserIdAvailable(int id) {
        if (userRepository.existsById(id)) {
            return false;
        }
        List<ApprovalRequest> pendingCreations = approvalRequestRepository.findByRequestTypeAndStatus(
                RequestType.USER_CREATE, ApprovalStatus.PENDING);
        for (ApprovalRequest pending : pendingCreations) {
            if (pending.getPayload() != null &&
                    (pending.getPayload().contains("\"userId\":" + id) ||
                     pending.getPayload().contains("\"userId\":\"" + id + "\""))) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    public ApprovalRequestDto submitRoleChangeRequest(RoleChangeRequest req, User adminUser) {
        if (req == null || req.userId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is required");
        }

        String newRoleStr = req.newRole() != null ? req.newRole().trim() : "";
        String reason = req.reason() != null ? req.reason().trim() : "";

        if (reason.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reason for role change is required");
        }
        if (reason.length() > 255) {
            reason = reason.substring(0, 255);
        }

        Role targetNewRole;
        try {
            targetNewRole = Role.valueOf(newRoleStr.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role specified: " + newRoleStr);
        }

        if (targetNewRole == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot assign system role OWNER");
        }

        User targetUser = userRepository.findByUserIdAndIsActiveTrue(req.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or is inactive"));

        if (targetUser.getUserId().equals(adminUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot request a role change for your own account");
        }

        if (targetUser.getRole() == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change the role of system OWNER");
        }

        if (targetUser.getRole() == targetNewRole) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already has role " + targetNewRole.name());
        }

        if (approvalRequestRepository.existsByRequestTypeAndTargetIdAndStatus(
                RequestType.ROLE_CHANGE, targetUser.getUserId(), ApprovalStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A pending role change request already exists for this user");
        }

        Map<String, Object> payloadMap = Map.of(
                "userId", targetUser.getUserId(),
                "fullName", targetUser.getFullName(),
                "currentRole", targetUser.getRole().name(),
                "newRole", targetNewRole.name(),
                "reason", reason
        );

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize request payload");
        }

        String detail = "Change role: " + targetUser.getFullName() + " (" + targetUser.getRole().name() + " -> " + targetNewRole.name() + ")";
        if (detail.length() > 255) {
            detail = detail.substring(0, 255);
        }

        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType(RequestType.ROLE_CHANGE)
                .targetType(TargetType.USER)
                .targetId(targetUser.getUserId())
                .payload(payloadJson)
                .detail(detail)
                .status(ApprovalStatus.PENDING)
                .requestedBy(adminUser)
                .requestedDatetime(LocalDateTime.now())
                .build();

        ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);
        auditLogService.log(adminUser, "REQUEST_SUBMIT", "Requested role change for user ID " + targetUser.getUserId() + " to " + targetNewRole.name());

        return ApprovalRequestDto.fromEntity(saved);
    }
}
