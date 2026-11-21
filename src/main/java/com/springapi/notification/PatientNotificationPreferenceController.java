package com.springapi.notification;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springapi.notification.dto.PatientNotificationPreferenceRequest;
import com.springapi.notification.dto.PatientNotificationPreferenceResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/patient/notification-preferences")
@PreAuthorize("hasRole('PATIENT')")
@SecurityRequirement(name = "bearerAuth")
public class PatientNotificationPreferenceController {

    private final PatientNotificationPreferenceService preferenceService;

    public PatientNotificationPreferenceController(
            PatientNotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public PatientNotificationPreferenceResponse getPreferences(
            @AuthenticationPrincipal UserDetails currentUser) {
        return preferenceService.getPreferences(currentUser.getUsername());
    }

    @PutMapping
    public PatientNotificationPreferenceResponse savePreferences(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody PatientNotificationPreferenceRequest request) {
        return preferenceService.savePreferences(currentUser.getUsername(), request);
    }
}