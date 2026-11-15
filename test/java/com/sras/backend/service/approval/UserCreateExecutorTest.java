package com.sras.backend.service.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.User;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.Role;
import com.sras.backend.exception.ValidationException;
import com.sras.backend.repository.UserRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCreateExecutorTest {

    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserCreateExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new UserCreateExecutor(userRepository, objectMapper);
    }

    @Test
    @DisplayName("supports: returns USER_CREATE")
    void testSupports() {
        assertEquals(RequestType.USER_CREATE, executor.supports());
    }

    @Test
    @DisplayName("execute: creates new active user with specified role")
    void testExecute_Success() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(60)
                .requestType(RequestType.USER_CREATE)
                .targetId(503100)
                .payload("{\"userId\":503100,\"fullName\":\"Nong New\",\"role\":\"STAFF\",\"passwordHash\":\"$2a$10$hash\"}")
                .build();

        when(userRepository.findById(503100)).thenReturn(Optional.empty());

        executor.execute(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals(503100, saved.getUserId());
        assertEquals("Nong New", saved.getFullName());
        assertEquals(Role.STAFF, saved.getRole());
        assertTrue(saved.getIsActive());
    }

    @Test
    @DisplayName("execute: throws ValidationException when userId <= 0")
    void testExecute_InvalidUserId() {
        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(61)
                .requestType(RequestType.USER_CREATE)
                .payload("{\"userId\":0,\"fullName\":\"Nobody\"}")
                .build();

        assertThrows(ValidationException.class, () -> executor.execute(request));
    }
}
