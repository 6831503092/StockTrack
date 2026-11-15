package com.sras.backend.service;

import com.sras.backend.dto.StaffActivityItemDto;
import com.sras.backend.dto.StaffCheckRequest;
import com.sras.backend.dto.StaffDashboardDto;
import com.sras.backend.dto.StaffOrderDto;
import com.sras.backend.dto.StaffRejectRequest;
import com.sras.backend.dto.StaffTransferDto;
import com.sras.backend.entity.AuditLog;
import com.sras.backend.entity.User;
import com.sras.backend.enums.MovementStatus;
import com.sras.backend.enums.ReceivingCheckResult;
import com.sras.backend.enums.StockOutCheckResult;
import com.sras.backend.repository.AuditLogRepository;
import com.sras.backend.repository.ReceivingCheckRepository;
import com.sras.backend.repository.StockMovementRepository;
import com.sras.backend.repository.StockOutCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final ReceivingService receivingService;
    private final StockOutService stockOutService;
    private final TransferService transferService;

    private final ReceivingCheckRepository receivingCheckRepository;
    private final StockOutCheckRepository stockOutCheckRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AuditLogRepository auditLogRepository;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    // ==========================================
    // DELEGATIONS FOR WORKFLOW OPERATIONS
    // ==========================================

    public List<StaffOrderDto> getPendingReceives() {
        return receivingService.getPendingReceives();
    }

    public void acceptReceive(Integer poId, User staffUser, StaffCheckRequest req) {
        receivingService.acceptReceive(poId, staffUser, req);
    }

    public void rejectReceive(Integer poId, User staffUser, StaffRejectRequest req) {
        receivingService.rejectReceive(poId, staffUser, req);
    }

    public List<StaffOrderDto> getPendingStockOuts() {
        return stockOutService.getPendingStockOuts();
    }

    public void confirmStockOut(Integer soId, User staffUser, StaffCheckRequest req) {
        stockOutService.confirmStockOut(soId, staffUser, req);
    }

    public void rejectStockOut(Integer soId, User staffUser, StaffRejectRequest req) {
        stockOutService.rejectStockOut(soId, staffUser, req);
    }

    public List<StaffTransferDto> getPendingTransfers() {
        return transferService.getPendingTransfers();
    }

    public void verifyTransfer(Integer movementId, User staffUser, StaffCheckRequest req) {
        transferService.verifyTransfer(movementId, staffUser, req);
    }

    public void rejectTransfer(Integer movementId, User staffUser, StaffRejectRequest req) {
        transferService.rejectTransfer(movementId, staffUser, req);
    }

    // ==========================================
    // DASHBOARD & ACTIVITY SUMMARY
    // ==========================================

    @Transactional(readOnly = true)
    public StaffDashboardDto getDashboardSummary() {
        long receiptCount = receivingCheckRepository.findAll().stream()
                .filter(r -> r.getResult() == ReceivingCheckResult.ACCEPTED).count();
        long stockOutCount = stockOutCheckRepository.findAll().stream()
                .filter(s -> s.getResult() == StockOutCheckResult.STOCK_OUT).count();
        long transferCount = stockMovementRepository.findAll().stream()
                .filter(m -> m.getStatus() == MovementStatus.COMPLETED).count();
        long totalCount = receiptCount + stockOutCount + transferCount;

        List<StaffActivityItemDto> activities = getRecentStaffActivities(15);

        return StaffDashboardDto.builder()
                .receiptCount(receiptCount)
                .stockOutCount(stockOutCount)
                .transferCount(transferCount)
                .totalCount(totalCount)
                .recentActivities(activities)
                .build();
    }

    private List<StaffActivityItemDto> getRecentStaffActivities(int limit) {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByActionDatetimeDesc();
        List<StaffActivityItemDto> result = new ArrayList<>();

        for (AuditLog al : logs) {
            if (result.size() >= limit) break;
            String action = al.getActionType();
            if (action == null) continue;

            String operation = null;
            String status = "Completed";

            if (action.startsWith("RECEIVE_")) {
                operation = "Receive";
                if (action.endsWith("REJECT")) status = "Rejected";
            } else if (action.startsWith("STOCK_OUT_")) {
                operation = "Stock Out";
                if (action.endsWith("REJECT")) status = "Rejected";
            } else if (action.startsWith("TRANSFER_")) {
                operation = "Transfer";
                if (action.endsWith("REJECT")) status = "Rejected";
            }

            if (operation != null) {
                result.add(StaffActivityItemDto.builder()
                        .date(al.getActionDatetime() != null ? al.getActionDatetime().format(DATETIME_FORMATTER) : "—")
                        .operation(operation)
                        .product(al.getDetail() != null ? al.getDetail() : operation)
                        .quantity(1)
                        .status(status)
                        .reference(action)
                        .build());
            }
        }
        return result;
    }
}
