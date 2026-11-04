package com.springapi.doctor;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.springapi.common.dto.PageResponse;
import com.springapi.user.UserService;
import com.springapi.user.dto.UserRequest;
import com.springapi.user.dto.UserResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/hospitals/{hospitalId}/doctors")
@Tag(name = "Doctors")
@SecurityRequirement(name = "bearerAuth")
public class DoctorController {

    private final UserService userService;

    public DoctorController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("@hospitalAuthorization.canAccess(authentication, #p0)")
    public PageResponse<UserResponse> getDoctors(
            @PathVariable Long hospitalId,
            @RequestParam(required = false) Boolean onDuty,
            @ParameterObject @SortDefault("id") Pageable pageable) {
        return userService.findDoctors(hospitalId, onDuty, pageable);
    }

    @GetMapping("/{doctorId}")
    @PreAuthorize("@doctorAuthorization.canAccessDoctor(authentication, #p0, #p1)")
    public UserResponse getDoctor(
            @PathVariable Long hospitalId,
            @PathVariable Long doctorId) {
        return userService.findDoctor(hospitalId, doctorId);
    }

    @PostMapping
    @PreAuthorize("@hospitalAuthorization.canAccess(authentication, #p0)")
    public ResponseEntity<UserResponse> createDoctor(
            @PathVariable Long hospitalId,
            @Valid @RequestBody UserRequest request) {
        UserResponse created = userService.createDoctor(hospitalId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{doctorId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{doctorId}")
    @PreAuthorize("@hospitalAuthorization.canAccess(authentication, #p0)")
    public UserResponse updateDoctor(
            @PathVariable Long hospitalId,
            @PathVariable Long doctorId,
            @Valid @RequestBody UserRequest request) {
        return userService.updateDoctor(hospitalId, doctorId, request);
    }

    @PatchMapping("/{doctorId}/availability")
    @PreAuthorize("@doctorAuthorization.canAccessDoctor(authentication, #p0, #p1)")
    public UserResponse updateAvailability(
            @PathVariable Long hospitalId,
            @PathVariable Long doctorId,
            @Valid @RequestBody DoctorAvailabilityRequest request) {
        return userService.setDoctorOnDuty(
                hospitalId, doctorId, request.onDuty());
    }
}
