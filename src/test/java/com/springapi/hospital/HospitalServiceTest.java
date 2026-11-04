package com.springapi.hospital;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springapi.common.exception.ConflictException;
import com.springapi.department.DepartmentRepository;

class HospitalServiceTest {

    private HospitalRepository hospitalRepository;
    private DepartmentRepository departmentRepository;
    private HospitalService hospitalService;

    @BeforeEach
    void setUp() {
        hospitalRepository = mock(HospitalRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        hospitalService = new HospitalService(
                hospitalRepository, departmentRepository);
    }

    @Test
    void preventsDeletingHospitalWithDepartments() {
        when(hospitalRepository.findById(3L))
                .thenReturn(Optional.of(new Hospital()));
        when(departmentRepository.existsByHospitalId(3L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> hospitalService.deleteHospital(3L));

        verify(hospitalRepository, never()).delete(org.mockito.ArgumentMatchers.any(Hospital.class));
    }
}
