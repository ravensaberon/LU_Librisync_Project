package com.lulibrisync.repository;

import com.lulibrisync.model.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {

    List<AdminNotification> findTop10ByAdminUser_EmailIgnoreCaseOrderByCreatedAtDesc(String email);

    List<AdminNotification> findTop10ByAdminUser_EmailIgnoreCaseAndReadFalseOrderByCreatedAtDesc(String email);

    List<AdminNotification> findByAdminUser_EmailIgnoreCaseOrderByCreatedAtDesc(String email);

    List<AdminNotification> findByAdminUser_EmailIgnoreCaseAndReadFalseOrderByCreatedAtDesc(String email);

    long countByAdminUser_EmailIgnoreCaseAndReadFalse(String email);

    Optional<AdminNotification> findByIdAndAdminUser_EmailIgnoreCase(Long id, String email);

    Optional<AdminNotification> findTopByAdminUser_EmailIgnoreCaseAndNotificationTypeAndTitleAndMessageOrderByCreatedAtDesc(String email,
                                                                                                                             com.lulibrisync.model.AdminNotificationType notificationType,
                                                                                                                             String title,
                                                                                                                             String message);
}
