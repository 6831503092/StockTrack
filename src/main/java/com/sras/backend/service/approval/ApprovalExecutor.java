package com.sras.backend.service.approval;

import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.enums.RequestType;

public interface ApprovalExecutor {
    RequestType supports();
    void execute(ApprovalRequest request);
}
