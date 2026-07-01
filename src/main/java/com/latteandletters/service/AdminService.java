package com.latteandletters.service;

import com.latteandletters.config.LegacyAwarePasswordEncoder;
import com.latteandletters.model.Admin;
import com.latteandletters.model.Role;
import com.latteandletters.model.User;
import com.latteandletters.repository.AdminRepository;
import com.latteandletters.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private static final java.util.regex.Pattern PASSWORD_PATTERN = java.util.regex.Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{12,100}$");

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final LegacyAwarePasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AdminService(UserRepository userRepository,
                        AdminRepository adminRepository,
                        LegacyAwarePasswordEncoder passwordEncoder,
                        AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public User getAdminByEmail(String email) {
        Admin admin = adminRepository.findByUser_EmailIgnoreCase(required(email, "Admin account not found."))
                .orElseThrow(() -> new IllegalArgumentException("Admin account profile not found."));
        User user = admin.getUser();

        if (user == null || !Role.ADMIN.equals(user.getRole())) {
            throw new IllegalArgumentException("Selected user is not an admin account.");
        }

        return user;
    }

    @Transactional
    public User updateProfile(String email, String name) {
        User admin = getAdminByEmail(email);
        admin.setName(required(name, "Display name is required."));
        User savedAdmin = userRepository.save(admin);
        auditLogService.log(
                email,
                "ADMIN_PROFILE_UPDATED",
                "ADMIN_ACCOUNT",
                savedAdmin.getId().toString(),
                "Admin profile updated",
                "Display name changed to " + savedAdmin.getName()
        );
        return savedAdmin;
    }

    @Transactional
    public void changePassword(String email,
                               String currentPassword,
                               String newPassword,
                               String confirmPassword) {
        User admin = getAdminByEmail(email);
        String normalizedCurrentPassword = required(currentPassword, "Current password is required.");
        String normalizedNewPassword = required(newPassword, "New password is required.");
        String normalizedConfirmPassword = required(confirmPassword, "Please confirm the new password.");

        if (!passwordEncoder.matches(normalizedCurrentPassword, admin.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (normalizedNewPassword.length() < 12) {
            throw new IllegalArgumentException("New password must be at least 12 characters.");
        }
        if (!PASSWORD_PATTERN.matcher(normalizedNewPassword).matches()) {
            throw new IllegalArgumentException("New password must include uppercase, lowercase, number, and special character.");
        }
        if (!normalizedNewPassword.equals(normalizedConfirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }
        if (passwordEncoder.matches(normalizedNewPassword, admin.getPasswordHash())) {
            throw new IllegalArgumentException("Choose a different password from the current one.");
        }

        admin.setPasswordHash(passwordEncoder.encode(normalizedNewPassword));
        userRepository.save(admin);
        auditLogService.log(
                email,
                "ADMIN_PASSWORD_CHANGED",
                "ADMIN_ACCOUNT",
                admin.getId().toString(),
                "Admin password changed",
                "Password changed successfully for admin account."
        );
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
