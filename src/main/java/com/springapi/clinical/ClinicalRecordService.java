package com.springapi.clinical;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.clinical.dto.ClinicalRecordRequest;
import com.springapi.clinical.dto.ClinicalRecordResponse;
import com.springapi.common.dto.PageResponse;
import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.notification.NotificationKind;
import com.springapi.notification.NotificationService;
import com.springapi.queue.QueueStatus;
import com.springapi.queue.QueueTicket;
import com.springapi.queue.QueueTicketRepository;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

@Service
@Transactional
public class ClinicalRecordService {

    private final ClinicalRecordRepository clinicalRecordRepository;
    private final QueueTicketRepository queueTicketRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ClinicalRecordService(
            ClinicalRecordRepository clinicalRecordRepository,
            QueueTicketRepository queueTicketRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.clinicalRecordRepository = clinicalRecordRepository;
        this.queueTicketRepository = queueTicketRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClinicalRecordResponse> getPatientRecords(
            Long patientId,
            Pageable pageable) {
        requirePatient(patientId);
        return PageResponse.from(clinicalRecordRepository
                .findAllByPatientIdOrderByCreatedAtDesc(patientId, pageable)
                .map(ClinicalRecordResponse::from));
    }

    public ClinicalRecordResponse addRecord(
            Long queueTicketId,
            String username,
            ClinicalRecordRequest request) {
        QueueTicket ticket = queueTicketRepository.findById(queueTicketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Queue ticket", queueTicketId));
        if (ticket.getStatus() != QueueStatus.COMPLETED) {
            throw new ConflictException(
                    "A clinical record can only be added to a completed queue ticket.");
        }
        if (clinicalRecordRepository.existsByQueueTicketId(queueTicketId)) {
            throw new ConflictException(
                    "A clinical record already exists for this queue ticket.");
        }

        User author = userRepository.findByUsername(username)
                .filter(user -> user.getRole() == UserRole.DOCTOR
                        || user.getRole() == UserRole.MEDICAL_STAFF
                        || user.getRole() == UserRole.SYSTEM_ADMIN)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Clinical staff member", username));

        ClinicalRecord record = new ClinicalRecord();
        record.setQueueTicket(ticket);
        record.setPatient(ticket.getPatient());
        record.setAuthoredBy(author);
        record.setDiagnosis(request.diagnosis().trim());
        record.setTreatment(request.treatment().trim());
        record.setPrescription(
                request.prescription() == null
                        ? null : request.prescription().trim());
        ClinicalRecord saved = clinicalRecordRepository.saveAndFlush(record);
        notificationService.create(
                ticket.getPatient(),
                NotificationKind.CLINICAL,
                "Visit record added",
                "A clinical record for queue ticket #%d is available."
                        .formatted(queueTicketId));
        return ClinicalRecordResponse.from(saved);
    }

    private User requirePatient(Long patientId) {
        return userRepository.findByIdAndRole(patientId, UserRole.PATIENT)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Patient", patientId));
    }
}
