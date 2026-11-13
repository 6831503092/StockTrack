package com.sras.backend.service.approval;

import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.enums.PurchaseOrderStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PoCreateExecutor implements ApprovalExecutor {

    private final PurchaseOrderRepository purchaseOrderRepository;

    @Override
    public RequestType supports() {
        return RequestType.PO_CREATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        if (request.getTargetId() == null) {
            log.warn("TargetId is null for PO_CREATE request #{}", request.getRequestId());
            return;
        }

        PurchaseOrder po = purchaseOrderRepository.findById(request.getTargetId())
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found with ID: " + request.getTargetId()));

        po.setStatus(PurchaseOrderStatus.APPROVED);
        purchaseOrderRepository.save(po);
        log.info("PO_CREATE executed: PO #{} approved and ready for receiving checks", po.getPoId());
    }
}
