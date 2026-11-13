package com.springapi.emergency;

import java.util.Optional;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmergencyCaseRepository
        extends JpaRepository<EmergencyCase, Long> {

    @Query("""
            select c from EmergencyCase c
            where c.hospital.id = :hospitalId
              and (:status is null or c.status = :status)
              and (:severity is null or c.severity = :severity)
            order by c.createdAt desc
            """)
    Page<EmergencyCase> findFiltered(
            @Param("hospitalId") Long hospitalId,
            @Param("status") EmergencyStatus status,
            @Param("severity") EmergencySeverity severity,
            Pageable pageable);

    @Query("""
            select c from EmergencyCase c
            where c.patient.id = :patientId
            order by c.createdAt desc
            """)
    Page<EmergencyCase> findForPatient(
            @Param("patientId") Long patientId,
            Pageable pageable);

    @Query("""
            select c from EmergencyCase c
            where c.assignedResponder.username = :username
              and c.status in :statuses
            order by c.createdAt desc
            """)
    Page<EmergencyCase> findAssignedToResponder(
            @Param("username") String username,
            @Param("statuses") Collection<EmergencyStatus> statuses,
            Pageable pageable);
    Optional<EmergencyCase> findByHospitalIdAndId(Long hospitalId, Long id);

    @Query("""
            select case when count(c) > 0 then true else false end
            from EmergencyCase c
            where c.hospital.id = :hospitalId
              and c.id = :caseId
              and c.reportedBy.username = :username
            """)
    boolean existsReportedBy(
            @Param("hospitalId") Long hospitalId,
            @Param("caseId") Long caseId,
            @Param("username") String username);

    @Query("""
            select case when count(c) > 0 then true else false end
            from EmergencyCase c
            where c.hospital.id = :hospitalId
              and c.id = :caseId
              and c.assignedResponder.username = :username
            """)
    boolean existsAssignedResponder(
            @Param("hospitalId") Long hospitalId,
            @Param("caseId") Long caseId,
            @Param("username") String username);

    long countByHospitalIdAndStatusIn(
            Long hospitalId,
            Collection<EmergencyStatus> statuses);

    long countByAssignedResponderUsernameAndStatusIn(
            String username,
            Collection<EmergencyStatus> statuses);

    long countByAssignedResponderUsernameAndStatus(
            String username,
            EmergencyStatus status);
}
