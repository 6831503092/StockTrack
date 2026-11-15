package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.dto.payload.ZoneCreatePayload;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.ConflictException;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.ZoneRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ZoneCreateExecutor extends BaseApprovalExecutor<ZoneCreatePayload> {

    private final ZoneRepository zoneRepository;

    public ZoneCreateExecutor(ZoneRepository zoneRepository, ObjectMapper objectMapper) {
        super(objectMapper);
        this.zoneRepository = zoneRepository;
    }

    @Override
    public RequestType supports() {
        return RequestType.ZONE_CREATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        ZoneCreatePayload payload = parsePayload(request, ZoneCreatePayload.class);

        String zoneCode = payload.zoneCode() != null ? payload.zoneCode().trim().toUpperCase() : "";
        if (zoneCode.isEmpty()) {
            throw new ValidationException("Zone code is required in ZONE_CREATE payload");
        }

        String zoneName = payload.zoneName() != null && !payload.zoneName().isBlank()
                ? payload.zoneName().trim()
                : "Zone " + zoneCode.replace("Z", "");

        int capacity = payload.capacity() != null && payload.capacity() > 0 ? payload.capacity() : 500;

        if (zoneRepository.existsByZoneCode(zoneCode)) {
            throw new ConflictException("Zone code '" + zoneCode + "' already exists");
        }
        if (zoneRepository.existsByZoneName(zoneName)) {
            throw new ConflictException("Zone name '" + zoneName + "' already exists");
        }

        Zone zone = new Zone();
        zone.setZoneCode(zoneCode);
        zone.setZoneName(zoneName);
        zone.setCapacity(capacity);

        Zone saved = zoneRepository.save(zone);
        request.setTargetId(saved.getZoneId());
        log.info("ZONE_CREATE executed: created zone #{} ({})", saved.getZoneId(), saved.getZoneCode());
    }
}
