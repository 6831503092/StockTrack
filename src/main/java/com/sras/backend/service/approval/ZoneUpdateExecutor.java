package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.payload.ZoneUpdatePayload;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.repository.ZoneRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ZoneUpdateExecutor extends BaseApprovalExecutor<ZoneUpdatePayload> {

    private final ZoneRepository zoneRepository;

    public ZoneUpdateExecutor(ZoneRepository zoneRepository, ObjectMapper objectMapper) {
        super(objectMapper);
        this.zoneRepository = zoneRepository;
    }

    @Override
    public RequestType supports() {
        return RequestType.ZONE_UPDATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        ZoneUpdatePayload payload = parsePayload(request, ZoneUpdatePayload.class);

        Integer targetId = request.getTargetId();
        if (targetId == null && payload.zoneId() != null) {
            targetId = payload.zoneId();
        }

        String zoneCode = payload.zoneCode() != null ? payload.zoneCode().trim().toUpperCase() : "";

        Zone zone = null;
        if (targetId != null && targetId > 0) {
            zone = zoneRepository.findById(targetId).orElse(null);
        }
        if (zone == null && !zoneCode.isEmpty()) {
            zone = zoneRepository.findByZoneCode(zoneCode).orElse(null);
        }
        if (zone == null) {
            throw new NotFoundException("Target zone not found for ZONE_UPDATE request #" + request.getRequestId());
        }

        if (payload.zoneName() != null && !payload.zoneName().isBlank()) {
            zone.setZoneName(payload.zoneName().trim());
        }

        if (payload.capacity() != null && payload.capacity() > 0) {
            zone.setCapacity(payload.capacity());
        }

        zoneRepository.save(zone);
        log.info("ZONE_UPDATE executed: updated zone #{} ({})", zone.getZoneId(), zone.getZoneCode());
    }
}
