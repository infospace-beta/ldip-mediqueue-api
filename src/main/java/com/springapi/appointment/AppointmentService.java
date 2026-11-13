package com.springapi.appointment;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.appointment.dto.AppointmentRequest;
import com.springapi.appointment.dto.AppointmentResponse;
import com.springapi.common.dto.PageResponse;
import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.department.Department;
import com.springapi.department.DepartmentRepository;
import com.springapi.department.DepartmentStatus;
import com.springapi.notification.NotificationKind;
import com.springapi.notification.NotificationService;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> getPatientAppointments(
            Long patientId,
            Pageable pageable) {
        requirePatient(patientId);
        return PageResponse.from(appointmentRepository
                .findAllByPatientId(patientId, pageable)
                .map(AppointmentResponse::from));
    }

    public AppointmentResponse book(
            String username,
            AppointmentRequest request) {
        User patient = userRepository.findByUsername(username)
                .filter(user -> user.getRole() == UserRole.PATIENT)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Patient", username));
        User doctor = userRepository
                .findByIdAndRole(request.doctorId(), UserRole.DOCTOR)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Doctor", request.doctorId()));
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department", request.departmentId()));

        if (department.getStatus() != DepartmentStatus.ACTIVE
                || department.getHospital().getStatus()
                        != com.springapi.hospital.HospitalStatus.ACTIVE
                || doctor.getHospital() == null
                || !doctor.getHospital().getId()
                        .equals(department.getHospital().getId())
                || !doctor.isEnabled()
                || !doctor.isOnDuty()) {
            throw new ConflictException(
                    "Choose an active department and an on-duty doctor at the same hospital.");
        }
        if (!request.startsAt().isAfter(Instant.now())) {
            throw new ConflictException(
                    "Appointment start time must be in the future.");
        }
        if (appointmentRepository.existsByDoctorIdAndStartsAtAndStatus(
                doctor.getId(), request.startsAt(), AppointmentStatus.BOOKED)) {
            throw new ConflictException(
                    "That doctor already has a booking at the selected time.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setDepartment(department);
        appointment.setStartsAt(request.startsAt());
        appointment.setReason(request.reason().trim());
        Appointment saved = appointmentRepository.saveAndFlush(appointment);

        String time = request.startsAt().toString();
        notificationService.create(
                patient,
                NotificationKind.APPOINTMENT,
                "Appointment booked",
                "Your appointment with %s is booked for %s."
                        .formatted(doctor.getFullName(), time));
        notificationService.create(
                doctor,
                NotificationKind.APPOINTMENT,
                "New appointment",
                "%s booked an appointment for %s."
                        .formatted(patient.getFullName(), time));
        return AppointmentResponse.from(saved);
    }

    public void cancel(Long appointmentId, String username) {
        Appointment appointment = appointmentRepository
                .findByIdAndPatientUsername(appointmentId, username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment", appointmentId));
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ConflictException(
                    "Only a booked appointment can be cancelled.");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        String cancellationMessage = "%s cancelled an appointment for %s."
                .formatted(
                        appointment.getPatient().getFullName(),
                        appointment.getStartsAt());
        notificationService.create(
                appointment.getPatient(),
                NotificationKind.APPOINTMENT,
                "Appointment cancelled",
                "Your appointment for %s was cancelled."
                        .formatted(appointment.getStartsAt()));
        notificationService.create(
                appointment.getDoctor(),
                NotificationKind.APPOINTMENT,
                "Appointment cancelled",
                cancellationMessage);
    }

    private User requirePatient(Long patientId) {
        return userRepository.findByIdAndRole(patientId, UserRole.PATIENT)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Patient", patientId));
    }
}
