package com.latteandletters.repository;

import com.latteandletters.model.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {

    List<AdminNotification> findTop10ByUser_EmailIgnoreCaseOrderByCreatedAtDesc(String email);

    List<AdminNotification> findTop10ByUser_EmailIgnoreCaseAndReadFalseOrderByCreatedAtDesc(String email);

    List<AdminNotification> findByUser_EmailIgnoreCaseOrderByCreatedAtDesc(String email);

    List<AdminNotification> findByUser_EmailIgnoreCaseAndReadFalseOrderByCreatedAtDesc(String email);

    long countByUser_EmailIgnoreCaseAndReadFalse(String email);

    Optional<AdminNotification> findByIdAndUser_EmailIgnoreCase(Long id, String email);

    Optional<AdminNotification> findTopByUser_EmailIgnoreCaseAndNotificationTypeAndTitleAndMessageOrderByCreatedAtDesc(String email,
                                                                                                                         com.latteandletters.model.AdminNotificationType notificationType,
                                                                                                                         String title,
                                                                                                                         String message);
}
