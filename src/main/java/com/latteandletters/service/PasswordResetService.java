package com.latteandletters.service;

import com.latteandletters.config.LegacyAwarePasswordEncoder;
import com.latteandletters.dto.PasswordResetOtpDispatchResult;
import com.latteandletters.dto.PasswordResetOtpState;
import com.latteandletters.model.PasswordResetToken;
import com.latteandletters.model.User;
import com.latteandletters.repository.PasswordResetTokenRepository;
import com.latteandletters.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@SuppressWarnings("null")
public class PasswordResetService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{12,100}$");
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password",
            "password123",
            "12345678",
            "123456789",
            "qwerty123",
            "admin123",
            "welcome123",
            "letmein123",
            "iloveyou",
            "abc12345",
            "passw0rd",
            "student123",
            "adminadmin",
            "11111111",
            "12341234"
    );
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a", Locale.ENGLISH);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LegacyAwarePasswordEncoder passwordEncoder;
    private final EmailNotificationService emailNotificationService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final int resendCooldownSeconds;
    private final int otpValidityMinutes;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                LegacyAwarePasswordEncoder passwordEncoder,
                                EmailNotificationService emailNotificationService,
                                @Value("${latteandletters.password-reset-otp.resend-seconds:180}") int resendCooldownSeconds,
                                @Value("${latteandletters.password-reset-otp.validity-minutes:10}") int otpValidityMinutes) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNotificationService = emailNotificationService;
        this.resendCooldownSeconds = Math.max(30, resendCooldownSeconds);
        this.otpValidityMinutes = Math.max(3, otpValidityMinutes);
    }

    public PasswordResetOtpState getActiveOtpState(String email) {
        User user = findUserByEmail(email);
        return passwordResetTokenRepository.findFirstByUser_IdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .filter(token -> token.getExpiresAt() != null && token.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::toState)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Long verifyOtp(String email, String otpCode) {
        User user = findUserByEmail(email);
        PasswordResetToken latestToken = passwordResetTokenRepository.findFirstByUser_IdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Request a password reset OTP first."));

        if (latestToken.getExpiresAt() == null || !latestToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("The OTP has expired. Request a new code to continue.");
        }

        if (otpCode == null || otpCode.trim().isBlank()) {
            throw new IllegalArgumentException("Enter the 6-digit OTP.");
        }
        if (!hashOtp(otpCode.trim()).equals(latestToken.getToken())) {
            throw new IllegalArgumentException("Invalid OTP. Please try again.");
        }

        return latestToken.getId();
    }

    @Transactional
    public PasswordResetOtpDispatchResult requestOtp(String email) {
        User user = findUserByEmail(email);
        PasswordResetToken latestToken = passwordResetTokenRepository.findFirstByUser_IdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElse(null);
        LocalDateTime now = LocalDateTime.now();

        if (latestToken != null
                && latestToken.getExpiresAt() != null
                && latestToken.getExpiresAt().isAfter(now)
                && latestToken.getCreatedAt() != null
                && latestToken.getCreatedAt().plusSeconds(resendCooldownSeconds).isAfter(now)) {
            return new PasswordResetOtpDispatchResult(toState(latestToken), false, false, true);
        }

        expireOutstandingTokens(user.getId());

        String otpCode = generateOtpCode();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(hashOtp(otpCode));
        token.setExpiresAt(now.plusMinutes(otpValidityMinutes));
        token.setUsed(false);

        PasswordResetToken savedToken = passwordResetTokenRepository.save(token);
        boolean delivered = emailNotificationService.sendImmediateHtmlEmail(
                user.getEmail(),
                "Latte and Letters Verification Code | Password Reset",
                buildPasswordResetEmailBody(user, otpCode, savedToken)
        );

        return new PasswordResetOtpDispatchResult(toState(savedToken), true, delivered, false);
    }

    @Transactional
    public PasswordResetOtpDispatchResult resendOtp(String email) {
        User user = findUserByEmail(email);
        PasswordResetToken latestToken = passwordResetTokenRepository.findFirstByUser_IdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Request a password reset OTP first."));

        if (latestToken.getExpiresAt() == null || !latestToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            expireOutstandingTokens(user.getId());
            throw new IllegalArgumentException("The last OTP has expired. Request a new password reset OTP.");
        }

        return requestOtp(email);
    }

    @Transactional
    public void resetPassword(String email,
                              String otpCode,
                              String newPassword,
                              String confirmPassword) {
        User user = findUserByEmail(email);
        PasswordResetToken latestToken = passwordResetTokenRepository.findFirstByUser_IdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Request a password reset OTP first."));

        if (latestToken.getExpiresAt() == null || !latestToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("The OTP has expired. Request a new code to continue.");
        }

        if (otpCode == null || otpCode.trim().isBlank()) {
            throw new IllegalArgumentException("Enter the 6-digit OTP.");
        }
        if (!hashOtp(otpCode.trim()).equals(latestToken.getToken())) {
            throw new IllegalArgumentException("Invalid OTP. Please try again.");
        }

        String normalizedPassword = validateNewPassword(user, newPassword, confirmPassword);
        user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        latestToken.setUsed(true);
        passwordResetTokenRepository.save(latestToken);
        expireOutstandingTokens(user.getId());
    }

    @Transactional
    public void updatePasswordWithVerifiedOtp(String email,
                                              Long tokenId,
                                              String newPassword,
                                              String confirmPassword) {
        User user = findUserByEmail(email);
        if (tokenId == null) {
            throw new IllegalArgumentException("Verify the OTP first before changing your password.");
        }

        PasswordResetToken token = passwordResetTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("The verified OTP session is no longer available."));

        if (token.isUsed() || token.getUser() == null || !token.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Verify a new OTP before changing your password.");
        }
        if (token.getExpiresAt() == null || !token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("The verified OTP has expired. Request a new code to continue.");
        }

        String normalizedPassword = validateNewPassword(user, newPassword, confirmPassword);
        user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        expireOutstandingTokens(user.getId());
    }

    private User findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        return userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("No account was found for that email address."));
    }

    private void expireOutstandingTokens(Long userId) {
        List<PasswordResetToken> tokens = passwordResetTokenRepository.findByUser_IdAndUsedFalseOrderByCreatedAtDesc(userId);
        boolean changed = false;
        for (PasswordResetToken token : tokens) {
            if (!token.isUsed()) {
                token.setUsed(true);
                changed = true;
            }
        }
        if (changed) {
            passwordResetTokenRepository.saveAll(tokens);
        }
    }

    private PasswordResetOtpState toState(PasswordResetToken token) {
        return new PasswordResetOtpState(
                token.getUser().getEmail(),
                maskEmail(token.getUser().getEmail()),
                token.getExpiresAt(),
                token.getCreatedAt() == null ? null : token.getCreatedAt().plusSeconds(resendCooldownSeconds)
        );
    }

    private String validateNewPassword(User user,
                                       String newPassword,
                                       String confirmPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required.");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new IllegalArgumentException("Confirm password is required.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password and confirmation do not match.");
        }
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new IllegalArgumentException("Password must include uppercase, lowercase, number, and special character, with at least 12 characters.");
        }
        if (COMMON_PASSWORDS.contains(newPassword.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("That password is too common. Please choose a stronger one.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Choose a different password from your current one.");
        }

        String emailLocalPart = user.getEmail().substring(0, user.getEmail().indexOf('@')).toLowerCase(Locale.ROOT);
        String normalizedPassword = newPassword.toLowerCase(Locale.ROOT);
        if (emailLocalPart.length() >= 3 && normalizedPassword.contains(emailLocalPart)) {
            throw new IllegalArgumentException("Password must not contain your email name.");
        }

        String[] nameTokens = user.getName().toLowerCase(Locale.ROOT).split("\\s+");
        for (String token : nameTokens) {
            String normalizedToken = token.replaceAll("[^a-z0-9]", "");
            if (normalizedToken.length() >= 3 && normalizedPassword.contains(normalizedToken)) {
                throw new IllegalArgumentException("Password must not contain your personal name.");
            }
        }

        return newPassword;
    }

    private String buildPasswordResetEmailBody(User user, String otpCode, PasswordResetToken token) {
        return """
                <div style="margin:0;padding:24px;background:#f4faf6;font-family:Segoe UI,Arial,sans-serif;color:#163322;">
                  <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #d5eadc;border-radius:24px;overflow:hidden;box-shadow:0 18px 44px rgba(18,77,47,0.12);">
                    <div style="padding:24px 32px;background:linear-gradient(135deg,#0f7a36,#34c66a);color:#ffffff;">
                      <div style="font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.88;">Latte and Letters</div>
                      <h1 style="margin:10px 0 4px;font-size:28px;line-height:1.2;">Password Reset Verification</h1>
                      <p style="margin:0;font-size:15px;opacity:0.92;">Use this one-time verification code to continue resetting your Latte and Letters account password.</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="margin:0 0 16px;font-size:15px;line-height:1.7;">Hello %s,</p>
                      <p style="margin:0 0 20px;font-size:15px;line-height:1.7;">We received a password reset request for your Latte and Letters account. Enter the code below on the reset password page to continue. For your security, do not share this code with anyone.</p>
                      <div style="margin:0 0 24px;padding:18px 20px;border-radius:20px;background:#effaf2;border:1px solid #c9ead1;text-align:center;">
                        <div style="font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;color:#2f7d49;">Verification Code</div>
                        <div style="margin-top:8px;font-size:34px;font-weight:800;letter-spacing:0.34em;color:#0f7a36;">%s</div>
                      </div>
                      <div style="margin:0 0 24px;padding:20px;border-radius:18px;background:#fbfefd;border:1px solid #e0efe4;">
                        <div style="font-size:15px;font-weight:700;color:#18452d;margin-bottom:12px;">Request Details</div>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;line-height:1.6;">
                          <tr><td style="padding:6px 0;color:#5f7b69;">Registered Email</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#5f7b69;">Code Expires</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#5f7b69;">Resend Available</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                        </table>
                      </div>
                      <div style="padding:16px 18px;border-radius:16px;background:#fff8ea;border:1px solid #f1ddb1;color:#6b5112;font-size:13px;line-height:1.7;">
                        If you did not request this password reset, ignore this email and keep your account secure. Your password will remain unchanged unless the correct verification code is entered before it expires.
                      </div>
                    </div>
                    <div style="padding:18px 32px;background:#f6fbf7;border-top:1px solid #e1efe5;font-size:12px;line-height:1.7;color:#6c8375;">
                      This is an automated message from Latte and Letters. Please do not reply to this email.
                    </div>
                  </div>
                </div>
                """.formatted(
                escapeHtml(user.getName()),
                escapeHtml(otpCode),
                escapeHtml(user.getEmail()),
                escapeHtml(DATE_TIME_FORMATTER.format(token.getExpiresAt())),
                escapeHtml(DATE_TIME_FORMATTER.format(token.getCreatedAt().plusSeconds(resendCooldownSeconds)))
        );
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String generateOtpCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String hashOtp(String otpCode) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(messageDigest.digest(otpCode.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash OTP code.", exception);
        }
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "your registered email";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domain;
    }
}
