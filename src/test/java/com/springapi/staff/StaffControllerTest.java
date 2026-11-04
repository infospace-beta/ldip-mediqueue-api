package com.springapi.staff;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.springapi.common.dto.PageResponse;
import com.springapi.user.UserRole;
import com.springapi.user.UserService;
import com.springapi.user.dto.UserRequest;
import com.springapi.user.dto.UserResponse;

class StaffControllerTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new StaffController(userService))
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void listsStaffForHospital() throws Exception {
        when(userService.findStaff(
                eq(2L), eq((UserRole) null), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/v1/hospitals/2/staff"))
                .andExpect(status().isOk());

        verify(userService).findStaff(
                eq(2L), eq((UserRole) null), any(Pageable.class));
    }

    @Test
    void createsStaffMember() throws Exception {
        when(userService.createStaff(eq(2L), any(UserRequest.class)))
                .thenReturn(userResponse(UserRole.RESPONDER));

        mockMvc.perform(post("/api/v1/hospitals/2/staff")
                        .contentType("application/json")
                        .content(userRequestJson("RESPONDER")))
                .andExpect(status().isCreated());

        verify(userService).createStaff(eq(2L), any(UserRequest.class));
    }

    @Test
    void updatesStaffMember() throws Exception {
        when(userService.updateStaff(eq(2L), eq(6L), any(UserRequest.class)))
                .thenReturn(userResponse(UserRole.MEDICAL_STAFF));

        mockMvc.perform(put("/api/v1/hospitals/2/staff/6")
                        .contentType("application/json")
                        .content(userRequestJson("MEDICAL_STAFF")))
                .andExpect(status().isOk());

        verify(userService).updateStaff(eq(2L), eq(6L), any(UserRequest.class));
    }

    @Test
    void deactivatesStaffMember() throws Exception {
        mockMvc.perform(patch("/api/v1/hospitals/2/staff/6/deactivate"))
                .andExpect(status().isNoContent());

        verify(userService).deactivateStaff(2L, 6L);
    }

    private String userRequestJson(String role) {
        return """
                {
                  "username": "worker",
                  "email": "worker@example.com",
                  "fullName": "Hospital Worker",
                  "role": "%s",
                  "enabled": true,
                  "password": "secure-password"
                }
                """.formatted(role);
    }

    private UserResponse userResponse(UserRole role) {
        return new UserResponse(
                6L, "worker", "worker@example.com", "Hospital Worker",
                role, true, false, 2L, null, null);
    }
}
