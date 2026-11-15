package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.exception.ValidationException;

public abstract class BaseApprovalExecutor<T> implements ApprovalExecutor {

    protected final ObjectMapper objectMapper;

    protected BaseApprovalExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected T parsePayload(ApprovalRequest request, Class<T> payloadClass) {
        if (request.getPayload() == null || request.getPayload().isBlank()) {
            throw new ValidationException("Payload is required for " + supports() + " request #" + request.getRequestId());
        }
        try {
            return objectMapper.readValue(request.getPayload(), payloadClass);
        } catch (Exception e) {
            throw new ValidationException("Failed to parse " + supports() + " payload: " + e.getMessage());
        }
    }

    protected T parsePayloadOptional(ApprovalRequest request, Class<T> payloadClass) {
        if (request.getPayload() == null || request.getPayload().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(request.getPayload(), payloadClass);
        } catch (Exception e) {
            throw new ValidationException("Failed to parse " + supports() + " payload: " + e.getMessage());
        }
    }
}
