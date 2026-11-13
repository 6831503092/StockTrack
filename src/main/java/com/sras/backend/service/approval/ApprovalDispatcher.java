package com.sras.backend.service.approval;

import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.enums.RequestType;
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
            return;
        }
        ApprovalExecutor executor = executorMap.get(request.getRequestType());
        if (executor != null) {
            log.info("Executing approval for request #{} of type {}", request.getRequestId(), request.getRequestType());
            executor.execute(request);
        } else {
            log.warn("No executor registered for RequestType {}", request.getRequestType());
        }
    }
}
