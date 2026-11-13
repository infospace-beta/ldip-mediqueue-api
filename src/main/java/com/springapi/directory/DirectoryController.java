package com.springapi.directory;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springapi.department.DepartmentService;
import com.springapi.department.DepartmentStatus;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.hospital.HospitalService;
import com.springapi.hospital.HospitalStatus;
import com.springapi.user.UserRole;
import com.springapi.user.UserService;
import com.springapi.user.dto.UserResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/directory")
@SecurityRequirement(name = "bearerAuth")
public class DirectoryController {

    private static final int DIRECTORY_PAGE_SIZE = 100;

    private final HospitalService hospitalService;
    private final DepartmentService departmentService;
    private final UserService userService;

    public DirectoryController(
            HospitalService hospitalService,
            DepartmentService departmentService,
            UserService userService) {
        this.hospitalService = hospitalService;
        this.departmentService = departmentService;
        this.userService = userService;
    }

    @GetMapping("/hospitals")
    @PreAuthorize("isAuthenticated()")
    public List<HospitalDirectoryEntry> getHospitals() {
        return hospitalService.getHospitalsByStatus(
                        HospitalStatus.ACTIVE,
                        PageRequest.of(0, DIRECTORY_PAGE_SIZE))
                .map(HospitalDirectoryEntry::from)
                .getContent();
    }

    @GetMapping("/hospitals/{hospitalId}/departments")
    @PreAuthorize("@directoryAuthorization.canReadDepartments(authentication, #p0)")
    public List<DepartmentDirectoryEntry> getDepartments(
            @PathVariable Long hospitalId) {
        if (hospitalService.getHospitalById(hospitalId).getStatus()
                != HospitalStatus.ACTIVE) {
            throw new ResourceNotFoundException("Active hospital", hospitalId);
        }
        return departmentService.getDepartmentsByHospitalAndStatus(
                        hospitalId,
                        DepartmentStatus.ACTIVE,
                        PageRequest.of(0, DIRECTORY_PAGE_SIZE))
                .map(DepartmentDirectoryEntry::from)
                .getContent();
    }

    @GetMapping("/hospitals/{hospitalId}/doctors")
    @PreAuthorize("hasRole('PATIENT') or @hospitalAuthorization.canAccess(authentication, #p0)")
    public List<DoctorDirectoryEntry> getDoctors(
            @PathVariable Long hospitalId) {
        if (hospitalService.getHospitalById(hospitalId).getStatus()
                != HospitalStatus.ACTIVE) {
            throw new ResourceNotFoundException("Active hospital", hospitalId);
        }
        return userService.findAvailableDoctors(
                        hospitalId, PageRequest.of(0, DIRECTORY_PAGE_SIZE))
                .content()
                .stream()
                .map(DoctorDirectoryEntry::from)
                .toList();
    }

    public record HospitalDirectoryEntry(
            Long id,
            String name,
            String county,
            String town) {
        static HospitalDirectoryEntry from(
                com.springapi.hospital.Hospital hospital) {
            return new HospitalDirectoryEntry(
                    hospital.getId(),
                    hospital.getName(),
                    hospital.getCounty(),
                    hospital.getTown());
        }
    }

    public record DepartmentDirectoryEntry(
            Long id,
            Long hospitalId,
            String name,
            String code) {
        static DepartmentDirectoryEntry from(
                com.springapi.department.Department department) {
            return new DepartmentDirectoryEntry(
                    department.getId(),
                    department.getHospital().getId(),
                    department.getName(),
                    department.getCode());
        }
    }

    public record DoctorDirectoryEntry(Long id, String fullName) {
        static DoctorDirectoryEntry from(UserResponse doctor) {
            if (doctor.role() != UserRole.DOCTOR) {
                throw new IllegalArgumentException(
                        "Only doctors can appear in the appointment directory.");
            }
            return new DoctorDirectoryEntry(doctor.id(), doctor.fullName());
        }
    }
}
