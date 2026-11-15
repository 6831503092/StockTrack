package com.sras.backend.service;

import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.User;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.PurchaseOrderStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.Role;
import com.sras.backend.enums.SalesOrderStatus;
import com.sras.backend.enums.TargetType;
import com.sras.backend.exception.BusinessRuleException;
import com.sras.backend.exception.ForbiddenException;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.ApprovalRequestRepository;
import com.sras.backend.repository.PurchaseOrderRepository;
import com.sras.backend.repository.SalesOrderRepository;
import com.sras.backend.service.approval.ApprovalDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ApprovalRequestService {

    public static final ZoneId BANGKOK_ZONE = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter SLASH_DATE_FMT = DateTimeFormatter.ofPattern("d/M/yyyy");

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalDispatcher approvalDispatcher;
    private final AuditLogService auditLogService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public ApprovalRequestService(
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalDispatcher approvalDispatcher,
            AuditLogService auditLogService,
            PurchaseOrderRepository purchaseOrderRepository,
            SalesOrderRepository salesOrderRepository,
            PlatformTransactionManager transactionManager
    ) {
        this(
                approvalRequestRepository,
                approvalDispatcher,
                auditLogService,
                purchaseOrderRepository,
                salesOrderRepository,
                createTransactionTemplate(transactionManager)
        );
    }

    private static TransactionTemplate createTransactionTemplate(PlatformTransactionManager transactionManager) {
        if (transactionManager == null) {
            return null;
        }
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    public ApprovalRequestService(
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalDispatcher approvalDispatcher,
            AuditLogService auditLogService,
            PurchaseOrderRepository purchaseOrderRepository,
            SalesOrderRepository salesOrderRepository,
            TransactionTemplate transactionTemplate
    ) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.approvalDispatcher = approvalDispatcher;
        this.auditLogService = auditLogService;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestDto> getAllRequests(String search, String dateStr, String statusStr) {
        return approvalRequestRepository
                .findAllByOrderByRequestedDatetimeDesc()
                .stream()
                .filter(r -> matchesStatusFilter(r.getStatus() != null ? r.getStatus().name() : null, statusStr))
                .filter(r -> matchesSearch(r, search))
                .filter(r -> matchesDateFilter(r.getRequestedDatetime(), dateStr))
                .map(ApprovalRequestDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestDto> getMyRequests(User currentUser, String search, String dateStr, String statusStr) {
        return approvalRequestRepository
                .findByRequestedByUserIdOrderByRequestedDatetimeDesc(currentUser.getUserId())
                .stream()
                .filter(r -> matchesStatusFilter(r.getStatus() != null ? r.getStatus().name() : null, statusStr))
                .filter(r -> matchesSearch(r, search))
                .filter(r -> matchesDateFilter(r.getRequestedDatetime(), dateStr))
                .map(ApprovalRequestDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApprovalRequestDto getRequestById(Integer requestId, User currentUser) {
        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Approval request not found"));

        boolean isRequester = request.getRequestedBy() != null &&
                request.getRequestedBy().getUserId().equals(currentUser.getUserId());
        boolean isPrivileged = currentUser.getRole() == Role.ADMIN ||
                currentUser.getRole() == Role.MANAGER ||
                currentUser.getRole() == Role.OWNER;

        if (!isRequester && !isPrivileged) {
            throw new ForbiddenException("Access denied to this request");
        }

        return ApprovalRequestDto.fromEntity(request);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getRequestKpis() {
        List<ApprovalRequest> all = approvalRequestRepository.findAll();
        long total = all.size();
        long pending = all.stream().filter(r -> r.getStatus() == ApprovalStatus.PENDING).count();
        long approved = all.stream().filter(r -> r.getStatus() == ApprovalStatus.APPROVED).count();
        long rejected = all.stream().filter(r -> r.getStatus() == ApprovalStatus.REJECTED).count();

        Map<String, Long> kpis = new HashMap<>();
        kpis.put("total", total);
        kpis.put("pending", pending);
        kpis.put("approved", approved);
        kpis.put("rejected", rejected);
        return kpis;
    }

    /**
     * Approves request and executes the corresponding approval executor.
     * Approve and Execute run together in the same transaction.
     * If execution fails, partial changes are rolled back, and the request status is saved as FAILED.
     */
    public ApprovalRequestDto approveRequest(Integer requestId, User managerUser, String reason) {
        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Approval request not found"));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new ValidationException("Only pending requests can be approved");
        }

        try {
            return runInTransaction(status -> {
                ApprovalRequest currentReq = approvalRequestRepository.findById(requestId).orElse(request);

                // Apply approved changes via dispatching to the matching executor
                approvalDispatcher.dispatch(currentReq);

                currentReq.setStatus(ApprovalStatus.APPROVED);
                currentReq.setDecidedBy(managerUser);
                currentReq.setDecidedDatetime(LocalDateTime.now(BANGKOK_ZONE));
                currentReq.setDecisionReason(reason != null && !reason.isBlank() ? reason.trim() : "Approved by Manager");

                ApprovalRequest saved = approvalRequestRepository.save(currentReq);
                approvalRequestRepository.flush();

                String targetType = resolveAuditTargetType(currentReq);
                Object targetId = resolveAuditTargetId(currentReq, requestId);

                auditLogService.log(managerUser, com.sras.backend.enums.AuditAction.REQUEST_APPROVE,
                        targetType, targetId,
                        "Approved request #" + requestId + " (" + currentReq.getRequestType() + "): " + currentReq.getDetail());

                return ApprovalRequestDto.fromEntity(saved);
            });
        } catch (Exception e) {
            log.error("Failed to approve request #{}", requestId, e);
            runInTransaction(status -> {
                ApprovalRequest failedReq = approvalRequestRepository.findById(requestId).orElse(request);
                failedReq.setStatus(ApprovalStatus.FAILED);
                failedReq.setDecidedBy(managerUser);
                failedReq.setDecidedDatetime(LocalDateTime.now(BANGKOK_ZONE));
                failedReq.setDecisionReason("Failed execution: " + e.getMessage());
                approvalRequestRepository.save(failedReq);
                approvalRequestRepository.flush();
                return null;
            });

            throw new BusinessRuleException("Approval execution failed: " + e.getMessage());
        }
    }

    @Transactional
    public ApprovalRequestDto rejectRequest(Integer requestId, User managerUser, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Decision reason is required to reject a request");
        }

        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Approval request not found"));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new ValidationException("Only pending requests can be rejected");
        }

        // If this was an order, update order status to REJECTED
        if (request.getRequestType() == RequestType.PO_CREATE && request.getTargetId() != null) {
            purchaseOrderRepository.findById(request.getTargetId()).ifPresent(po -> {
                po.setStatus(PurchaseOrderStatus.REJECTED);
                purchaseOrderRepository.save(po);
            });
        } else if (request.getRequestType() == RequestType.SO_CREATE && request.getTargetId() != null) {
            salesOrderRepository.findById(request.getTargetId()).ifPresent(so -> {
                so.setStatus(SalesOrderStatus.REJECTED);
                salesOrderRepository.save(so);
            });
        }

        request.setStatus(ApprovalStatus.REJECTED);
        request.setDecidedBy(managerUser);
        request.setDecidedDatetime(LocalDateTime.now(BANGKOK_ZONE));
        request.setDecisionReason(reason.trim());

        ApprovalRequest saved = approvalRequestRepository.save(request);
        approvalRequestRepository.flush();

        String targetType = resolveAuditTargetType(request);
        Object targetId = resolveAuditTargetId(request, requestId);

        auditLogService.log(managerUser, com.sras.backend.enums.AuditAction.REQUEST_REJECT,
                targetType, targetId,
                "Rejected request #" + requestId + " (" + request.getRequestType() + "). Reason: " + reason.trim());

        return ApprovalRequestDto.fromEntity(saved);
    }

    private String resolveAuditTargetType(ApprovalRequest req) {
        if (req == null) return "REQUEST";
        if (req.getRequestType() == RequestType.PO_CREATE) return "PO";
        if (req.getRequestType() == RequestType.SO_CREATE) return "SO";
        if (req.getRequestType() == RequestType.PRODUCT_MOVE) return "MOVEMENT";
        return req.getTargetType() != null ? req.getTargetType().name() : "REQUEST";
    }

    private Object resolveAuditTargetId(ApprovalRequest req, Integer fallbackRequestId) {
        if (req != null && req.getTargetId() != null && req.getTargetId() > 0) {
            return req.getTargetId();
        }
        return fallbackRequestId;
    }

    private <T> T runInTransaction(TransactionCallback<T> action) {
        if (transactionTemplate != null) {
            return transactionTemplate.execute(action);
        }
        return action.doInTransaction(null);
    }

    public static ApprovalRequest buildPendingRequest(
            RequestType requestType,
            TargetType targetType,
            Integer targetId,
            String payloadJson,
            String detail,
            User requestedBy
    ) {
        return ApprovalRequest.builder()
                .requestType(requestType)
                .targetType(targetType)
                .targetId(targetId)
                .payload(payloadJson)
                .detail(detail)
                .status(ApprovalStatus.PENDING)
                .requestedBy(requestedBy)
                .requestedDatetime(LocalDateTime.now(BANGKOK_ZONE))
                .build();
    }

    public static boolean matchesStatusFilter(String actualStatus, String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank() || statusFilter.equalsIgnoreCase("ALL")) {
            return true;
        }
        return actualStatus != null && actualStatus.equalsIgnoreCase(statusFilter.trim());
    }

    public static boolean matchesDateFilter(LocalDateTime dateTime, String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return true;
        }
        if (dateTime == null) {
            return false;
        }
        try {
            String trimmed = dateStr.trim();
            LocalDate filterDate = trimmed.contains("/")
                    ? LocalDate.parse(trimmed, SLASH_DATE_FMT)
                    : LocalDate.parse(trimmed);
            return dateTime.toLocalDate().isEqual(filterDate);
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    private boolean matchesSearch(ApprovalRequest request, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String q = search.trim().toLowerCase();
        String idStr = request.getRequestId() != null ? String.valueOf(request.getRequestId()) : "";
        String typeStr = request.getRequestType() != null ? request.getRequestType().name().toLowerCase() : "";
        String detailStr = request.getDetail() != null ? request.getDetail().toLowerCase() : "";
        String targetTypeStr = request.getTargetType() != null ? request.getTargetType().name().toLowerCase() : "";
        String requesterStr = request.getRequestedBy() != null && request.getRequestedBy().getFullName() != null
                ? request.getRequestedBy().getFullName().toLowerCase() : "";

        return idStr.contains(q) || typeStr.contains(q) || detailStr.contains(q) || targetTypeStr.contains(q) || requesterStr.contains(q);
    }
}
