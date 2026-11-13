package com.springapi.clinical;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalRecordRepository
        extends JpaRepository<ClinicalRecord, Long> {

    Page<ClinicalRecord> findAllByPatientIdOrderByCreatedAtDesc(
            Long patientId,
            Pageable pageable);

    boolean existsByQueueTicketId(Long queueTicketId);
}
