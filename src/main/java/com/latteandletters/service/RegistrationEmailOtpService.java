package com.latteandletters.service;

import com.latteandletters.dto.RegistrationAvailabilityResult;
import com.latteandletters.dto.RegistrationEmailOtpSessionState;
import com.latteandletters.dto.RegistrationOtpState;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class RegistrationEmailOtpService {

    public static final String SESSION_KEY = "registrationEmailOtpState";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a", Locale.ENGLISH);

    private final AuthService authService;
    private final EmailNotificationService emailNotificationService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final int resendCooldownSeconds;
    private final int otpValidityMinutes;

    public RegistrationEmailOtpService(AuthService authService,
                                       EmailNotificationService emailNotificationService,
                                       @Value("${latteandletters.registration-otp.resend-seconds:180}") int resendCooldownSeconds,
                                       @Value("${latteandletters.registration-otp.validity-minutes:15}") int otpValidityMinutes) {
        this.authService = authService;
        this.emailNotificationService = emailNotificationService;
        this.resendCooldownSeconds = Math.max(30, resendCooldownSeconds);
        this.otpValidityMinutes = Math.max(5, otpValidityMinutes);
    }

    public RegistrationOtpState requestOtp(String email, HttpSession session) {
        String normalizedEmail = validateRegistrationEmail(email);
        RegistrationEmailOtpSessionState currentState = getSessionState(session);
        LocalDateTime now = LocalDateTime.now();

        if (currentState != null
                && normalizedEmail.equalsIgnoreCase(currentState.getEmail())
                && !currentState.isVerified()
                && currentState.getResendAvailableAt() != null
                && currentState.getResendAvailableAt().isAfter(now)) {
            throw new IllegalArgumentException("Please wait before requesting another OTP.");
        }

        RegistrationEmailOtpSessionState nextState = new RegistrationEmailOtpSessionState();
        String otpCode = generateOtpCodeAndSend(normalizedEmail);
        nextState.setEmail(normalizedEmail);
        nextState.setMaskedEmail(maskEmail(normalizedEmail));
        nextState.setOtpHash(hashOtp(otpCode));
        nextState.setExpiresAt(now.plusMinutes(otpValidityMinutes));
        nextState.setResendAvailableAt(now.plusSeconds(resendCooldownSeconds));
        nextState.setVerified(false);
        session.setAttribute(SESSION_KEY, nextState);
        return toPublicState(nextState);
    }

    public RegistrationOtpState getOtpState(String email, HttpSession session) {
        RegistrationEmailOtpSessionState state = findMatchingState(email, session);
        if (state == null || state.isVerified()) {
            return null;
        }
        if (state.getExpiresAt() == null || !state.getExpiresAt().isAfter(LocalDateTime.now())) {
            return null;
        }
        return toPublicState(state);
    }

    public boolean isEmailVerified(String email, HttpSession session) {
        RegistrationEmailOtpSessionState state = findMatchingState(email, session);
        return state != null && state.isVerified();
    }

    public void verifyOtp(String email, String otpCode, HttpSession session) {
        RegistrationEmailOtpSessionState state = findMatchingState(email, session);
        if (state == null) {
            throw new IllegalArgumentException("Request an OTP first before verifying your email.");
        }
        if (state.isVerified()) {
            return;
        }
        if (state.getExpiresAt() == null || !state.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("The verification code has expired. Request a new one.");
        }
        if (otpCode == null || otpCode.isBlank()) {
            throw new IllegalArgumentException("Enter the 6-digit verification code.");
        }
        if (!hashOtp(otpCode.trim()).equals(state.getOtpHash())) {
            throw new IllegalArgumentException("Invalid verification code. Please try again.");
        }

        state.setVerified(true);
        session.setAttribute(SESSION_KEY, state);
    }

    public void clear(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }

    private RegistrationEmailOtpSessionState findMatchingState(String email, HttpSession session) {
        RegistrationEmailOtpSessionState state = getSessionState(session);
        if (state == null) {
            return null;
        }
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return normalizedEmail.equalsIgnoreCase(state.getEmail()) ? state : null;
    }

    private RegistrationEmailOtpSessionState getSessionState(HttpSession session) {
        Object value = session.getAttribute(SESSION_KEY);
        return value instanceof RegistrationEmailOtpSessionState sessionState ? sessionState : null;
    }

    private String validateRegistrationEmail(String email) {
        RegistrationAvailabilityResult result = authService.checkRegistrationAvailability("email", email);
        if (!result.valid()) {
            throw new IllegalArgumentException(result.message());
        }
        if (!result.available()) {
            throw new IllegalArgumentException(result.message());
        }
        return result.normalizedValue();
    }

    private RegistrationOtpState toPublicState(RegistrationEmailOtpSessionState state) {
        return new RegistrationOtpState(
                state.getEmail(),
                state.getMaskedEmail(),
                state.getExpiresAt(),
                state.getResendAvailableAt()
        );
    }

    private String generateOtpCodeAndSend(String email) {
        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        emailNotificationService.sendImmediateHtmlEmail(
                email,
                "Latte and Letters - Registration Email Verification",
                buildEmailBody(email, otpCode)
        );
        return otpCode;
    }

    private String buildEmailBody(String email, String otpCode) {
        return """
                <div style="margin:0;padding:24px;background:#f4faf6;font-family:Segoe UI,Arial,sans-serif;color:#163322;">
                  <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #d5eadc;border-radius:24px;overflow:hidden;box-shadow:0 18px 44px rgba(18,77,47,0.12);">
                    <div style="padding:24px 32px;background:linear-gradient(135deg,#0f7a36,#34c66a);color:#ffffff;">
                      <div style="font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.88;">Latte and Letters</div>
                      <h1 style="margin:10px 0 4px;font-size:28px;line-height:1.2;">Confirm Your Email</h1>
                      <p style="margin:0;font-size:15px;opacity:0.92;">Use this one-time code to continue your student registration.</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="margin:0 0 20px;font-size:15px;line-height:1.7;">Enter this 6-digit verification code on the Latte and Letters registration page. The code expires after %s minutes.</p>
                      <div style="margin:0 0 24px;padding:18px 20px;border-radius:20px;background:#effaf2;border:1px solid #c9ead1;text-align:center;">
                        <div style="font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;color:#2f7d49;">Verification Code</div>
                        <div style="margin-top:8px;font-size:34px;font-weight:800;letter-spacing:0.34em;color:#0f7a36;">%s</div>
                      </div>
                      <div style="margin:0 0 24px;padding:20px;border-radius:18px;background:#fbfefd;border:1px solid #e0efe4;">
                        <div style="font-size:15px;font-weight:700;color:#18452d;margin-bottom:12px;">Verification Details</div>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;line-height:1.6;">
                          <tr><td style="padding:6px 0;color:#5f7b69;">Email</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#5f7b69;">Expires</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                        </table>
                      </div>
                      <div style="padding:16px 18px;border-radius:16px;background:#fff8ea;border:1px solid #f1ddb1;color:#6b5112;font-size:13px;line-height:1.7;">
                        If you did not request this registration, you can ignore this message. Never share this code with anyone.
                      </div>
                    </div>
                  </div>
                </div>
                """.formatted(
                otpValidityMinutes,
                escapeHtml(otpCode),
                escapeHtml(email),
                escapeHtml(DATE_TIME_FORMATTER.format(LocalDateTime.now().plusMinutes(otpValidityMinutes)))
        );
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
        String local = email.substring(0, atIndex);
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + email.substring(atIndex);
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
}
