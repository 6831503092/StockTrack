package com.sras.backend.service;

import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.User;
import com.sras.backend.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalRequestService {

    private final ApprovalRequestRepository approvalRequestRepository;

    public List<ApprovalRequestDto> getMyRequests(User currentUser, String search, String dateStr, String statusStr) {
        List<ApprovalRequest> requests = approvalRequestRepository
                .findByRequestedByUserIdOrderByRequestedDatetimeDesc(currentUser.getUserId());

        return requests.stream()
                .filter(r -> matchesStatus(r, statusStr))
                .filter(r -> matchesSearch(r, search))
                .filter(r -> matchesDate(r, dateStr))
                .map(ApprovalRequestDto::fromEntity)
                .collect(Collectors.toList());
    }

    public ApprovalRequestDto getRequestById(Integer requestId, User currentUser) {
        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found"));

        boolean isRequester = request.getRequestedBy() != null &&
                request.getRequestedBy().getUserId().equals(currentUser.getUserId());
        boolean isPrivileged = currentUser.getRole() != null &&
                (currentUser.getRole().name().equals("ADMIN") || currentUser.getRole().name().equals("MANAGER"));

        if (!isRequester && !isPrivileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this request");
        }

        return ApprovalRequestDto.fromEntity(request);
    }

    private boolean matchesStatus(ApprovalRequest request, String statusStr) {
        if (statusStr == null || statusStr.isBlank() || statusStr.equalsIgnoreCase("ALL")) {
            return true;
        }
        return request.getStatus() != null && request.getStatus().name().equalsIgnoreCase(statusStr.trim());
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

    private boolean matchesDate(ApprovalRequest request, String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return true;
        }
        if (request.getRequestedDatetime() == null) {
            return false;
        }
        try {
            LocalDate filterDate = LocalDate.parse(dateStr.trim());
            return request.getRequestedDatetime().toLocalDate().isEqual(filterDate);
        } catch (DateTimeParseException e) {
            return true;
        }
    }
}
