
package com.springapi.hospital;

import java.util.List;

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

import com.springapi.hospital.dto.HospitalRequest;
import com.springapi.hospital.dto.HospitalResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping
    public List<HospitalResponse> getAllHospitals() {
        return hospitalService.getAllHospitals()
                .stream()
                .map(HospitalResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public HospitalResponse getHospitalById(@PathVariable Long id) {
        return HospitalResponse.from(hospitalService.getHospitalById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalResponse createHospital(
            @Valid @RequestBody HospitalRequest request) {

        Hospital hospital = new Hospital();
        hospital.setName(request.name().trim());
        hospital.setAddress(request.address());
        hospital.setPhone(request.phone());
        hospital.setEmail(request.email());

        return HospitalResponse.from(hospitalService.createHospital(hospital));
    }

    @PutMapping("/{id}")
    public HospitalResponse updateHospital(
            @PathVariable Long id,
            @Valid @RequestBody HospitalRequest request) {

        Hospital hospital = new Hospital();
        hospital.setName(request.name().trim());
        hospital.setAddress(request.address());
        hospital.setPhone(request.phone());
        hospital.setEmail(request.email());

        return HospitalResponse.from(
                hospitalService.updateHospital(id, hospital));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHospital(@PathVariable Long id) {
        hospitalService.deleteHospital(id);
    }
}
