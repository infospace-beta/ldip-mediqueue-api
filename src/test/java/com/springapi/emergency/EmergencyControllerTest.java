package com.springapi.emergency;

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

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.springapi.emergency.dto.EmergencyRequest;
import com.springapi.emergency.dto.EmergencyResponse;
import com.springapi.common.dto.PageResponse;

class EmergencyControllerTest {

    private EmergencyService emergencyService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        emergencyService = mock(EmergencyService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new EmergencyController(emergencyService))
                .setCustomArgumentResolvers(
                        new ReporterPrincipalResolver(),
                        new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void listsEmergencyCasesForHospital() throws Exception {
        when(emergencyService.getAll(
                eq(2L), eq((EmergencyStatus) null),
                eq((EmergencySeverity) null), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/v1/hospitals/2/emergency-cases"))
                .andExpect(status().isOk());

        verify(emergencyService).getAll(
                eq(2L), eq((EmergencyStatus) null),
                eq((EmergencySeverity) null), any(Pageable.class));
    }

    @Test
    void getsEmergencyCase() throws Exception {
        when(emergencyService.get(2L, 5L)).thenReturn(emergencyCase());

        mockMvc.perform(get("/api/v1/hospitals/2/emergency-cases/5"))
                .andExpect(status().isOk());

        verify(emergencyService).get(2L, 5L);
    }

    @Test
    void reportsEmergencyCase() throws Exception {
        when(emergencyService.report(
                eq(2L), eq("reporter"), any(EmergencyRequest.class)))
                .thenReturn(emergencyCase());

        mockMvc.perform(post("/api/v1/hospitals/2/emergency-cases")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Road accident",
                                  "description": "Two people need assistance",
                                  "severity": "HIGH",
                                  "patientId": null
                                }
                                """))
                .andExpect(status().isCreated());

        verify(emergencyService).report(
                eq(2L), eq("reporter"), any(EmergencyRequest.class));
    }

    @Test
    void updatesEmergencyCase() throws Exception {
        when(emergencyService.update(
                eq(2L), eq(5L), any(EmergencyRequest.class)))
                .thenReturn(emergencyCase());

        mockMvc.perform(put("/api/v1/hospitals/2/emergency-cases/5")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Road accident",
                                  "description": "Updated details",
                                  "severity": "HIGH"
                                }
                                """))
                .andExpect(status().isOk());

        verify(emergencyService).update(
                eq(2L), eq(5L), any(EmergencyRequest.class));
    }

    @Test
    void assignsResponder() throws Exception {
        when(emergencyService.assignResponder(2L, 5L, 8L))
                .thenReturn(emergencyCase());

        mockMvc.perform(put(
                        "/api/v1/hospitals/2/emergency-cases/5/assignment")
                        .contentType("application/json")
                        .content("{\"responderId\":8}"))
                .andExpect(status().isOk());

        verify(emergencyService).assignResponder(2L, 5L, 8L);
    }

    @Test
    void updatesEmergencyStatus() throws Exception {
        when(emergencyService.updateStatus(
                2L, 5L, EmergencyStatus.RESPONDING))
                .thenReturn(emergencyCase());

        mockMvc.perform(patch(
                        "/api/v1/hospitals/2/emergency-cases/5/status")
                        .contentType("application/json")
                        .content("{\"status\":\"RESPONDING\"}"))
                .andExpect(status().isOk());

        verify(emergencyService).updateStatus(
                2L, 5L, EmergencyStatus.RESPONDING);
    }

    @Test
    void resolvesEmergencyCase() throws Exception {
        when(emergencyService.resolve(2L, 5L)).thenReturn(emergencyCase());

        mockMvc.perform(post(
                        "/api/v1/hospitals/2/emergency-cases/5/resolve"))
                .andExpect(status().isOk());

        verify(emergencyService).resolve(2L, 5L);
    }

    private EmergencyResponse emergencyCase() {
        return new EmergencyResponse(
                5L, 2L, null, 3L, null, "Road accident",
                "Two people need assistance", EmergencySeverity.HIGH,
                EmergencyStatus.REPORTED, null,
                Instant.parse("2026-11-04T07:00:00Z"),
                Instant.parse("2026-11-04T07:00:00Z"));
    }

    private static class ReporterPrincipalResolver
            implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && UserDetails.class.isAssignableFrom(
                            parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            return User.withUsername("reporter")
                    .password("")
                    .roles("PATIENT")
                    .build();
        }
    }
}
