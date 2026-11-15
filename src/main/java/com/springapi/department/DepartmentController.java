package com.springapi.department;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.springapi.department.dto.DepartmentRequest;
import com.springapi.department.dto.DepartmentResponse;
import com.springapi.common.dto.PageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/hospitals/{hospitalId}/departments")
@Tag(name = "Departments")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@hospitalAuthorization.canAccess(authentication, #p0)")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public PageResponse<DepartmentResponse> getAllDepartments(
            @PathVariable Long hospitalId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DepartmentStatus status,
            @ParameterObject @SortDefault("id") Pageable pageable) {
        return PageResponse.from(departmentService
                .getAllDepartmentsByHospital(
                        hospitalId, search, status, pageable)
                .map(DepartmentResponse::from));
    }

    @GetMapping("/{departmentId}")
    public DepartmentResponse getDepartment(
            @PathVariable Long hospitalId,
            @PathVariable Long departmentId) {
        return DepartmentResponse.from(
                departmentService.getDepartmentById(hospitalId, departmentId));
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @PathVariable Long hospitalId,
            @Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse created = DepartmentResponse.from(
                departmentService.createDepartment(hospitalId, request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{departmentId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{departmentId}")
    public DepartmentResponse updateDepartment(
            @PathVariable Long hospitalId,
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentRequest request) {
        return DepartmentResponse.from(
                departmentService.updateDepartment(
                        hospitalId, departmentId, request));
    }

    @DeleteMapping("/{departmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDepartment(
            @PathVariable Long hospitalId,
            @PathVariable Long departmentId) {
        departmentService.deleteDepartment(hospitalId, departmentId);
    }
}
