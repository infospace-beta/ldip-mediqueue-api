package com.springapi.queue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface QueueTicketRepository
        extends JpaRepository<QueueTicket, Long> {

    boolean existsByPatientIdAndDepartmentIdAndStatusIn(
            Long patientId,
            Long departmentId,
            Collection<QueueStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QueueTicket> findFirstByDepartmentIdAndStatusOrderByCreatedAtAsc(
            Long departmentId,
            QueueStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QueueTicket> findByIdAndDepartmentId(Long id, Long departmentId);

    @Query("select t from QueueTicket t where t.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QueueTicket> findLockedById(@Param("id") Long id);

    Optional<QueueTicket> findByIdAndPatientUsername(Long id, String username);

    @Query("""
            select case when count(t) > 0 then true else false end
            from QueueTicket t
            where t.patient.id = :patientId
              and t.department.hospital.id = :hospitalId
            """)
    boolean existsByPatientIdAndHospitalId(
            @Param("patientId") Long patientId,
            @Param("hospitalId") Long hospitalId);

    @Query("""
            select t from QueueTicket t
            where t.patient.id = :patientId
              and (:status is null or t.status = :status)
            """)
    Page<QueueTicket> findPatientHistory(
            @Param("patientId") Long patientId,
            @Param("status") QueueStatus status,
            Pageable pageable);

    List<QueueTicket> findAllByPatientIdOrderByCreatedAtDesc(Long patientId);

    @Query("""
            select t from QueueTicket t
            where t.department.id = :departmentId
              and (:status is null or t.status = :status)
            order by t.createdAt asc
            """)
    Page<QueueTicket> findDepartmentQueue(
            @Param("departmentId") Long departmentId,
            @Param("status") QueueStatus status,
            Pageable pageable);

    @Query("""
            select count(t) from QueueTicket t
            where t.department.hospital.id = :hospitalId
              and t.status = :status
            """)
    long countByHospitalAndStatus(
            @Param("hospitalId") Long hospitalId,
            @Param("status") QueueStatus status);

    @Query(value = """
            select avg(extract(epoch from (called_at - created_at)) / 60.0)
            from queue_tickets t
            join departments d on d.id = t.department_id
            where d.hospital_id = :hospitalId
              and t.called_at is not null
            """, nativeQuery = true)
    Double findAverageWaitMinutesByHospitalId(
            @Param("hospitalId") Long hospitalId);
}
