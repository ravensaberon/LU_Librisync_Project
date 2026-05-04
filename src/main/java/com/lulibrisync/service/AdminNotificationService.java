package com.lulibrisync.service;

import com.lulibrisync.model.AdminNotification;
import com.lulibrisync.model.AdminNotificationType;
import com.lulibrisync.model.Role;
import com.lulibrisync.model.User;
import com.lulibrisync.repository.AdminNotificationRepository;
import com.lulibrisync.repository.UserRepository;
import com.lulibrisync.util.PaginationSlice;
import com.lulibrisync.util.PaginationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class AdminNotificationService {

    private final AdminNotificationRepository adminNotificationRepository;
    private final UserRepository userRepository;

    public AdminNotificationService(AdminNotificationRepository adminNotificationRepository,
                                    UserRepository userRepository) {
        this.adminNotificationRepository = adminNotificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void notifyAdmins(AdminNotificationType notificationType,
                             String title,
                             String message,
                             String linkUrl) {
        List<User> admins = userRepository.findAllByRole(Role.ADMIN);
        for (User admin : admins) {
            createNotification(admin, notificationType, title, message, linkUrl, false);
        }
    }

    @Transactional
    public void notifyUser(String userEmail,
                           AdminNotificationType notificationType,
                           String title,
                           String message,
                           String linkUrl) {
        createNotification(getUserByEmail(userEmail), notificationType, title, message, linkUrl, false);
    }

    @Transactional
    public void notifyUserIfAbsent(String userEmail,
                                   AdminNotificationType notificationType,
                                   String title,
                                   String message,
                                   String linkUrl) {
        User user = getUserByEmail(userEmail);
        Optional<AdminNotification> latest = adminNotificationRepository
                .findTopByAdminUser_EmailIgnoreCaseAndNotificationTypeAndTitleAndMessageOrderByCreatedAtDesc(
                        user.getEmail(),
                        notificationType,
                        title,
                        message
                );
        if (latest.isPresent() && latest.get().getCreatedAt() != null
                && latest.get().getCreatedAt().isAfter(LocalDateTime.now().minusHours(12))) {
            return;
        }
        createNotification(user, notificationType, title, message, linkUrl, false);
    }

    public List<AdminNotification> getRecentNotifications(String adminEmail) {
        return getRecentNotifications(adminEmail, 10);
    }

    public List<AdminNotification> getRecentNotifications(String userEmail, int limit) {
        ensureUserExists(userEmail);
        List<AdminNotification> notifications = adminNotificationRepository.findByAdminUser_EmailIgnoreCaseOrderByCreatedAtDesc(userEmail);
        return notifications.stream()
                .limit(Math.max(1, limit))
                .toList();
    }

    public long countUnreadNotifications(String userEmail) {
        ensureUserExists(userEmail);
        return adminNotificationRepository.countByAdminUser_EmailIgnoreCaseAndReadFalse(userEmail);
    }

    @Transactional
    public void markAllAsRead(String userEmail) {
        ensureUserExists(userEmail);
        List<AdminNotification> unreadNotifications = adminNotificationRepository
                .findByAdminUser_EmailIgnoreCaseAndReadFalseOrderByCreatedAtDesc(userEmail);
        for (AdminNotification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        adminNotificationRepository.saveAll(unreadNotifications);
    }

    public List<AdminNotification> getAllNotifications(String userEmail) {
        ensureUserExists(userEmail);
        return adminNotificationRepository.findByAdminUser_EmailIgnoreCaseOrderByCreatedAtDesc(userEmail);
    }

    public PaginationSlice<AdminNotification> getNotificationPage(String userEmail, Integer page, int pageSize) {
        return PaginationUtils.paginate(getAllNotifications(userEmail), page, pageSize);
    }

    private void ensureUserExists(String userEmail) {
        getUserByEmail(userEmail);
    }

    private User getUserByEmail(String userEmail) {
        return userRepository.findByEmailIgnoreCase(userEmail == null ? "" : userEmail.trim())
                .orElseThrow(() -> new IllegalArgumentException("User account not found."));
    }

    private void createNotification(User user,
                                    AdminNotificationType notificationType,
                                    String title,
                                    String message,
                                    String linkUrl,
                                    boolean read) {
        AdminNotification notification = new AdminNotification();
        notification.setAdminUser(user);
        notification.setNotificationType(notificationType);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLinkUrl(linkUrl);
        notification.setRead(read);
        notification.setReadAt(read ? LocalDateTime.now() : null);
        adminNotificationRepository.save(notification);
    }
}
