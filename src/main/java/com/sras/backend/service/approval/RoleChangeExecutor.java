package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.payload.RoleChangePayload;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.User;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.Role;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RoleChangeExecutor extends BaseApprovalExecutor<RoleChangePayload> {

    private final UserRepository userRepository;

    public RoleChangeExecutor(UserRepository userRepository, ObjectMapper objectMapper) {
        super(objectMapper);
        this.userRepository = userRepository;
    }

    @Override
    public RequestType supports() {
        return RequestType.ROLE_CHANGE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        RoleChangePayload payload = parsePayload(request, RoleChangePayload.class);
        int userId = payload.userId() != null && payload.userId() > 0
                ? payload.userId()
                : (request.getTargetId() != null ? request.getTargetId() : 0);
        String newRoleStr = payload.newRole();

        if (userId <= 0 || newRoleStr == null || newRoleStr.isBlank()) {
            throw new ValidationException("User ID and new role are required for ROLE_CHANGE");
        }

        User user = userRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new NotFoundException("Active user not found with ID: " + userId));

        try {
            user.setRole(Role.valueOf(newRoleStr.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid role: " + newRoleStr);
        }

        userRepository.save(user);
        log.info("ROLE_CHANGE executed: changed user #{} role to {}", userId, newRoleStr);
    }
}
