package com.sras.backend.service;

import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.entity.User;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.PurchaseOrderStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.Role;
import com.sras.backend.enums.TargetType;
import com.sras.backend.repository.ApprovalRequestRepository;
import com.sras.backend.repository.PurchaseOrderRepository;
import com.sras.backend.repository.SalesOrderRepository;
import com.sras.backend.service.approval.ApprovalDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalRequestServiceTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private ApprovalDispatcher approvalDispatcher;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @InjectMocks
    private ApprovalRequestService approvalRequestService;

    private User adminUser;
    private User managerUser;
    private ApprovalRequest sampleRequest;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUserId(503092);
        adminUser.setFullName("Admin User");
        adminUser.setRole(Role.ADMIN);

        managerUser = new User();
        managerUser.setUserId(503069);
        managerUser.setFullName("Manager User");
        managerUser.setRole(Role.MANAGER);

        sampleRequest = ApprovalRequest.builder()
                .requestId(1)
                .requestType(RequestType.USER_CREATE)
                .targetType(TargetType.USER)
                .targetId(503199)
                .detail("Create new user: Ployphan (ID: 503199) with role STAFF")
                .payload("{\"userId\":503199,\"passwordHash\":\"$2a$10$abcdef\"}")
                .status(ApprovalStatus.PENDING)
                .requestedBy(adminUser)
                .requestedDatetime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getMyRequests: returns list of requests and sanitizes payload credentials")
    void testGetMyRequests_Success() {
        when(approvalRequestRepository.findByRequestedByUserIdOrderByRequestedDatetimeDesc(adminUser.getUserId()))
                .thenReturn(List.of(sampleRequest));

        List<ApprovalRequestDto> result = approvalRequestService.getMyRequests(adminUser, null, null, "ALL");

        assertNotNull(result);
        assertEquals(1, result.size());
        ApprovalRequestDto dto = result.get(0);
        assertEquals(1, dto.getRequestId());
        assertEquals("USER_CREATE", dto.getRequestType());
        assertEquals("PENDING", dto.getStatus());
        assertEquals("Admin User", dto.getRequestedByName());
        assertEquals("ADMIN", dto.getRole());
        assertFalse(dto.getPayload().contains("$2a$10$abcdef"));
        assertEquals("{\"userId\":503199,\"passwordHash\":\"[PROTECTED]\"}", dto.getPayload());
    }

    @Test
    @DisplayName("getAllRequests: returns list of all requests for Manager")
    void testGetAllRequests_Success() {
        when(approvalRequestRepository.findAllByOrderByRequestedDatetimeDesc())
                .thenReturn(List.of(sampleRequest));

        List<ApprovalRequestDto> result = approvalRequestService.getAllRequests(null, null, "ALL");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getRequestId());
    }

    @Test
    @DisplayName("getRequestKpis: returns correct aggregate counts")
    void testGetRequestKpis() {
        ApprovalRequest approvedReq = ApprovalRequest.builder()
                .requestId(2)
                .status(ApprovalStatus.APPROVED)
                .build();

        when(approvalRequestRepository.findAll()).thenReturn(List.of(sampleRequest, approvedReq));

        Map<String, Long> kpis = approvalRequestService.getRequestKpis();

        assertEquals(2L, kpis.get("total"));
        assertEquals(1L, kpis.get("pending"));
        assertEquals(1L, kpis.get("approved"));
        assertEquals(0L, kpis.get("rejected"));
    }

    @Test
    @DisplayName("approveRequest: executes dispatcher and updates status to APPROVED")
    void testApproveRequest_Success() {
        when(approvalRequestRepository.findById(1)).thenReturn(Optional.of(sampleRequest));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalRequestDto result = approvalRequestService.approveRequest(1, managerUser, "Approved by Manager");

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        assertEquals("Manager User", result.getDecidedByName());
        assertEquals("Approved by Manager", result.getDecisionReason());
        verify(approvalDispatcher).dispatch(sampleRequest);
    }

    @Test
    @DisplayName("rejectRequest: sets status to REJECTED with mandatory reason")
    void testRejectRequest_Success() {
        ApprovalRequest poRequest = ApprovalRequest.builder()
                .requestId(2)
                .requestType(RequestType.PO_CREATE)
                .targetType(TargetType.PRODUCT)
                .targetId(101)
                .status(ApprovalStatus.PENDING)
                .requestedBy(adminUser)
                .requestedDatetime(LocalDateTime.now())
                .build();

        PurchaseOrder po = new PurchaseOrder();
        po.setPoId(101);
        po.setStatus(PurchaseOrderStatus.PENDING);

        when(approvalRequestRepository.findById(2)).thenReturn(Optional.of(poRequest));
        when(purchaseOrderRepository.findById(101)).thenReturn(Optional.of(po));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalRequestDto result = approvalRequestService.rejectRequest(2, managerUser, "Exceeds monthly budget");

        assertNotNull(result);
        assertEquals("REJECTED", result.getStatus());
        assertEquals("Exceeds monthly budget", result.getDecisionReason());
        assertEquals(PurchaseOrderStatus.REJECTED, po.getStatus());
    }

    @Test
    @DisplayName("getRequestById: throws 404 when request is not found")
    void testGetRequestById_NotFound() {
        when(approvalRequestRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> approvalRequestService.getRequestById(999, adminUser));
    }
}
