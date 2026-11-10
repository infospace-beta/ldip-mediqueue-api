
package com.springapi.department;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.hospital.Hospital;
import com.springapi.hospital.HospitalRepository;
import com.springapi.department.dto.DepartmentRequest;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final HospitalRepository hospitalRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            HospitalRepository hospitalRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.hospitalRepository = hospitalRepository;
    }

    @Transactional(readOnly = true)
    public Page<Department> getAllDepartmentsByHospital(
            Long hospitalId,
            String search,
            DepartmentStatus status,
            Pageable pageable) {
        if (!hospitalRepository.existsById(hospitalId)) {
            throw new ResourceNotFoundException("Hospital", hospitalId);
        }

        String normalizedSearch = search == null || search.isBlank()
                ? null : search.trim();
        return departmentRepository.findFiltered(
                hospitalId, normalizedSearch, status, pageable);
    }

    @Transactional(readOnly = true)
    public Department getDepartmentById(Long hospitalId, Long id) {
        return departmentRepository.findByHospitalIdAndId(hospitalId, id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department", id));
    }

    public Department createDepartment(
            Long hospitalId,
            DepartmentRequest request
    ) {
        validateHospitalId(hospitalId, request.hospitalId());
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Hospital", hospitalId));

        String name = request.name().trim();
        String code = request.code().trim().toUpperCase();

        checkDuplicateName(hospitalId, name, null);
        checkDuplicateCode(hospitalId, code, null);

        Department department = new Department();
        department.setHospital(hospital);
        department.setName(name);
        department.setCode(code);
        department.setDescription(normalizeDescription(request.description()));
        department.setStatus(request.status());

        return departmentRepository.save(department);
    }

    public Department updateDepartment(
            Long hospitalId,
            Long id,
            DepartmentRequest request
    ) {
        validateHospitalId(hospitalId, request.hospitalId());
        Department department = getDepartmentById(hospitalId, id);

        String name = request.name().trim();
        String code = request.code().trim().toUpperCase();

        checkDuplicateName(hospitalId, name, id);
        checkDuplicateCode(hospitalId, code, id);

        department.setName(name);
        department.setCode(code);
        department.setDescription(normalizeDescription(request.description()));
        department.setStatus(request.status());

        return departmentRepository.save(department);
    }

    public void deleteDepartment(Long hospitalId, Long id) {
        Department department = getDepartmentById(hospitalId, id);
        departmentRepository.delete(department);
    }

    private void validateHospitalId(Long pathHospitalId, Long requestHospitalId) {
        if (!pathHospitalId.equals(requestHospitalId)) {
            throw new IllegalArgumentException(
                    "Hospital ID in the request must match the URL.");
        }
    }

    private void checkDuplicateName(
            Long hospitalId,
            String name,
            Long excludeId
    ) {
        boolean exists = excludeId == null
                ? departmentRepository.existsByHospitalIdAndNameIgnoreCase(
                        hospitalId, name)
                : departmentRepository.existsByHospitalIdAndNameIgnoreCaseAndIdNot(
                        hospitalId, name, excludeId);

        if (exists) {
            throw new ConflictException(
                    "A department with this name already exists in this hospital."
            );
        }
    }

    private void checkDuplicateCode(
            Long hospitalId,
            String code,
            Long excludeId
    ) {
        boolean exists = excludeId == null
                ? departmentRepository.existsByHospitalIdAndCodeIgnoreCase(
                        hospitalId, code)
                : departmentRepository.existsByHospitalIdAndCodeIgnoreCaseAndIdNot(
                        hospitalId, code, excludeId);

        if (exists) {
            throw new ConflictException(
                    "A department with this code already exists in this hospital."
            );
        }
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}
