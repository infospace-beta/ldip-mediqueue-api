package com.springapi.emergency;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springapi.common.dto.PageResponse;
import com.springapi.emergency.dto.EmergencyResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/responders/me/emergency-cases")
@Tag(name = "Responder emergency cases")
@SecurityRequirement(name = "bearerAuth")
public class ResponderEmergencyController {

    private final EmergencyService emergencyService;

    public ResponderEmergencyController(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    @GetMapping
    @PreAuthorize("hasRole('RESPONDER')")
    public PageResponse<EmergencyResponse> getAssignedCases(
            @AuthenticationPrincipal UserDetails currentUser,
            @ParameterObject @SortDefault("createdAt") Pageable pageable) {

        return emergencyService.getAssignedToResponder(
                currentUser.getUsername(),
                pageable);
    }
}
