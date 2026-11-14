package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.RequestType;
import com.sras.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class ZoneCreateExecutor implements ApprovalExecutor {

    private final ZoneRepository zoneRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RequestType supports() {
        return RequestType.ZONE_CREATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        String payload = request.getPayload();
        if (payload == null || payload.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing payload for ZONE_CREATE");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String zoneCode = root.hasNonNull("zoneCode") ? root.get("zoneCode").asText().trim().toUpperCase() : "";
            String zoneName = root.hasNonNull("zoneName") ? root.get("zoneName").asText().trim() : "";
            int capacity = root.hasNonNull("capacity") ? root.get("capacity").asInt(500) : 500;

            if (zoneCode.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone code is required in ZONE_CREATE payload");
            }

            if (zoneName.isEmpty()) {
                zoneName = "Zone " + zoneCode.replace("Z", "");
            }

            if (zoneRepository.existsByZoneCode(zoneCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone code '" + zoneCode + "' already exists");
            }
            if (zoneRepository.existsByZoneName(zoneName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone name '" + zoneName + "' already exists");
            }

            Zone zone = new Zone();
            zone.setZoneCode(zoneCode);
            zone.setZoneName(zoneName);
            zone.setCapacity(capacity > 0 ? capacity : 500);

            Zone saved = zoneRepository.save(zone);
            request.setTargetId(saved.getZoneId());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse ZONE_CREATE payload: " + e.getMessage());
        }
    }
}
