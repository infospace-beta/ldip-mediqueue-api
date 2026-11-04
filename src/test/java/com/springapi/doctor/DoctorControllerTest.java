package com.springapi.doctor;

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

class DoctorControllerTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DoctorController(userService))
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void listsDoctorsForHospital() throws Exception {
        when(userService.findDoctors(
                eq(2L), eq((Boolean) null), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/v1/hospitals/2/doctors"))
                .andExpect(status().isOk());

        verify(userService).findDoctors(
                eq(2L), eq((Boolean) null), any(Pageable.class));
    }

    @Test
    void getsDoctorForHospital() throws Exception {
        when(userService.findDoctor(2L, 6L))
                .thenReturn(userResponse());

        mockMvc.perform(get("/api/v1/hospitals/2/doctors/6"))
                .andExpect(status().isOk());

        verify(userService).findDoctor(2L, 6L);
    }

    @Test
    void createsDoctor() throws Exception {
        when(userService.createDoctor(eq(2L), any(UserRequest.class)))
                .thenReturn(userResponse());

        mockMvc.perform(post("/api/v1/hospitals/2/doctors")
                        .contentType("application/json")
                        .content(doctorRequestJson()))
                .andExpect(status().isCreated());

        verify(userService).createDoctor(eq(2L), any(UserRequest.class));
    }

    @Test
    void updatesDoctor() throws Exception {
        when(userService.updateDoctor(eq(2L), eq(6L), any(UserRequest.class)))
                .thenReturn(userResponse());

        mockMvc.perform(put("/api/v1/hospitals/2/doctors/6")
                        .contentType("application/json")
                        .content(doctorRequestJson()))
                .andExpect(status().isOk());

        verify(userService).updateDoctor(eq(2L), eq(6L), any(UserRequest.class));
    }

    @Test
    void updatesDoctorAvailability() throws Exception {
        when(userService.setDoctorOnDuty(2L, 6L, true))
                .thenReturn(userResponse());

        mockMvc.perform(patch(
                        "/api/v1/hospitals/2/doctors/6/availability")
                        .contentType("application/json")
                        .content("{\"onDuty\":true}"))
                .andExpect(status().isOk());

        verify(userService).setDoctorOnDuty(2L, 6L, true);
    }

    private String doctorRequestJson() {
        return """
                {
                  "username": "doctor",
                  "email": "doctor@example.com",
                  "fullName": "Doctor",
                  "role": "DOCTOR",
                  "enabled": true,
                  "password": "secure-password"
                }
                """;
    }

    private UserResponse userResponse() {
        return new UserResponse(
                6L, "doctor", "doctor@example.com", "Doctor",
                UserRole.DOCTOR, true, false, 2L, null, null);
    }
}
