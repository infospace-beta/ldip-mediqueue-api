package com.springapi.staff;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.springapi.common.dto.PageResponse;
import com.springapi.user.UserService;
import com.springapi.user.UserRole;
import com.springapi.user.dto.UserRequest;
import com.springapi.user.dto.UserResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/hospitals/{hospitalId}/staff")
@Tag(name = "Staff and responders")
@SecurityRequirement(name = "bearerAuth")
public class StaffController {

    private final UserService userService;

    public StaffController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("@hospitalAuthorization.canAccess(authentication, #p0)")
    public PageResponse<UserResponse> getStaff(
            @PathVariable Long hospitalId,
            @RequestParam(required = false) UserRole role,
            @ParameterObject @SortDefault("id") Pageable pageable) {
        return userService.findStaff(hospitalId, role, pageable);
    }

    @PostMapping
    @PreAuthorize("@hospitalAuthorization.canAccess(authentication, #p0)")
    public ResponseEntity<UserResponse> createStaff(
            @PathVariable Long hospitalId,
            @Valid @RequestBody UserRequest request) {
        UserResponse created = userService.createStaff(hospitalId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{staffId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{staffId}")
    @PreAuthorize("@hospitalAuthorization.canAccess(authentication, #p0)")
    public UserResponse updateStaff(
            @PathVariable Long hospitalId,
            @PathVariable Long staffId,
            @Valid @RequestBody UserRequest request) {
        return userService.updateStaff(hospitalId, staffId, request);
    }

    @PatchMapping("/{staffId}/deactivate")
    @PreAuthorize("@hospitalAuthorization.canAccess(authentication, #p0)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateStaff(
            @PathVariable Long hospitalId,
            @PathVariable Long staffId) {
        userService.deactivateStaff(hospitalId, staffId);
    }
}
