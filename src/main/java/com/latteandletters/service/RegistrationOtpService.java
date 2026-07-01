package com.latteandletters.service;

import com.latteandletters.dto.RegistrationOtpState;
import com.latteandletters.model.RegistrationOtpToken;
import com.latteandletters.model.User;
import com.latteandletters.model.UserStatus;
import com.latteandletters.repository.RegistrationOtpTokenRepository;
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

@Service
@SuppressWarnings("null")
public class RegistrationOtpService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a", Locale.ENGLISH);

    private final UserRepository userRepository;
    private final RegistrationOtpTokenRepository tokenRepository;
    private final EmailNotificationService emailNotificationService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final int resendCooldownSeconds;
    private final int otpValidityMinutes;

    public RegistrationOtpService(UserRepository userRepository,
                                  RegistrationOtpTokenRepository tokenRepository,
                                  EmailNotificationService emailNotificationService,
                                  @Value("${latteandletters.registration-otp.resend-seconds:180}") int resendCooldownSeconds,
                                  @Value("${latteandletters.registration-otp.validity-minutes:15}") int otpValidityMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailNotificationService = emailNotificationService;
        this.resendCooldownSeconds = Math.max(30, resendCooldownSeconds);
        this.otpValidityMinutes = Math.max(3, otpValidityMinutes);
    }

    /**
     * Called right after a new PENDING user is saved. Generates and sends the OTP.
     */
    @Transactional
    public RegistrationOtpState sendOtp(User user) {
        return sendOtp(user, null);
    }

    @Transactional
    public RegistrationOtpState sendOtp(User user, String temporaryPassword) {
        expireOutstandingTokens(user.getId());

        String otpCode = generateOtpCode();
        RegistrationOtpToken token = new RegistrationOtpToken();
        token.setUser(user);
        token.setToken(hashOtp(otpCode));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(otpValidityMinutes));
        token.setUsed(false);

        RegistrationOtpToken saved = tokenRepository.save(token);
        emailNotificationService.sendImmediateHtmlEmail(
                user.getEmail(),
                "Latte and Letters — Verify Your Email",
                buildVerificationEmailBody(user, otpCode, saved, temporaryPassword)
        );
        return toState(saved);
    }

    /**
     * Resend OTP — respects cooldown, expires old tokens first.
     * Returns the new state. Throws if cooldown is still active.
     */
    @Transactional
    public RegistrationOtpState resendOtp(String email) {
        User user = findPendingUserByEmail(email);
        RegistrationOtpToken latest = tokenRepository
                .findFirstByUser_IdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();
        if (latest != null
                && latest.getExpiresAt() != null
                && latest.getExpiresAt().isAfter(now)
                && latest.getCreatedAt() != null
                && latest.getCreatedAt().plusSeconds(resendCooldownSeconds).isAfter(now)) {
            throw new IllegalArgumentException("Please wait before requesting another OTP.");
        }

        return sendOtp(user);
    }

    /**
     * Verifies the OTP and activates the user account.
     */
    @Transactional
    public void verifyAndActivate(String email, String otpCode) {
        User user = findPendingUserByEmail(email);

        RegistrationOtpToken token = tokenRepository
                .findFirstByUser_IdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No active verification code found. Please request a new one."));

        if (token.getExpiresAt() == null || !token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("The verification code has expired. Please request a new one.");
        }

        if (otpCode == null || otpCode.isBlank()) {
            throw new IllegalArgumentException("Enter the 6-digit verification code.");
        }
        if (!hashOtp(otpCode.trim()).equals(token.getToken())) {
            throw new IllegalArgumentException("Invalid verification code. Please try again.");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
        expireOutstandingTokens(user.getId());
    }

    /**
     * Returns the active OTP state for a pending user, or null if none.
     */
    public RegistrationOtpState getActiveOtpState(String email) {
        User user = userRepository.findByEmailIgnoreCase(email == null ? "" : email.trim()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.PENDING) {
            return null;
        }
        return tokenRepository.findFirstByUser_IdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .filter(t -> t.getExpiresAt() != null && t.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::toState)
                .orElse(null);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User findPendingUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("No account found for that email address."));
        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalArgumentException("This account is already verified or is not awaiting verification.");
        }
        return user;
    }

    private void expireOutstandingTokens(Long userId) {
        List<RegistrationOtpToken> tokens =
                tokenRepository.findByUser_IdAndUsedFalseOrderByCreatedAtDesc(userId);
        for (RegistrationOtpToken t : tokens) {
            t.setUsed(true);
        }
        if (!tokens.isEmpty()) {
            tokenRepository.saveAll(tokens);
        }
    }

    private RegistrationOtpState toState(RegistrationOtpToken token) {
        return new RegistrationOtpState(
                token.getUser().getEmail(),
                maskEmail(token.getUser().getEmail()),
                token.getExpiresAt(),
                token.getCreatedAt() == null
                        ? null
                        : token.getCreatedAt().plusSeconds(resendCooldownSeconds)
        );
    }

    private String generateOtpCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String hashOtp(String otpCode) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(otpCode.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash OTP code.", e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return "your registered email";
        int at = email.indexOf('@');
        if (at <= 1) return email;
        String local = email.substring(0, at);
        String domain = email.substring(at);
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String buildVerificationEmailBody(User user, String otpCode, RegistrationOtpToken token, String temporaryPassword) {
        String tempPwSection = "";
        if (temporaryPassword != null && !temporaryPassword.isBlank()) {
            tempPwSection = """
                      <div style="margin:0 0 24px;padding:18px 20px;border-radius:20px;background:#fffbea;border:1px solid #f1ddb1;">
                        <div style="font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;color:#7c4a00;margin-bottom:8px;">Your Temporary Password</div>
                        <div style="font-family:monospace;font-size:22px;font-weight:800;letter-spacing:0.18em;color:#7c4a00;word-break:break-all;">%s</div>
                        <div style="margin-top:8px;font-size:12px;color:#7c4a00;opacity:0.85;">Use this password for your first sign-in, then set a new personal password immediately when the system asks you on first login.</div>
                      </div>
                    """.formatted(escapeHtml(temporaryPassword));
        }
        return """
                <div style="margin:0;padding:24px;background:#f4faf6;font-family:Segoe UI,Arial,sans-serif;color:#163322;">
                  <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #d5eadc;border-radius:24px;overflow:hidden;box-shadow:0 18px 44px rgba(18,77,47,0.12);">
                    <div style="padding:24px 32px;background:linear-gradient(135deg,#0f7a36,#34c66a);color:#ffffff;">
                      <div style="font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.88;">Latte and Letters</div>
                      <h1 style="margin:10px 0 4px;font-size:28px;line-height:1.2;">Verify Your Email</h1>
                      <p style="margin:0;font-size:15px;opacity:0.92;">Use this one-time code to activate your Latte and Letters student account.</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="margin:0 0 16px;font-size:15px;line-height:1.7;">Hello %s,</p>
                      <p style="margin:0 0 20px;font-size:15px;line-height:1.7;">Thank you for registering at Latte and Letters. Enter the verification code below to activate your account. Do not share this code with anyone.</p>
                      <div style="margin:0 0 24px;padding:18px 20px;border-radius:20px;background:#effaf2;border:1px solid #c9ead1;text-align:center;">
                        <div style="font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;color:#2f7d49;">Verification Code</div>
                        <div style="margin-top:8px;font-size:34px;font-weight:800;letter-spacing:0.34em;color:#0f7a36;">%s</div>
                      </div>
                      %s
                      <div style="margin:0 0 24px;padding:20px;border-radius:18px;background:#fbfefd;border:1px solid #e0efe4;">
                        <div style="font-size:15px;font-weight:700;color:#18452d;margin-bottom:12px;">Account Details</div>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;line-height:1.6;">
                          <tr><td style="padding:6px 0;color:#5f7b69;">Registered Email</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#5f7b69;">Code Expires</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                        </table>
                      </div>
                      <div style="padding:16px 18px;border-radius:16px;background:#fff8ea;border:1px solid #f1ddb1;color:#6b5112;font-size:13px;line-height:1.7;">
                        If you did not create this account, you can safely ignore this email. The account will remain inactive until verified.
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
                tempPwSection,
                escapeHtml(user.getEmail()),
                escapeHtml(DATE_TIME_FORMATTER.format(token.getExpiresAt()))
        );
    }
}
