package com.sras.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.dto.SupplierDto;
import com.sras.backend.dto.SupplierRequest;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Supplier;
import com.sras.backend.entity.User;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.TargetType;
import com.sras.backend.repository.ApprovalRequestRepository;
import com.sras.backend.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalRequestFactory approvalRequestFactory;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<SupplierDto> getActiveSuppliers() {
        return supplierRepository.findByIsActiveTrue()
                .stream()
                .map(SupplierDto::fromEntity)
                .toList();
    }

    @Transactional
    public SupplierDto createSupplier(SupplierRequest req, User actor) {
        if (req == null || req.companyName() == null || req.companyName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name is required");
        }
        String name = req.companyName().trim();
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name cannot exceed 50 characters");
        }
        if (supplierRepository.findByCompanyName(name).filter(s -> Boolean.TRUE.equals(s.getIsActive())).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active supplier with this name already exists");
        }
        ensureCategoryNotDuplicate(req.category(), null);

        Supplier supplier = supplierRepository.findByCompanyName(name).orElseGet(Supplier::new);
        supplier.setCompanyName(name);
        supplier.setZoneCode(req.zoneCode() != null && !req.zoneCode().trim().isEmpty() ? req.zoneCode().trim().toUpperCase() : null);
        supplier.setCategory(req.category() != null && !req.category().trim().isEmpty() ? req.category().trim() : null);
        supplier.setIsActive(true);

        Supplier saved = supplierRepository.save(supplier);
        auditLogService.log(actor, com.sras.backend.enums.AuditAction.SUPPLIER_CREATE, "SUPPLIER", saved.getSupplierId(),
                "Created supplier #" + saved.getSupplierId() + " (" + saved.getCompanyName() + ")");
        return SupplierDto.fromEntity(saved);
    }

    @Transactional
    public SupplierDto updateSupplier(Integer id, SupplierRequest req, User actor) {
        if (req == null || req.companyName() == null || req.companyName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name is required");
        }
        String name = req.companyName().trim();
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name cannot exceed 50 characters");
        }

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found with ID: " + id));

        supplierRepository.findByCompanyName(name).ifPresent(existing -> {
            if (!existing.getSupplierId().equals(id) && Boolean.TRUE.equals(existing.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another active supplier already exists with name: " + name);
            }
        });
        ensureCategoryNotDuplicate(req.category(), id);

        supplier.setCompanyName(name);
        if (req.zoneCode() != null) {
            supplier.setZoneCode(req.zoneCode().trim().isEmpty() ? null : req.zoneCode().trim().toUpperCase());
        }
        if (req.category() != null) {
            supplier.setCategory(req.category().trim().isEmpty() ? null : req.category().trim());
        }
        Supplier saved = supplierRepository.save(supplier);
        auditLogService.log(actor, com.sras.backend.enums.AuditAction.SUPPLIER_UPDATE, "SUPPLIER", saved.getSupplierId(),
                "Updated supplier #" + saved.getSupplierId() + " (" + saved.getCompanyName() + ")");
        return SupplierDto.fromEntity(saved);
    }

    @Transactional
    public void deleteSupplier(Integer id, User actor) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found with ID: " + id));

        supplier.setIsActive(false);
        supplierRepository.save(supplier);
        auditLogService.log(actor, com.sras.backend.enums.AuditAction.SUPPLIER_DELETE, "SUPPLIER", supplier.getSupplierId(),
                "Deactivated supplier #" + supplier.getSupplierId() + " (" + supplier.getCompanyName() + ")");
    }

    @Transactional
    public ApprovalRequestDto requestSupplierCreate(SupplierRequest req, User actor) {
        if (req == null || req.companyName() == null || req.companyName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name is required");
        }
        String name = req.companyName().trim();
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name cannot exceed 50 characters");
        }
        if (supplierRepository.findByCompanyName(name).filter(s -> Boolean.TRUE.equals(s.getIsActive())).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active supplier with this name already exists");
        }
        ensureCategoryNotDuplicate(req.category(), null);

        String zoneCode = req.zoneCode() != null && !req.zoneCode().trim().isEmpty() ? req.zoneCode().trim().toUpperCase() : "";
        String category = req.category() != null && !req.category().trim().isEmpty() ? req.category().trim() : "";

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("operation", "CREATE");
        payloadMap.put("companyName", name);
        payloadMap.put("category", category);
        payloadMap.put("zoneCode", zoneCode);
        payloadMap.put("reason", "Request to add new supplier: " + name +
                " (Category: " + (category.isEmpty() ? "-" : category) +
                ", Zone: " + (zoneCode.isEmpty() ? "None" : zoneCode) + ")");

        String payloadJson = serializeSupplierPayload(payloadMap, name);
        String detail = "Request to add new supplier: " + name +
                " (Category: " + (category.isEmpty() ? "-" : category) +
                ", Zone: " + (zoneCode.isEmpty() ? "None" : zoneCode) + ")";

        ApprovalRequest saved = approvalRequestFactory.create(
                RequestType.SUPPLIER_CREATE, TargetType.SUPPLIER, null, payloadJson, detail, actor
        );

        auditLogService.log(actor, com.sras.backend.enums.AuditAction.REQUEST_SUBMIT, "SUPPLIER", saved.getRequestId(),
                "Submitted request to create supplier: " + name);
        return ApprovalRequestDto.fromEntity(saved);
    }

    @Transactional
    public ApprovalRequestDto requestSupplierUpdate(Integer id, SupplierRequest req, User actor) {
        if (req == null || req.companyName() == null || req.companyName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name is required");
        }
        String name = req.companyName().trim();
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier company name cannot exceed 50 characters");
        }

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found with ID: " + id));

        supplierRepository.findByCompanyName(name).ifPresent(existing -> {
            if (!existing.getSupplierId().equals(id) && Boolean.TRUE.equals(existing.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another active supplier already exists with name: " + name);
            }
        });

        if (approvalRequestRepository.existsByRequestTypeAndTargetIdAndStatus(
                RequestType.SUPPLIER_UPDATE, id, ApprovalStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending update request already exists for this supplier");
        }
        ensureCategoryNotDuplicate(req.category(), id);

        String zoneCode = req.zoneCode() != null && !req.zoneCode().trim().isEmpty() ? req.zoneCode().trim().toUpperCase() : "";
        String category = req.category() != null && !req.category().trim().isEmpty() ? req.category().trim() : "";

        String oldName = supplier.getCompanyName() != null ? supplier.getCompanyName().trim() : "";
        String oldCat = supplier.getCategory() != null ? supplier.getCategory().trim() : "";
        String oldZone = supplier.getZoneCode() != null ? supplier.getZoneCode().trim().toUpperCase() : "";
        if (name.equals(oldName) && category.equals(oldCat) && zoneCode.equals(oldZone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No changes detected for this supplier");
        }

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("operation", "UPDATE");
        payloadMap.put("supplierId", supplier.getSupplierId());
        payloadMap.put("companyName", name);
        payloadMap.put("oldCompanyName", supplier.getCompanyName());
        payloadMap.put("category", category);
        payloadMap.put("oldCategory", supplier.getCategory() != null ? supplier.getCategory() : "");
        payloadMap.put("zoneCode", zoneCode);
        payloadMap.put("oldZoneCode", supplier.getZoneCode() != null ? supplier.getZoneCode() : "");
        payloadMap.put("reason", "Request to update supplier #" + supplier.getSupplierId() +
                " (" + supplier.getCompanyName() + " -> " + name + ")");

        String payloadJson = serializeSupplierPayload(payloadMap, name);
        String detail = "Request to edit supplier #" + supplier.getSupplierId() + ": " +
                supplier.getCompanyName() + " -> " + name +
                " (Category: " + (category.isEmpty() ? "-" : category) +
                ", Zone: " + (zoneCode.isEmpty() ? "None" : zoneCode) + ")";

        ApprovalRequest saved = approvalRequestFactory.create(
                RequestType.SUPPLIER_UPDATE, TargetType.SUPPLIER, supplier.getSupplierId(), payloadJson, detail, actor
        );

        auditLogService.log(actor, com.sras.backend.enums.AuditAction.REQUEST_SUBMIT, "SUPPLIER", saved.getRequestId(),
                "Submitted request to update supplier #" + supplier.getSupplierId() + " (" + name + ")");
        return ApprovalRequestDto.fromEntity(saved);
    }

    @Transactional
    public ApprovalRequestDto requestSupplierDeactivate(Integer id, User actor) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found with ID: " + id));

        if (approvalRequestRepository.existsByRequestTypeAndTargetIdAndStatus(
                RequestType.SUPPLIER_DEACTIVATE, id, ApprovalStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending deactivation request already exists for this supplier");
        }

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("operation", "DELETE");
        payloadMap.put("supplierId", supplier.getSupplierId());
        payloadMap.put("companyName", supplier.getCompanyName());
        payloadMap.put("category", supplier.getCategory() != null ? supplier.getCategory() : "");
        payloadMap.put("zoneCode", supplier.getZoneCode() != null ? supplier.getZoneCode() : "");
        payloadMap.put("reason", "Request to deactivate supplier #" + supplier.getSupplierId() + " (" + supplier.getCompanyName() + ")");

        String payloadJson = serializeSupplierPayload(payloadMap, supplier.getCompanyName());
        String detail = "Request to delete supplier #" + supplier.getSupplierId() + ": " + supplier.getCompanyName() +
                " (Category: " + (supplier.getCategory() != null ? supplier.getCategory() : "-") +
                ", Zone: " + (supplier.getZoneCode() != null ? supplier.getZoneCode() : "None") + ")";

        ApprovalRequest saved = approvalRequestFactory.create(
                RequestType.SUPPLIER_DEACTIVATE, TargetType.SUPPLIER, supplier.getSupplierId(), payloadJson, detail, actor
        );

        auditLogService.log(actor, com.sras.backend.enums.AuditAction.REQUEST_SUBMIT, "SUPPLIER", saved.getRequestId(),
                "Submitted request to deactivate supplier #" + supplier.getSupplierId() + " (" + supplier.getCompanyName() + ")");
        return ApprovalRequestDto.fromEntity(saved);
    }

    private void ensureCategoryNotDuplicate(String category, Integer excludeSupplierId) {
        if (category == null || category.trim().isEmpty()) {
            return;
        }
        String normalized = category.trim();
        boolean existsInSuppliers = supplierRepository.findByIsActiveTrue().stream()
                .filter(s -> excludeSupplierId == null || !s.getSupplierId().equals(excludeSupplierId))
                .anyMatch(s -> s.getCategory() != null && s.getCategory().trim().equalsIgnoreCase(normalized));
        if (existsInSuppliers) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Products \"" + normalized + "\" already exists and cannot be created");
        }

        boolean existsInPendingCreates = approvalRequestRepository
                .findByRequestTypeAndStatus(RequestType.SUPPLIER_CREATE, ApprovalStatus.PENDING)
                .stream()
                .anyMatch(r -> {
                    if (r.getPayload() == null || r.getPayload().isBlank()) return false;
                    try {
                        String pendingCat = objectMapper.readTree(r.getPayload()).path("category").asText("").trim();
                        return !pendingCat.isEmpty() && pendingCat.equalsIgnoreCase(normalized);
                    } catch (Exception e) {
                        return false;
                    }
                });
        if (existsInPendingCreates) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Products \"" + normalized + "\" already exists in a pending request and cannot be created");
        }
    }

    private String serializeSupplierPayload(Map<String, Object> payloadMap, String fallbackName) {
        try {
            return objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            return "{\"companyName\":\"" + fallbackName + "\"}";
        }
    }
}
