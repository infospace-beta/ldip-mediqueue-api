package com.springapi.notification;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.common.dto.PageResponse;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.notification.dto.NotificationResponse;
import com.springapi.user.User;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void create(
            User recipient,
            NotificationKind kind,
            String title,
            String message) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setKind(kind);
        notification.setTitle(title);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getForUser(
            String username,
            Pageable pageable) {
        return PageResponse.from(notificationRepository
                .findAllByRecipientUsernameOrderByCreatedAtDesc(
                        username, pageable)
                .map(NotificationResponse::from));
    }

    public void markRead(Long notificationId, String username) {
        Notification notification = notificationRepository
                .findByIdAndRecipientUsername(notificationId, username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification", notificationId));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    public void markAllRead(String username) {
        notificationRepository.markAllRead(username, Instant.now());
    }
}
