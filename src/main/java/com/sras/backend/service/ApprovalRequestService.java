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
import com.sras.backend.repository.ApprovalRequestRepository;
import com.sras.backend.repository.PurchaseOrderRepository;
import com.sras.backend.repository.SalesOrderRepository;
import com.sras.backend.service.approval.ApprovalDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalRequestService {

    private static final DateTimeFormatter SLASH_DATE_FMT = DateTimeFormatter.ofPattern("d/M/yyyy");

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalDispatcher approvalDispatcher;
    private final AuditLogService auditLogService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;

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

    public ApprovalRequestDto getRequestById(Integer requestId, User currentUser) {
        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found"));

        boolean isRequester = request.getRequestedBy() != null &&
                request.getRequestedBy().getUserId().equals(currentUser.getUserId());
        boolean isPrivileged = currentUser.getRole() == Role.ADMIN ||
                currentUser.getRole() == Role.MANAGER ||
                currentUser.getRole() == Role.OWNER;

        if (!isRequester && !isPrivileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this request");
        }

        return ApprovalRequestDto.fromEntity(request);
    }

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

    @Transactional
    public ApprovalRequestDto approveRequest(Integer requestId, User managerUser, String reason) {
        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found"));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending requests can be approved");
        }

        try {
            // Apply approved changes via dispatching to the matching executor
            approvalDispatcher.dispatch(request);

            request.setStatus(ApprovalStatus.APPROVED);
            request.setDecidedBy(managerUser);
            request.setDecidedDatetime(LocalDateTime.now());
            request.setDecisionReason(reason != null && !reason.isBlank() ? reason.trim() : "Approved by Manager");

            ApprovalRequest saved = approvalRequestRepository.save(request);

            auditLogService.log(managerUser, "REQUEST_APPROVE",
                    "Approved request #" + requestId + " (" + request.getRequestType() + "): " + request.getDetail());

            return ApprovalRequestDto.fromEntity(saved);
        } catch (Exception e) {
            log.error("Failed to approve request #{}", requestId, e);
            request.setStatus(ApprovalStatus.FAILED);
            request.setDecidedBy(managerUser);
            request.setDecidedDatetime(LocalDateTime.now());
            request.setDecisionReason("Failed execution: " + e.getMessage());
            approvalRequestRepository.save(request);

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Approval execution failed: " + e.getMessage());
        }
    }

    @Transactional
    public ApprovalRequestDto rejectRequest(Integer requestId, User managerUser, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision reason is required to reject a request");
        }

        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found"));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending requests can be rejected");
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
        request.setDecidedDatetime(LocalDateTime.now());
        request.setDecisionReason(reason.trim());

        ApprovalRequest saved = approvalRequestRepository.save(request);

        auditLogService.log(managerUser, "REQUEST_REJECT",
                "Rejected request #" + requestId + " (" + request.getRequestType() + "). Reason: " + reason.trim());

        return ApprovalRequestDto.fromEntity(saved);
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
                .requestedDatetime(LocalDateTime.now())
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
