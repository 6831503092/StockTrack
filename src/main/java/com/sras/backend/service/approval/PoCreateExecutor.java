package com.sras.backend.service.approval;

import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.enums.PurchaseOrderStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.BusinessRuleException;
import com.sras.backend.exception.NotFoundException;
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
        PurchaseOrder po = null;
        if (request.getTargetId() != null) {
            po = purchaseOrderRepository.findById(request.getTargetId()).orElse(null);
        }
        if (po == null && request.getRequestId() != null) {
            po = purchaseOrderRepository.findByRequestRequestId(request.getRequestId()).orElse(null);
        }

        if (po == null) {
            throw new NotFoundException("Purchase Order not found for PO_CREATE request #" + request.getRequestId());
        }

        if (po.getStatus() != PurchaseOrderStatus.PENDING) {
            throw new BusinessRuleException("Purchase Order #" + po.getPoId() + " is not in PENDING state (current: " + po.getStatus() + ")");
        }

        po.setStatus(PurchaseOrderStatus.APPROVED);
        purchaseOrderRepository.save(po);
        log.info("PO_CREATE executed: PO #{} approved and ready for receiving checks", po.getPoId());
    }
}
