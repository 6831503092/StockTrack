package com.sras.backend.service.approval;

import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.SalesOrder;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.SalesOrderStatus;
import com.sras.backend.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SoCreateExecutor implements ApprovalExecutor {

    private final SalesOrderRepository salesOrderRepository;

    @Override
    public RequestType supports() {
        return RequestType.SO_CREATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        SalesOrder so = null;
        if (request.getTargetId() != null) {
            so = salesOrderRepository.findById(request.getTargetId()).orElse(null);
        }
        if (so == null && request.getRequestId() != null) {
            so = salesOrderRepository.findByRequestRequestId(request.getRequestId()).orElse(null);
        }

        if (so == null) {
            log.warn("Sales Order not found for SO_CREATE request #{}", request.getRequestId());
            return;
        }

        so.setStatus(SalesOrderStatus.APPROVED);
        salesOrderRepository.save(so);
        log.info("SO_CREATE executed: SO #{} approved and ready for issue checks", so.getSoId());
    }
}
