package com.sras.backend.service;

import com.sras.backend.entity.AuditLog;
import com.sras.backend.entity.User;
import com.sras.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(User user, String actionType, String detail) {
        if (user == null) {
            return;
        }
        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .role(user.getRole())
                .actionType(actionType != null ? actionType : "UNKNOWN")
                .detail(detail != null && detail.length() > 255 ? detail.substring(0, 255) : detail)
                .actionDatetime(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }
}
