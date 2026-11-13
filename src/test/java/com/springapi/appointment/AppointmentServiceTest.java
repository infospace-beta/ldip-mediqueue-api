package com.springapi.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springapi.common.exception.ConflictException;
import com.springapi.department.Department;
import com.springapi.department.DepartmentRepository;
import com.springapi.department.DepartmentStatus;
import com.springapi.appointment.dto.AppointmentRequest;
import com.springapi.appointment.dto.AppointmentResponse;
import com.springapi.hospital.Hospital;
import com.springapi.hospital.HospitalStatus;
import com.springapi.notification.NotificationService;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

class AppointmentServiceTest {

    private AppointmentRepository appointmentRepository;
    private DepartmentRepository departmentRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);
        appointmentService = new AppointmentService(
                appointmentRepository,
                departmentRepository,
                userRepository,
                notificationService);
    }

    @Test
    void booksFutureSlotWithOnDutyDoctorAtSameHospital() {
        Instant startsAt = Instant.now().plusSeconds(3600);
        AppointmentRequest request = new AppointmentRequest(
                10L, 3L, startsAt, "Consultation");
        User patient = user(UserRole.PATIENT, null);
        Hospital hospital = hospital();
        User doctor = user(UserRole.DOCTOR, hospital);
        Department department = department(hospital);
        when(userRepository.findByUsername("patient"))
                .thenReturn(Optional.of(patient));
        when(userRepository.findByIdAndRole(10L, UserRole.DOCTOR))
                .thenReturn(Optional.of(doctor));
        when(departmentRepository.findById(3L))
                .thenReturn(Optional.of(department));
        when(appointmentRepository.existsByDoctorIdAndStartsAtAndStatus(
                10L, startsAt, AppointmentStatus.BOOKED))
                .thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.book("patient", request);

        assertEquals(startsAt, response.startsAt());
        assertEquals("Dr. Ada", response.doctorName());
        assertEquals("Cardiology", response.departmentName());
        verify(appointmentRepository).saveAndFlush(any(Appointment.class));
    }

    @Test
    void rejectsAlreadyBookedDoctorSlot() {
        Instant startsAt = Instant.now().plusSeconds(3600);
        AppointmentRequest request = new AppointmentRequest(
                10L, 3L, startsAt, "Consultation");
        Hospital hospital = hospital();
        User patient = user(UserRole.PATIENT, null);
        User doctor = user(UserRole.DOCTOR, hospital);
        Department department = department(hospital);
        when(userRepository.findByUsername("patient"))
                .thenReturn(Optional.of(patient));
        when(userRepository.findByIdAndRole(10L, UserRole.DOCTOR))
                .thenReturn(Optional.of(doctor));
        when(departmentRepository.findById(3L))
                .thenReturn(Optional.of(department));
        when(appointmentRepository.existsByDoctorIdAndStartsAtAndStatus(
                10L, startsAt, AppointmentStatus.BOOKED))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> appointmentService.book("patient", request));

        verify(appointmentRepository, never())
                .saveAndFlush(any(Appointment.class));
    }

    @Test
    void rejectsDoctorWhoIsNotOnDuty() {
        Hospital hospital = hospital();
        User doctor = user(UserRole.DOCTOR, hospital);
        User patient = user(UserRole.PATIENT, null);
        Department department = department(hospital);
        when(doctor.isOnDuty()).thenReturn(false);
        when(userRepository.findByUsername("patient"))
                .thenReturn(Optional.of(patient));
        when(userRepository.findByIdAndRole(10L, UserRole.DOCTOR))
                .thenReturn(Optional.of(doctor));
        when(departmentRepository.findById(3L))
                .thenReturn(Optional.of(department));
        AppointmentRequest request = new AppointmentRequest(
                10L, 3L, Instant.now().plusSeconds(3600), "Consultation");

        assertThrows(
                ConflictException.class,
                () -> appointmentService.book("patient", request));

        verify(appointmentRepository, never())
                .saveAndFlush(any(Appointment.class));
    }

    private User user(UserRole role, Hospital hospital) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(role == UserRole.DOCTOR ? 10L : 7L);
        when(user.getRole()).thenReturn(role);
        when(user.getHospital()).thenReturn(hospital);
        when(user.isEnabled()).thenReturn(true);
        when(user.isOnDuty()).thenReturn(true);
        when(user.getFullName()).thenReturn(
                role == UserRole.DOCTOR ? "Dr. Ada" : "Patient");
        return user;
    }

    private Hospital hospital() {
        Hospital hospital = mock(Hospital.class);
        when(hospital.getId()).thenReturn(4L);
        when(hospital.getName()).thenReturn("MediQueue Hospital");
        when(hospital.getStatus()).thenReturn(HospitalStatus.ACTIVE);
        return hospital;
    }

    private Department department(Hospital hospital) {
        Department department = mock(Department.class);
        when(department.getHospital()).thenReturn(hospital);
        when(department.getStatus()).thenReturn(DepartmentStatus.ACTIVE);
        when(department.getName()).thenReturn("Cardiology");
        return department;
    }
}
