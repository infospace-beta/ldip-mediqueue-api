package com.springapi.dev;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.springapi.department.Department;
import com.springapi.department.DepartmentRepository;
import com.springapi.hospital.Hospital;
import com.springapi.hospital.HospitalRepository;
import com.springapi.user.User;
import com.springapi.user.UserRepository;

class DevelopmentDataSeederTest {

    private HospitalRepository hospitalRepository;
    private DepartmentRepository departmentRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private Hospital hospital;

    @BeforeEach
    void setUp() {
        hospitalRepository = mock(HospitalRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        hospital = mock(Hospital.class);

        when(hospital.getId()).thenReturn(10L);
        when(hospitalRepository.findByRegistrationNumber("DEV-MQ-001"))
                .thenReturn(Optional.of(hospital));
        when(departmentRepository.findByHospitalIdAndCodeIgnoreCase(
                any(), any())).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUsername(any()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("development-only-password"))
                .thenReturn("encoded-development-password");
    }

    @Test
    void seedsDepartmentsAndSampleAccountsWithEncodedPasswords() {
        DevelopmentDataSeeder seeder = seeder("development-only-password");

        seeder.run(mock(ApplicationArguments.class));

        verify(departmentRepository, times(3))
                .save(any(Department.class));
        verify(userRepository, times(7)).save(any(User.class));
        verify(passwordEncoder, times(7))
                .encode("development-only-password");
    }

    @Test
    void refusesToSeedWithoutAConfiguredDevelopmentPassword() {
        DevelopmentDataSeeder seeder = seeder("");

        assertThrows(IllegalStateException.class,
                () -> seeder.run(mock(ApplicationArguments.class)));

        verify(hospitalRepository,
                org.mockito.Mockito.never())
                .findByRegistrationNumber(any());
    }

    private DevelopmentDataSeeder seeder(String password) {
        return new DevelopmentDataSeeder(
                hospitalRepository,
                departmentRepository,
                userRepository,
                passwordEncoder,
                password);
    }
}
