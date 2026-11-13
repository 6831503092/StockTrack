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
public class UserCreateExecutor implements ApprovalExecutor {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RequestType supports() {
        return RequestType.USER_CREATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        if (request.getPayload() == null || request.getPayload().isBlank()) {
            log.warn("USER_CREATE payload is empty for request #{}", request.getRequestId());
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(request.getPayload());
            int userId = node.path("userId").asInt(request.getTargetId() != null ? request.getTargetId() : 0);
            String fullName = node.path("fullName").asText("");
            String roleStr = node.path("role").asText("STAFF");
            String passwordHash = node.path("passwordHash").asText("");

            User user = userRepository.findById(userId).orElse(new User());
            user.setUserId(userId);
            user.setFullName(fullName);
            user.setRole(Role.valueOf(roleStr));
            if (!passwordHash.isBlank()) {
                user.setPasswordHash(passwordHash);
            }
            user.setIsActive(true);
            userRepository.save(user);
            log.info("USER_CREATE executed: created user #{} ({}) with role {}", userId, fullName, roleStr);
        } catch (Exception e) {
            log.error("Failed to execute USER_CREATE for request #{}", request.getRequestId(), e);
            throw new RuntimeException("Failed to execute user creation: " + e.getMessage(), e);
        }
    }
}
