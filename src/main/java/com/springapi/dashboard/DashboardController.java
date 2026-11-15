package com.springapi.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springapi.dashboard.dto.DoctorDashboardResponse;
import com.springapi.dashboard.dto.HospitalDashboardResponse;
import com.springapi.dashboard.dto.ResponderDashboardResponse;
import com.springapi.dashboard.dto.StaffDashboardResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/dashboards")
@Tag(name = "Dashboards")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/hospitals/{hospitalId}")
    @PreAuthorize("@hospitalAuthorization.canAccess(authentication, #p0)")
    public HospitalDashboardResponse getHospitalDashboard(
            @PathVariable Long hospitalId) {
        return dashboardService.getHospitalDashboard(hospitalId);
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorDashboardResponse getDoctorDashboard(
            @AuthenticationPrincipal UserDetails currentUser) {
        return dashboardService.getDoctorDashboard(currentUser.getUsername());
    }

    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'MEDICAL_STAFF')")
    public StaffDashboardResponse getStaffDashboard(
            @AuthenticationPrincipal UserDetails currentUser) {
        return dashboardService.getStaffDashboard(currentUser.getUsername());
    }

    @GetMapping("/responder")
    @PreAuthorize("hasRole('RESPONDER')")
    public ResponderDashboardResponse getResponderDashboard(
            @AuthenticationPrincipal UserDetails currentUser) {
        return dashboardService.getResponderDashboard(
                currentUser.getUsername());
    }
}
