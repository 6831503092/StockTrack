package com.sras.backend.service;

import com.sras.backend.entity.AuditLog;
import com.sras.backend.entity.User;
import com.sras.backend.enums.AuditAction;
import com.sras.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(User user, AuditAction action, String targetType, Object targetId, String detail) {
        if (user == null) {
            return;
        }

        AuditAction resolvedAction = action != null ? action : AuditAction.UNKNOWN;
        String resolvedTargetType = targetType != null ? targetType : resolvedAction.getDefaultTargetType();
        String resolvedTargetId = targetId != null ? String.valueOf(targetId) : null;

        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .role(user.getRole())
                .actionType(resolvedAction.name())
                .targetType(resolvedTargetType)
                .targetId(resolvedTargetId)
                .detail(detail != null && detail.length() > 255 ? detail.substring(0, 255) : detail)
                .actionDatetime(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }

    public void log(User user, AuditAction action, String detail) {
        String defTargetType = action != null ? action.getDefaultTargetType() : null;
        log(user, action, defTargetType, null, detail);
    }

    public void log(User user, String actionType, String targetType, Object targetId, String detail) {
        AuditAction action = AuditAction.fromString(actionType);
        log(user, action, targetType, targetId, detail);
    }

    public void log(User user, String actionType, String detail) {
        AuditAction action = AuditAction.fromString(actionType);
        log(user, action, action.getDefaultTargetType(), null, detail);
    }
}
