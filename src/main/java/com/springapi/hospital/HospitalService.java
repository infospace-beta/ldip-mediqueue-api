
package com.springapi.hospital;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.department.DepartmentRepository;

@Service
@Transactional
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;

    public HospitalService(
            HospitalRepository hospitalRepository,
            DepartmentRepository departmentRepository) {
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public Page<Hospital> getAllHospitals(
            String search,
            HospitalStatus status,
            Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank()
                ? null : search.trim();
        return hospitalRepository.findFiltered(
                normalizedSearch, status, pageable);
    }

    @Transactional(readOnly = true)
    public Hospital getHospitalById(Long id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Hospital", id));
    }

    public Hospital createHospital(Hospital hospital) {

        hospital.setName(hospital.getName().trim());

        String registrationNumber =
                normalizeRegistrationNumber(hospital.getRegistrationNumber());

        hospital.setRegistrationNumber(registrationNumber);

        if (registrationNumber != null
                && hospitalRepository.existsByRegistrationNumber(registrationNumber)) {
            throw new ConflictException(
                    "A hospital with registration number "
                            + registrationNumber + " already exists."
            );
        }

        normalizeOptionalFields(hospital);

        return hospitalRepository.save(hospital);
    }

    public Hospital updateHospital(Long id, Hospital updatedHospital) {

        Hospital existingHospital = getHospitalById(id);

        String registrationNumber =
                normalizeRegistrationNumber(updatedHospital.getRegistrationNumber());

        if (registrationNumber != null
                && hospitalRepository.existsByRegistrationNumberAndIdNot(
                        registrationNumber, id)) {
            throw new ConflictException(
                    "Another hospital already uses registration number "
                            + registrationNumber + "."
            );
        }

        existingHospital.setName(updatedHospital.getName().trim());
        existingHospital.setRegistrationNumber(registrationNumber);
        existingHospital.setHospitalType(updatedHospital.getHospitalType());
        existingHospital.setAddress(updatedHospital.getAddress());
        existingHospital.setCounty(updatedHospital.getCounty());
        existingHospital.setTown(updatedHospital.getTown());
        existingHospital.setPhone(updatedHospital.getPhone());
        existingHospital.setEmail(updatedHospital.getEmail());
        existingHospital.setStatus(updatedHospital.getStatus());

        normalizeOptionalFields(existingHospital);

        return hospitalRepository.save(existingHospital);
    }

    public void deleteHospital(Long id) {

        Hospital hospital = getHospitalById(id);

        if (departmentRepository.existsByHospitalId(id)) {
            throw new ConflictException(
                    "Cannot delete a hospital while it has departments.");
        }

        hospitalRepository.delete(hospital);
    }

    private String normalizeRegistrationNumber(String registrationNumber) {

        if (registrationNumber == null
                || registrationNumber.isBlank()) {
            return null;
        }

        return registrationNumber.trim().toUpperCase();
    }

    private void normalizeOptionalFields(Hospital hospital) {

        if (hospital.getAddress() != null) {
            hospital.setAddress(hospital.getAddress().trim());
        }

        if (hospital.getCounty() != null) {
            hospital.setCounty(hospital.getCounty().trim());
        }

        if (hospital.getTown() != null) {
            hospital.setTown(hospital.getTown().trim());
        }

        if (hospital.getPhone() != null) {
            hospital.setPhone(hospital.getPhone().trim());
        }

        if (hospital.getEmail() != null) {
            hospital.setEmail(hospital.getEmail().trim().toLowerCase());
        }
    }
}
