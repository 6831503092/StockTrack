package com.sras.backend.service.approval;

import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.BusinessRuleException;
import com.sras.backend.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ApprovalDispatcher {

    private final Map<RequestType, ApprovalExecutor> executorMap = new EnumMap<>(RequestType.class);

    public ApprovalDispatcher(List<ApprovalExecutor> executors) {
        for (ApprovalExecutor executor : executors) {
            executorMap.put(executor.supports(), executor);
        }
    }

    public void dispatch(ApprovalRequest request) {
        if (request == null || request.getRequestType() == null) {
            throw new ValidationException("Approval request and request type must not be null");
        }
        ApprovalExecutor executor = executorMap.get(request.getRequestType());
        if (executor == null) {
            throw new BusinessRuleException("No executor registered for RequestType " + request.getRequestType());
        }
        log.info("Executing approval for request #{} of type {}", request.getRequestId(), request.getRequestType());
        executor.execute(request);
    }
}
