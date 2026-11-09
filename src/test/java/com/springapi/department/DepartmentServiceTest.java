package com.springapi.department;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springapi.common.exception.ConflictException;
import com.springapi.department.dto.DepartmentRequest;
import com.springapi.hospital.Hospital;
import com.springapi.hospital.HospitalRepository;

class DepartmentServiceTest {

    private DepartmentRepository departmentRepository;
    private HospitalRepository hospitalRepository;
    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(DepartmentRepository.class);
        hospitalRepository = mock(HospitalRepository.class);
        departmentService = new DepartmentService(
                departmentRepository, hospitalRepository);
    }

    @Test
    void rejectsDuplicateDepartmentName() {
        when(hospitalRepository.findById(3L))
                .thenReturn(Optional.of(new Hospital()));
        when(departmentRepository.existsByHospitalIdAndNameIgnoreCase(
                3L, "Emergency"))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> departmentService.createDepartment(3L,
                        new DepartmentRequest(
                                3L, "Emergency", "ER", null,
                                DepartmentStatus.ACTIVE)));

        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    void rejectsDepartmentRequestForDifferentHospital() {
        assertThrows(IllegalArgumentException.class,
                () -> departmentService.createDepartment(3L,
                        new DepartmentRequest(
                                4L, "Emergency", "ER", null,
                                DepartmentStatus.ACTIVE)));

        verify(hospitalRepository, never()).findById(3L);
    }
}
