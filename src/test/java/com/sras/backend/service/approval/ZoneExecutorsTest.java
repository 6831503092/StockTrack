package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.RequestType;
import com.sras.backend.exception.ConflictException;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneExecutorsTest {

    @Mock
    private ZoneRepository zoneRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ZoneCreateExecutor createExecutor;
    private ZoneUpdateExecutor updateExecutor;

    @BeforeEach
    void setUp() {
        createExecutor = new ZoneCreateExecutor(zoneRepository, objectMapper);
        updateExecutor = new ZoneUpdateExecutor(zoneRepository, objectMapper);
    }

    @Test
    @DisplayName("ZONE_CREATE: creates new zone successfully")
    void testZoneCreate_Success() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(70)
                .requestType(RequestType.ZONE_CREATE)
                .payload("{\"zoneCode\":\"Z99\",\"zoneName\":\"Zone Premium\",\"capacity\":800}")
                .build();

        when(zoneRepository.existsByZoneCode("Z99")).thenReturn(false);
        when(zoneRepository.existsByZoneName("Zone Premium")).thenReturn(false);
        when(zoneRepository.save(any(Zone.class))).thenAnswer(inv -> {
            Zone z = inv.getArgument(0);
            z.setZoneId(99);
            return z;
        });

        createExecutor.execute(request);

        assertEquals(99, request.getTargetId());
        ArgumentCaptor<Zone> captor = ArgumentCaptor.forClass(Zone.class);
        verify(zoneRepository).save(captor.capture());
        assertEquals("Z99", captor.getValue().getZoneCode());
        assertEquals(800, captor.getValue().getCapacity());
    }

    @Test
    @DisplayName("ZONE_CREATE: throws ConflictException when code already exists")
    void testZoneCreate_CodeConflict() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(71)
                .requestType(RequestType.ZONE_CREATE)
                .payload("{\"zoneCode\":\"Z01\",\"zoneName\":\"Zone One\"}")
                .build();

        when(zoneRepository.existsByZoneCode("Z01")).thenReturn(true);

        assertThrows(ConflictException.class, () -> createExecutor.execute(request));
    }

    @Test
    @DisplayName("ZONE_CREATE: throws ValidationException when zoneCode is blank")
    void testZoneCreate_BlankCode() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(72)
                .requestType(RequestType.ZONE_CREATE)
                .payload("{\"zoneCode\":\"\",\"zoneName\":\"Zone\"}")
                .build();

        assertThrows(ValidationException.class, () -> createExecutor.execute(request));
    }

    @Test
    @DisplayName("ZONE_UPDATE: updates zone name and capacity")
    void testZoneUpdate_Success() {
        Zone existingZone = new Zone();
        existingZone.setZoneId(1);
        existingZone.setZoneCode("Z01");
        existingZone.setZoneName("Old Name");
        existingZone.setCapacity(500);

        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(73)
                .requestType(RequestType.ZONE_UPDATE)
                .targetId(1)
                .payload("{\"zoneId\":1,\"zoneName\":\"New Name\",\"capacity\":1000}")
                .build();

        when(zoneRepository.findById(1)).thenReturn(Optional.of(existingZone));

        updateExecutor.execute(request);

        assertEquals("New Name", existingZone.getZoneName());
        assertEquals(1000, existingZone.getCapacity());
        verify(zoneRepository).save(existingZone);
    }

    @Test
    @DisplayName("ZONE_UPDATE: throws NotFoundException when zone does not exist")
    void testZoneUpdate_NotFound() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(74)
                .requestType(RequestType.ZONE_UPDATE)
                .targetId(999)
                .payload("{\"zoneId\":999,\"zoneName\":\"Ghost\"}")
                .build();

        when(zoneRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> updateExecutor.execute(request));
    }
}
