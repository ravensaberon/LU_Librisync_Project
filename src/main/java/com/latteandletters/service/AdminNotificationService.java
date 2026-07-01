package com.latteandletters.service;

import com.latteandletters.model.AdminNotification;
import com.latteandletters.model.AdminNotificationType;
import com.latteandletters.model.Admin;
import com.latteandletters.model.User;
import com.latteandletters.repository.AdminRepository;
import com.latteandletters.repository.AdminNotificationRepository;
import com.latteandletters.repository.UserRepository;
import com.latteandletters.util.PaginationSlice;
import com.latteandletters.util.PaginationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class AdminNotificationService {

    private final AdminNotificationRepository adminNotificationRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;

    public AdminNotificationService(AdminNotificationRepository adminNotificationRepository,
                                    AdminRepository adminRepository,
                                    UserRepository userRepository) {
        this.adminNotificationRepository = adminNotificationRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void notifyAdmins(AdminNotificationType notificationType,
                             String title,
                             String message,
                             String linkUrl) {
        List<Admin> admins = adminRepository.findAll();
        for (Admin admin : admins) {
            if (admin.getUser() != null) {
                createNotification(admin.getUser(), notificationType, title, message, linkUrl, false);
            }
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
                .findTopByUser_EmailIgnoreCaseAndNotificationTypeAndTitleAndMessageOrderByCreatedAtDesc(
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
        List<AdminNotification> notifications = adminNotificationRepository.findByUser_EmailIgnoreCaseOrderByCreatedAtDesc(userEmail);
        return notifications.stream()
                .limit(Math.max(1, limit))
                .toList();
    }

    public long countUnreadNotifications(String userEmail) {
        ensureUserExists(userEmail);
        return adminNotificationRepository.countByUser_EmailIgnoreCaseAndReadFalse(userEmail);
    }

    @Transactional
    public void markAllAsRead(String userEmail) {
        ensureUserExists(userEmail);
        List<AdminNotification> unreadNotifications = adminNotificationRepository
                .findByUser_EmailIgnoreCaseAndReadFalseOrderByCreatedAtDesc(userEmail);
        for (AdminNotification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        adminNotificationRepository.saveAll(unreadNotifications);
    }

    public List<AdminNotification> getAllNotifications(String userEmail) {
        ensureUserExists(userEmail);
        return adminNotificationRepository.findByUser_EmailIgnoreCaseOrderByCreatedAtDesc(userEmail);
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
        notification.setUser(user);
        notification.setNotificationType(notificationType);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLinkUrl(linkUrl);
        notification.setRead(read);
        notification.setReadAt(read ? LocalDateTime.now() : null);
        adminNotificationRepository.save(notification);
    }
}
