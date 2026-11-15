package com.sras.backend.service;

import com.sras.backend.dto.StaffCheckRequest;
import com.sras.backend.dto.StaffRejectRequest;
import com.sras.backend.dto.StaffTransferDto;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.StockMovement;
import com.sras.backend.entity.User;
import com.sras.backend.enums.MovementStatus;
import com.sras.backend.exception.BusinessRuleException;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;
    private final AuditLogService auditLogService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    @Transactional(readOnly = true)
    public List<StaffTransferDto> getPendingTransfers() {
        List<StockMovement> movements = stockMovementRepository.findByStatus(MovementStatus.PENDING_VERIFICATION);
        return movements.stream().map(this::mapMovementToDto).collect(Collectors.toList());
    }

    @Transactional
    public void verifyTransfer(Integer movementId, User staffUser, StaffCheckRequest req) {
        StockMovement movement = stockMovementRepository.findById(movementId)
                .orElseThrow(() -> new NotFoundException("Stock movement not found: " + movementId));

        if (movement.getStatus() != MovementStatus.PENDING_VERIFICATION) {
            throw new BusinessRuleException("Stock movement is not pending verification (status=" + movement.getStatus() + ")");
        }

        int counted = movement.getQuantity();
        if (req != null && req.getItems() != null && !req.getItems().isEmpty() && req.getItems().get(0).getCountedQty() != null) {
            counted = req.getItems().get(0).getCountedQty();
        }

        movement.setStatus(MovementStatus.COMPLETED);
        movement.setVerifiedBy(staffUser);
        movement.setVerifiedDatetime(LocalDateTime.now());
        movement.setCountedQty(counted);
        stockMovementRepository.save(movement);

        // Move or swap products between zones
        if (movement.getFromZone() != null && movement.getToZone() != null) {
            stockMovementService.swapZones(movement.getFromZone(), movement.getToZone());
        } else if (movement.getProduct() != null && movement.getToZone() != null) {
            Product product = movement.getProduct();
            product.setZone(movement.getToZone());
            productRepository.save(product);
        }

        auditLogService.log(staffUser, com.sras.backend.enums.AuditAction.TRANSFER_VERIFY, "MOVEMENT", movementId,
                "Verified stock transfer #" + movementId +
                        (movement.getFromZone() != null && movement.getToZone() != null
                                ? " between " + movement.getFromZone().getZoneName() + " and " + movement.getToZone().getZoneName()
                                : ""));
        log.info("Stock movement #{} verified by staff #{}", movementId, staffUser.getUserId());
    }

    @Transactional
    public void rejectTransfer(Integer movementId, User staffUser, StaffRejectRequest req) {
        StockMovement movement = stockMovementRepository.findById(movementId)
                .orElseThrow(() -> new NotFoundException("Stock movement not found: " + movementId));

        if (movement.getStatus() != MovementStatus.PENDING_VERIFICATION) {
            throw new BusinessRuleException("Stock movement is not pending verification (status=" + movement.getStatus() + ")");
        }

        if (req == null || req.getReason() == null || req.getReason().trim().isBlank()) {
            throw new ValidationException("A reason is required to reject this transfer.");
        }
        String reason = req.getReason().trim();

        movement.setStatus(MovementStatus.REJECTED);
        movement.setVerifiedBy(staffUser);
        movement.setVerifiedDatetime(LocalDateTime.now());
        movement.setRejectReason(reason);
        stockMovementRepository.save(movement);

        auditLogService.log(staffUser, com.sras.backend.enums.AuditAction.TRANSFER_REJECT, "MOVEMENT", movementId,
                "Rejected stock transfer #" + movementId + ". Reason: " + reason);
        log.info("Stock movement #{} rejected by staff #{}: {}", movementId, staffUser.getUserId(), reason);
    }

    public StaffTransferDto mapMovementToDto(StockMovement m) {
        String reqRef = m.getRequest() != null ? "REQ-" + String.format("%04d", m.getRequest().getRequestId()) : "TR-" + m.getMovementId();
        String requestedBy = (m.getRequest() != null && m.getRequest().getRequestedBy() != null)
                ? m.getRequest().getRequestedBy().getFullName()
                : "Admin";
        String date = m.getRequest() != null && m.getRequest().getRequestedDatetime() != null
                ? m.getRequest().getRequestedDatetime().format(DATE_FORMATTER)
                : LocalDateTime.now().format(DATE_FORMATTER);

        return StaffTransferDto.builder()
                .movementId(m.getMovementId())
                .code("TR-" + m.getMovementId())
                .reference(reqRef)
                .requestedBy(requestedBy)
                .date(date)
                .productId(m.getProduct() != null ? m.getProduct().getProductId() : null)
                .productName(m.getProduct() != null ? m.getProduct().getProductName() : "Product")
                .fromZoneId(m.getFromZone() != null ? m.getFromZone().getZoneId() : null)
                .fromZoneName(m.getFromZone() != null ? m.getFromZone().getZoneName() : "Source Zone")
                .toZoneId(m.getToZone() != null ? m.getToZone().getZoneId() : null)
                .toZoneName(m.getToZone() != null ? m.getToZone().getZoneName() : "Target Zone")
                .quantity(m.getQuantity())
                .status(m.getStatus() != null ? m.getStatus().name() : "")
                .build();
    }
}
