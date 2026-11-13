package com.springapi.appointment;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    Page<Appointment> findAllByPatientId(
            Long patientId,
            Pageable pageable);

    boolean existsByDoctorIdAndStartsAtAndStatus(
            Long doctorId,
            Instant startsAt,
            AppointmentStatus status);

    Optional<Appointment> findByIdAndPatientUsername(
            Long appointmentId,
            String username);
}
