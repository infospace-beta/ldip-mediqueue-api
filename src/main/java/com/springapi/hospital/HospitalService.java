
package com.springapi.hospital;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    @Transactional(readOnly = true)
    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Hospital getHospitalById(Long id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Hospital not found with ID: " + id));
    }

    public Hospital createHospital(Hospital hospital) {
        hospital.setName(hospital.getName().trim());
        return hospitalRepository.save(hospital);
    }

    public Hospital updateHospital(Long id, Hospital updatedHospital) {
        Hospital existingHospital = getHospitalById(id);

        existingHospital.setName(updatedHospital.getName().trim());
        existingHospital.setAddress(updatedHospital.getAddress());
        existingHospital.setPhone(updatedHospital.getPhone());
        existingHospital.setEmail(updatedHospital.getEmail());

        return hospitalRepository.save(existingHospital);
    }

    public void deleteHospital(Long id) {
        Hospital hospital = getHospitalById(id);
        hospitalRepository.delete(hospital);
    }
}
