package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.User;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.Role;
import com.sras.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleChangeExecutor implements ApprovalExecutor {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RequestType supports() {
        return RequestType.ROLE_CHANGE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        if (request.getPayload() == null || request.getPayload().isBlank()) {
            log.warn("ROLE_CHANGE payload is empty for request #{}", request.getRequestId());
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(request.getPayload());
            int userId = node.path("userId").asInt(request.getTargetId() != null ? request.getTargetId() : 0);
            String newRoleStr = node.path("newRole").asText("");

            if (userId <= 0 || newRoleStr.isBlank()) {
                throw new IllegalArgumentException("Invalid user ID or new role in payload");
            }

            User user = userRepository.findByUserIdAndIsActiveTrue(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Active user not found with ID: " + userId));

            user.setRole(Role.valueOf(newRoleStr));
            userRepository.save(user);
            log.info("ROLE_CHANGE executed: changed user #{} role to {}", userId, newRoleStr);
        } catch (Exception e) {
            log.error("Failed to execute ROLE_CHANGE for request #{}", request.getRequestId(), e);
            throw new RuntimeException("Failed to execute role change: " + e.getMessage(), e);
        }
    }
}
