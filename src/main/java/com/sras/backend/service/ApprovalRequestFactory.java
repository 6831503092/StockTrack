package com.sras.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.User;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.TargetType;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ApprovalRequestFactory {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ObjectMapper objectMapper;

    public ApprovalRequest create(
            RequestType requestType,
            TargetType targetType,
            Integer targetId,
            Object payload,
            String detail,
            User requestedBy
    ) {
        String payloadJson;
        if (payload == null) {
            payloadJson = null;
        } else if (payload instanceof String str) {
            payloadJson = str;
        } else {
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (Exception e) {
                throw new ValidationException("Failed to serialize request payload: " + e.getMessage());
            }
        }

        String truncatedDetail = detail != null && detail.length() > 255
                ? detail.substring(0, 252) + "..."
                : detail;

        ApprovalRequest request = ApprovalRequest.builder()
                .requestType(requestType)
                .targetType(targetType)
                .targetId(targetId)
                .payload(payloadJson)
                .detail(truncatedDetail)
                .status(ApprovalStatus.PENDING)
                .requestedBy(requestedBy)
                .requestedDatetime(LocalDateTime.now())
                .build();

        return approvalRequestRepository.save(request);
    }
}
