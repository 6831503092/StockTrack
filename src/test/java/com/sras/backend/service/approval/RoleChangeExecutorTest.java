package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.User;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.Role;
import com.sras.backend.exception.NotFoundException;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleChangeExecutorTest {

    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RoleChangeExecutor executor;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        executor = new RoleChangeExecutor(userRepository, objectMapper);

        sampleUser = new User();
        sampleUser.setUserId(503001);
        sampleUser.setFullName("Somchai");
        sampleUser.setRole(Role.STAFF);
        sampleUser.setIsActive(true);
    }

    @Test
    @DisplayName("supports: returns ROLE_CHANGE")
    void testSupports() {
        assertEquals(RequestType.ROLE_CHANGE, executor.supports());
    }

    @Test
    @DisplayName("execute: successfully changes user role")
    void testExecute_Success() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(50)
                .requestType(RequestType.ROLE_CHANGE)
                .targetId(503001)
                .payload("{\"userId\":503001,\"newRole\":\"MANAGER\"}")
                .build();

        when(userRepository.findByUserIdAndIsActiveTrue(503001)).thenReturn(Optional.of(sampleUser));

        executor.execute(request);

        assertEquals(Role.MANAGER, sampleUser.getRole());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("execute: throws NotFoundException when active user is not found")
    void testExecute_UserNotFound() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(51)
                .requestType(RequestType.ROLE_CHANGE)
                .targetId(999999)
                .payload("{\"userId\":999999,\"newRole\":\"MANAGER\"}")
                .build();

        when(userRepository.findByUserIdAndIsActiveTrue(999999)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> executor.execute(request));
    }

    @Test
    @DisplayName("execute: throws ValidationException when newRole is invalid")
    void testExecute_InvalidRole() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(52)
                .requestType(RequestType.ROLE_CHANGE)
                .targetId(503001)
                .payload("{\"userId\":503001,\"newRole\":\"SUPER_HERO\"}")
                .build();

        when(userRepository.findByUserIdAndIsActiveTrue(503001)).thenReturn(Optional.of(sampleUser));

        assertThrows(ValidationException.class, () -> executor.execute(request));
    }
}
