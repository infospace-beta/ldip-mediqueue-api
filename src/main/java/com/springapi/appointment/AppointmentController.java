package com.springapi.appointment;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.springapi.appointment.dto.AppointmentRequest;
import com.springapi.appointment.dto.AppointmentResponse;
import com.springapi.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Appointments")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/patients/{patientId}/appointments")
    @PreAuthorize("@patientAuthorization.canRead(authentication, #p0)")
    public PageResponse<AppointmentResponse> getPatientAppointments(
            @PathVariable Long patientId,
            @ParameterObject @SortDefault(
                    sort = "startsAt",
                    direction = org.springframework.data.domain.Sort.Direction.ASC)
            Pageable pageable) {
        return appointmentService.getPatientAppointments(patientId, pageable);
    }

    @PostMapping("/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(
            @Valid @RequestBody AppointmentRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return appointmentService.book(currentUser.getUsername(), request);
    }

    @DeleteMapping("/appointments/{appointmentId}")
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal UserDetails currentUser) {
        appointmentService.cancel(
                appointmentId, currentUser.getUsername());
    }
}
