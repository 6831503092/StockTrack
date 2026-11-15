package com.sras.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sras.backend.config.GlobalExceptionHandler;
import com.sras.backend.config.WebMvcConfig;
import com.sras.backend.dto.ApprovalRequestDto;
import com.sras.backend.dto.DecisionRequest;
import com.sras.backend.dto.OwnerDashboardDto;
import com.sras.backend.dto.UserCreateRequest;
import com.sras.backend.dto.ZoneCreateRequest;
import com.sras.backend.dto.ZoneDto;
import com.sras.backend.entity.User;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.RequestType;
import com.sras.backend.enums.Role;
import com.sras.backend.enums.TargetType;
import com.sras.backend.exception.ForbiddenException;
import com.sras.backend.security.CurrentUserArgumentResolver;
import com.sras.backend.security.RequireRolesInterceptor;
import com.sras.backend.service.ApprovalRequestService;
import com.sras.backend.service.AuthService;
import com.sras.backend.service.OwnerService;
import com.sras.backend.service.ProductQueryService;
import com.sras.backend.service.StaffService;
import com.sras.backend.service.UserManagementService;
import com.sras.backend.service.ZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        ApprovalRequestController.class,
        UserManagementController.class,
        ZoneController.class,
        OwnerController.class,
        StaffController.class
})
@Import({
        WebMvcConfig.class,
        RequireRolesInterceptor.class,
        CurrentUserArgumentResolver.class,
        GlobalExceptionHandler.class
})
class SecurityRoleAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private ApprovalRequestService approvalRequestService;

    @MockBean
    private UserManagementService userManagementService;

    @MockBean
    private ZoneService zoneService;

    @MockBean
    private ProductQueryService productQueryService;

    @MockBean
    private OwnerService ownerService;

    @MockBean
    private StaffService staffService;

    private User adminUser;
    private User managerUser;
    private User staffUser;
    private User ownerUser;

    private User createUser(Integer id, String name, Role role) {
        User u = new User();
        u.setUserId(id);
        u.setFullName(name);
        u.setRole(role);
        u.setIsActive(true);
        return u;
    }

    @BeforeEach
    void setUp() {
        adminUser = createUser(503092, "Admin User", Role.ADMIN);
        managerUser = createUser(503069, "Manager User", Role.MANAGER);
        staffUser = createUser(503080, "Staff User", Role.STAFF);
        ownerUser = createUser(503001, "Owner User", Role.OWNER);
    }

    @Test
    @DisplayName("POST /api/requests/{id}/approve by STAFF should return 403 Forbidden")
    void approveRequest_byStaff_returns403() throws Exception {
        when(authService.requireRole(any(), eq("503080"), any()))
                .thenThrow(new ForbiddenException("Access denied: insufficient privileges"));

        DecisionRequest reviewReq = new DecisionRequest("Trying to approve as staff");

        mockMvc.perform(post("/api/requests/101/approve")
                        .header("X-User-Id", "503080")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied: insufficient privileges"));
    }

    @Test
    @DisplayName("POST /api/requests/{id}/approve by MANAGER should return 200 OK")
    void approveRequest_byManager_returns200() throws Exception {
        when(authService.requireRole(any(), eq("503069"), any()))
                .thenReturn(managerUser);

        ApprovalRequestDto dto = ApprovalRequestDto.builder()
                .requestId(101)
                .status("APPROVED")
                .requestType("PO_CREATE")
                .targetType("PRODUCT")
                .build();

        when(approvalRequestService.approveRequest(eq(101), eq(managerUser), any()))
                .thenReturn(dto);

        DecisionRequest reviewReq = new DecisionRequest("Approved by manager");

        mockMvc.perform(post("/api/requests/101/approve")
                        .header("X-User-Id", "503069")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/requests/user-create by STAFF should return 403 Forbidden")
    void createUserRequest_byStaff_returns403() throws Exception {
        when(authService.requireRole(any(), eq("503080"), any()))
                .thenThrow(new ForbiddenException("Access denied: Role ADMIN required"));

        UserCreateRequest req = new UserCreateRequest(null, "New Employee", "password123", "STAFF", "New hire");

        mockMvc.perform(post("/api/requests/user-create")
                        .header("X-User-Id", "503080")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/requests/user-create by ADMIN should return 200 OK")
    void createUserRequest_byAdmin_returns200() throws Exception {
        when(authService.requireRole(any(), eq("503092"), any()))
                .thenReturn(adminUser);

        ApprovalRequestDto dto = ApprovalRequestDto.builder()
                .requestId(202)
                .status("PENDING")
                .requestType("USER_CREATE")
                .build();

        when(userManagementService.submitUserCreateRequest(any(), eq(adminUser)))
                .thenReturn(dto);

        UserCreateRequest req = new UserCreateRequest(null, "New Employee", "password123", "STAFF", "New hire");

        mockMvc.perform(post("/api/requests/user-create")
                        .header("X-User-Id", "503092")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/zones by STAFF should return 403 Forbidden")
    void createZone_byStaff_returns403() throws Exception {
        when(authService.requireRole(any(), eq("503080"), any()))
                .thenThrow(new ForbiddenException("Access denied: Role ADMIN required"));

        ZoneCreateRequest req = new ZoneCreateRequest("Z9", "Zone 9", 500);

        mockMvc.perform(post("/api/zones")
                        .header("X-User-Id", "503080")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/owner/dashboard by STAFF should return 403 Forbidden")
    void getOwnerDashboard_byStaff_returns403() throws Exception {
        when(authService.requireRole(any(), eq("503080"), any()))
                .thenThrow(new ForbiddenException("Access denied: Role OWNER required"));

        mockMvc.perform(get("/api/owner/dashboard")
                        .header("X-User-Id", "503080"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/owner/dashboard by OWNER should return 200 OK")
    void getOwnerDashboard_byOwner_returns200() throws Exception {
        when(authService.requireRole(any(), eq("503001"), any()))
                .thenReturn(ownerUser);

        OwnerDashboardDto dashboardDto = OwnerDashboardDto.builder()
                .totalRevenue("฿1,500,000")
                .inventoryValue("฿500,000")
                .totalSale("150")
                .totalExport("1,900")
                .totalProfit("฿492,000")
                .build();

        when(ownerService.getDashboard()).thenReturn(dashboardDto);

        mockMvc.perform(get("/api/owner/dashboard")
                        .header("X-User-Id", "503001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRevenue").value("฿1,500,000"));
    }

    @Test
    @DisplayName("POST /api/staff/receiving/{id}/accept by ADMIN should return 403 Forbidden")
    void acceptReceiving_byAdmin_returns403() throws Exception {
        when(authService.requireRole(any(), eq("503092"), any()))
                .thenThrow(new ForbiddenException("Access denied: Role STAFF required"));

        mockMvc.perform(post("/api/staff/receiving/1001/accept")
                        .header("X-User-Id", "503092")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
