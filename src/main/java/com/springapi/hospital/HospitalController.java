
package com.springapi.hospital;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.springapi.hospital.dto.HospitalRequest;
import com.springapi.hospital.dto.HospitalResponse;
import com.springapi.common.dto.PageResponse;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/hospitals")
@SecurityRequirement(name = "bearerAuth")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResponse<HospitalResponse> getAllHospitals(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) HospitalStatus status,
            @ParameterObject @SortDefault("id") Pageable pageable) {
        return PageResponse.from(hospitalService
                .getAllHospitals(search, status, pageable)
                .map(HospitalResponse::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or @hospitalAuthorization.canAccess(authentication, #p0)")
    public HospitalResponse getHospitalById(@PathVariable Long id) {
        return HospitalResponse.from(
                hospitalService.getHospitalById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalResponse createHospital(
            @Valid @RequestBody HospitalRequest request) {

        Hospital hospital = new Hospital();

        hospital.setName(request.name().trim());
        hospital.setRegistrationNumber(request.registrationNumber());
        hospital.setHospitalType(request.hospitalType());
        hospital.setAddress(request.address());
        hospital.setCounty(request.county());
        hospital.setTown(request.town());
        hospital.setPhone(request.phone());
        hospital.setEmail(request.email());
        hospital.setStatus(request.status());

        return HospitalResponse.from(
                hospitalService.createHospital(hospital));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or @hospitalAuthorization.canAccess(authentication, #p0)")
    public HospitalResponse updateHospital(
            @PathVariable Long id,
            @Valid @RequestBody HospitalRequest request) {

        Hospital hospital = new Hospital();

        hospital.setName(request.name().trim());
        hospital.setRegistrationNumber(request.registrationNumber());
        hospital.setHospitalType(request.hospitalType());
        hospital.setAddress(request.address());
        hospital.setCounty(request.county());
        hospital.setTown(request.town());
        hospital.setPhone(request.phone());
        hospital.setEmail(request.email());
        hospital.setStatus(request.status());

        return HospitalResponse.from(
                hospitalService.updateHospital(id, hospital));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHospital(@PathVariable Long id) {
        hospitalService.deleteHospital(id);
    }
}
