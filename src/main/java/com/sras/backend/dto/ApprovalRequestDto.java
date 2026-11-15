package com.sras.backend.dto;

import com.sras.backend.entity.ApprovalRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestDto {
    private Integer requestId;
    private String requestType;
    private String targetType;
    private Integer targetId;
    private String detail;
    private String payload;
    private String status;
    private Integer requestedByUserId;
    private String requestedByName;
    private String role;
    private LocalDateTime requestedDatetime;
    private Integer decidedByUserId;
    private String decidedByName;
    private LocalDateTime decidedDatetime;
    private String decisionReason;

    public static ApprovalRequestDto fromEntity(ApprovalRequest entity) {
        if (entity == null) {
            return null;
        }
        return ApprovalRequestDto.builder()
                .requestId(entity.getRequestId())
                .requestType(entity.getRequestType() != null ? entity.getRequestType().name() : null)
                .targetType(entity.getTargetType() != null ? entity.getTargetType().name() : null)
                .targetId(entity.getTargetId())
                .detail(entity.getDetail())
                .payload(sanitizePayload(entity.getPayload()))
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .requestedByUserId(entity.getRequestedBy() != null ? entity.getRequestedBy().getUserId() : null)
                .requestedByName(entity.getRequestedBy() != null ? entity.getRequestedBy().getFullName() : null)
                .role(entity.getRequestedBy() != null && entity.getRequestedBy().getRole() != null
                        ? entity.getRequestedBy().getRole().name() : null)
                .requestedDatetime(entity.getRequestedDatetime())
                .decidedByUserId(entity.getDecidedBy() != null ? entity.getDecidedBy().getUserId() : null)
                .decidedByName(entity.getDecidedBy() != null ? entity.getDecidedBy().getFullName() : null)
                .decidedDatetime(entity.getDecidedDatetime())
                .decisionReason(entity.getDecisionReason())
                .build();
    }

    private static String sanitizePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return payload;
        }
        return payload.replaceAll("\"passwordHash\"\\s*:\\s*\"[^\"]*\"", "\"passwordHash\":\"[PROTECTED]\"")
                      .replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"[PROTECTED]\"");
    }
}
