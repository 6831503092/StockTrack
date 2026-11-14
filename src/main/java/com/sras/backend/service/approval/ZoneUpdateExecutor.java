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
public class ZoneUpdateExecutor implements ApprovalExecutor {

    private final ZoneRepository zoneRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RequestType supports() {
        return RequestType.ZONE_UPDATE;
    }

    @Override
    public void execute(ApprovalRequest request) {
        String payload = request.getPayload();
        if (payload == null || payload.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing payload for ZONE_UPDATE");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            Integer targetId = request.getTargetId();
            if (targetId == null && root.hasNonNull("zoneId")) {
                targetId = root.get("zoneId").asInt();
            }

            String zoneCode = root.hasNonNull("zoneCode") ? root.get("zoneCode").asText().trim().toUpperCase() : "";

            Zone zone = null;
            if (targetId != null) {
                zone = zoneRepository.findById(targetId).orElse(null);
            }
            if (zone == null && !zoneCode.isEmpty()) {
                zone = zoneRepository.findByZoneCode(zoneCode).orElse(null);
            }
            if (zone == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Target zone not found for ZONE_UPDATE");
            }

            if (root.hasNonNull("zoneName")) {
                String newName = root.get("zoneName").asText().trim();
                if (!newName.isEmpty()) {
                    zone.setZoneName(newName);
                }
            }

            if (root.hasNonNull("capacity")) {
                int newCap = root.get("capacity").asInt();
                if (newCap > 0) {
                    zone.setCapacity(newCap);
                }
            }

            zoneRepository.save(zone);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse ZONE_UPDATE payload: " + e.getMessage());
        }
    }
}
