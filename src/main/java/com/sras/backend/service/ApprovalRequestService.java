package com.sras.backend.service;

import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.User;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.Role;
import com.sras.backend.enums.TargetType;
import com.sras.backend.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalRequestService {

    private static final DateTimeFormatter SLASH_DATE_FMT = DateTimeFormatter.ofPattern("d/M/yyyy");

    private final ApprovalRequestRepository approvalRequestRepository;

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
        boolean isPrivileged = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER;

        if (!isRequester && !isPrivileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this request");
        }

        return ApprovalRequestDto.fromEntity(request);
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

        return idStr.contains(q) || typeStr.contains(q) || detailStr.contains(q) || targetTypeStr.contains(q);
    }
}
