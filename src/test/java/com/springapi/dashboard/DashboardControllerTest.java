package com.springapi.dashboard;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.springapi.dashboard.dto.DoctorDashboardResponse;
import com.springapi.dashboard.dto.HospitalDashboardResponse;
import com.springapi.dashboard.dto.ResponderDashboardResponse;
import com.springapi.dashboard.dto.StaffDashboardResponse;

class DashboardControllerTest {

    private DashboardService dashboardService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        dashboardService = mock(DashboardService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DashboardController(dashboardService))
                .setCustomArgumentResolvers(new DashboardUserResolver())
                .build();
    }

    @Test
    void returnsHospitalDashboard() throws Exception {
        when(dashboardService.getHospitalDashboard(2L))
                .thenReturn(new HospitalDashboardResponse(2L, 3, 5.0, 1, 2));

        mockMvc.perform(get("/api/v1/dashboards/hospitals/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalId").value(2))
                .andExpect(jsonPath("$.waitingTickets").value(3))
                .andExpect(jsonPath("$.averageWaitMinutes").value(5.0))
                .andExpect(jsonPath("$.openEmergencyCases").value(1))
                .andExpect(jsonPath("$.onDutyDoctors").value(2));

        verify(dashboardService).getHospitalDashboard(2L);
    }

    @Test
    void returnsDoctorDashboard() throws Exception {
        when(dashboardService.getDoctorDashboard("dashboard-user"))
                .thenReturn(new DoctorDashboardResponse(2L, true, 3, 1));

        mockMvc.perform(get("/api/v1/dashboards/doctor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalId").value(2))
                .andExpect(jsonPath("$.onDuty").value(true))
                .andExpect(jsonPath("$.waitingTickets").value(3))
                .andExpect(jsonPath("$.openEmergencyCases").value(1));

        verify(dashboardService).getDoctorDashboard("dashboard-user");
    }

    @Test
    void returnsStaffDashboard() throws Exception {
        when(dashboardService.getStaffDashboard("dashboard-user"))
                .thenReturn(new StaffDashboardResponse(2L, 3, 1, 2));

        mockMvc.perform(get("/api/v1/dashboards/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalId").value(2))
                .andExpect(jsonPath("$.waitingTickets").value(3))
                .andExpect(jsonPath("$.openEmergencyCases").value(1))
                .andExpect(jsonPath("$.onDutyDoctors").value(2));

        verify(dashboardService).getStaffDashboard("dashboard-user");
    }

    @Test
    void returnsResponderDashboard() throws Exception {
        when(dashboardService.getResponderDashboard("dashboard-user"))
                .thenReturn(new ResponderDashboardResponse(2L, 1, 1));

        mockMvc.perform(get("/api/v1/dashboards/responder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalId").value(2))
                .andExpect(jsonPath("$.assignedCases").value(1))
                .andExpect(jsonPath("$.respondingCases").value(1));

        verify(dashboardService).getResponderDashboard("dashboard-user");
    }

    private static class DashboardUserResolver
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
            return User.withUsername("dashboard-user")
                    .password("")
                    .roles("DOCTOR")
                    .build();
        }
    }
}
