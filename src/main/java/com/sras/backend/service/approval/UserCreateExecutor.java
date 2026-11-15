package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.payload.UserCreatePayload;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.User;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.Role;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserCreateExecutor extends BaseApprovalExecutor<UserCreatePayload> {

    private final UserRepository userRepository;

    public UserCreateExecutor(UserRepository userRepository, ObjectMapper objectMapper) {
        super(objectMapper);
        this.userRepository = userRepository;
    }

    @Override
    public RequestType supports() {
        return RequestType.USER_CREATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        UserCreatePayload payload = parsePayload(request, UserCreatePayload.class);
        int userId = payload.userId() != null && payload.userId() > 0
                ? payload.userId()
                : (request.getTargetId() != null ? request.getTargetId() : 0);

        if (userId <= 0) {
            throw new ValidationException("Valid userId is required for USER_CREATE");
        }

        String fullName = payload.fullName() != null ? payload.fullName().trim() : "";
        String roleStr = payload.role() != null ? payload.role().trim().toUpperCase() : "STAFF";

        User user = userRepository.findById(userId).orElseGet(() -> {
            User u = new User();
            u.setUserId(userId);
            return u;
        });

        user.setFullName(fullName);
        try {
            user.setRole(Role.valueOf(roleStr));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid role for USER_CREATE: " + roleStr);
        }

        if (payload.passwordHash() != null && !payload.passwordHash().isBlank()) {
            user.setPasswordHash(payload.passwordHash());
        }
        user.setIsActive(true);
        userRepository.save(user);
        log.info("USER_CREATE executed: created user #{} ({}) with role {}", userId, fullName, roleStr);
    }
}
