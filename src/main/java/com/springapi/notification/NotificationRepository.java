package com.springapi.notification;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByRecipientUsernameOrderByCreatedAtDesc(
            String username,
            Pageable pageable);

    Optional<Notification> findByIdAndRecipientUsername(
            Long notificationId,
            String username);

    @Modifying
    @Query("""
            update Notification n
            set n.readAt = :readAt
            where n.recipient.username = :username
              and n.readAt is null
            """)
    int markAllRead(
            @Param("username") String username,
            @Param("readAt") java.time.Instant readAt);
}
