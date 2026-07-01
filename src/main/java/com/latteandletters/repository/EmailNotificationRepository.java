package com.latteandletters.repository;

import com.latteandletters.model.EmailNotification;
import com.latteandletters.model.EmailNotificationStatus;
import com.latteandletters.model.EmailNotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {

    List<EmailNotification> findTop25ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(EmailNotificationStatus status,
                                                                                               LocalDateTime scheduledAt);

    Optional<EmailNotification> findTopByUser_IdAndNotificationTypeAndSubjectOrderByCreatedAtDesc(Long userId,
                                                                                                   EmailNotificationType notificationType,
                                                                                                   String subject);

    Optional<EmailNotification> findByUser_IdAndNotificationTypeAndSubjectAndStatus(Long userId,
                                                                                     EmailNotificationType notificationType,
                                                                                     String subject,
                                                                                     EmailNotificationStatus status);
}
