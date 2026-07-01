package com.latteandletters.service;

import com.latteandletters.dto.StudentProfileOtpDispatchResult;
import com.latteandletters.dto.StudentProfileOtpState;
import com.latteandletters.dto.StudentProfileUpdateRequest;
import com.latteandletters.model.Student;
import com.latteandletters.model.StudentProfileOtpRequest;
import com.latteandletters.repository.StudentProfileOtpRequestRepository;
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
import java.util.Locale;

@Service
public class StudentProfileOtpService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a", Locale.ENGLISH);

    private final StudentProfileOtpRequestRepository otpRequestRepository;
    private final StudentService studentService;
    private final EmailNotificationService emailNotificationService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final int resendCooldownSeconds;
    private final int otpValidityMinutes;

    public StudentProfileOtpService(StudentProfileOtpRequestRepository otpRequestRepository,
                                    StudentService studentService,
                                    EmailNotificationService emailNotificationService,
                                    @Value("${latteandletters.profile-otp.resend-seconds:180}") int resendCooldownSeconds,
                                    @Value("${latteandletters.profile-otp.validity-minutes:10}") int otpValidityMinutes) {
        this.otpRequestRepository = otpRequestRepository;
        this.studentService = studentService;
        this.emailNotificationService = emailNotificationService;
        this.resendCooldownSeconds = Math.max(30, resendCooldownSeconds);
        this.otpValidityMinutes = Math.max(3, otpValidityMinutes);
    }

    public StudentProfileOtpState getActiveOtpState(Student student) {
        return otpRequestRepository.findFirstByStudent_IdAndUsedFalseOrderByCreatedAtDesc(student.getId())
                .filter(request -> request.getExpiresAt() != null && request.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::toOtpState)
                .orElse(null);
    }

    public StudentProfileOtpState getLatestOtpState(Student student) {
        return otpRequestRepository.findFirstByStudent_IdAndUsedFalseOrderByCreatedAtDesc(student.getId())
                .map(this::toOtpState)
                .orElse(null);
    }

    @Transactional
    public StudentProfileOtpDispatchResult requestOtp(Student student, StudentProfileUpdateRequest request) {
        StudentProfileUpdateRequest normalizedRequest = studentService.normalizeProfileUpdateRequest(request);
        StudentProfileOtpRequest otpRequest = otpRequestRepository.findFirstByStudent_IdAndUsedFalseOrderByCreatedAtDesc(student.getId())
                .orElseGet(StudentProfileOtpRequest::new);

        otpRequest.setStudent(student);
        otpRequest.setDestinationEmail(student.getUser().getEmail());
        applyPendingRequest(otpRequest, normalizedRequest);

        LocalDateTime now = LocalDateTime.now();
        if (otpRequest.getId() != null
                && otpRequest.getResendAvailableAt() != null
                && otpRequest.getResendAvailableAt().isAfter(now)
                && otpRequest.getExpiresAt() != null
                && otpRequest.getExpiresAt().isAfter(now)) {
            StudentProfileOtpRequest savedRequest = otpRequestRepository.save(otpRequest);
            return new StudentProfileOtpDispatchResult(
                    toOtpState(savedRequest),
                    false,
                    false,
                    true,
                    secondsRemaining(now, savedRequest.getResendAvailableAt())
            );
        }

        String otpCode = generateOtpCode();
        otpRequest.setOtpHash(hashOtp(otpCode));
        otpRequest.setLastSentAt(now);
        otpRequest.setResendAvailableAt(now.plusSeconds(resendCooldownSeconds));
        otpRequest.setExpiresAt(now.plusMinutes(otpValidityMinutes));
        otpRequest.setUsed(false);
        otpRequest.setVerifiedAt(null);

        StudentProfileOtpRequest savedRequest = otpRequestRepository.save(otpRequest);
        boolean delivered = emailNotificationService.sendImmediateHtmlEmail(
                student.getUser().getEmail(),
                "Latte and Letters Verification Code | Profile Update",
                buildProfileOtpEmailBody(student, otpCode, savedRequest)
        );
        if (!delivered) {
            throw new IllegalStateException("Unable to send profile OTP email right now. Please check the SMTP configuration and try again.");
        }

        return new StudentProfileOtpDispatchResult(
                toOtpState(savedRequest),
                true,
                delivered,
                false,
                secondsRemaining(now, savedRequest.getResendAvailableAt())
        );
    }

    @Transactional
    public StudentProfileOtpDispatchResult resendOtp(Student student) {
        StudentProfileOtpState latestState = getLatestOtpState(student);
        if (latestState == null) {
            throw new IllegalArgumentException("Request a profile update OTP first.");
        }
        return requestOtp(student, latestState.getUpdateRequest());
    }

    @Transactional
    public StudentProfileUpdateRequest verifyOtp(Student student, String otpCode) {
        if (otpCode == null || otpCode.trim().isBlank()) {
            throw new IllegalArgumentException("Enter the one-time passcode first.");
        }

        StudentProfileOtpRequest otpRequest = otpRequestRepository.findFirstByStudent_IdAndUsedFalseOrderByCreatedAtDesc(student.getId())
                .orElseThrow(() -> new IllegalArgumentException("Request a profile update OTP first."));

        LocalDateTime now = LocalDateTime.now();
        if (otpRequest.getExpiresAt() == null || !otpRequest.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("The OTP has expired. Request a new code to continue.");
        }

        if (!hashOtp(otpCode.trim()).equals(otpRequest.getOtpHash())) {
            throw new IllegalArgumentException("Invalid OTP. Please try again.");
        }

        otpRequest.setUsed(true);
        otpRequest.setVerifiedAt(now);
        otpRequestRepository.save(otpRequest);
        return toUpdateRequest(otpRequest);
    }

    private void applyPendingRequest(StudentProfileOtpRequest otpRequest, StudentProfileUpdateRequest request) {
        otpRequest.setPendingName(request.getName());
        otpRequest.setPendingCourse(request.getCourse());
        otpRequest.setPendingYearLevel(request.getYearLevel());
        otpRequest.setPendingPhone(request.getPhone());
        otpRequest.setPendingAddress(request.getAddress());
        otpRequest.setPendingDateOfBirth(request.getDateOfBirth());
    }

    private StudentProfileUpdateRequest toUpdateRequest(StudentProfileOtpRequest otpRequest) {
        return new StudentProfileUpdateRequest(
                otpRequest.getPendingName(),
                otpRequest.getPendingCourse(),
                otpRequest.getPendingYearLevel(),
                otpRequest.getPendingPhone(),
                otpRequest.getPendingAddress(),
                otpRequest.getPendingDateOfBirth()
        );
    }

    private StudentProfileOtpState toOtpState(StudentProfileOtpRequest otpRequest) {
        return new StudentProfileOtpState(
                toUpdateRequest(otpRequest),
                otpRequest.getExpiresAt(),
                otpRequest.getDestinationEmail(),
                otpRequest.getResendAvailableAt()
        );
    }

    private String buildProfileOtpEmailBody(Student student, String otpCode, StudentProfileOtpRequest otpRequest) {
        StringBuilder requestedChanges = new StringBuilder();
        appendRequestedChange(requestedChanges, "Name", otpRequest.getPendingName());
        appendRequestedChange(requestedChanges, "Program", otpRequest.getPendingCourse());
        appendRequestedChange(requestedChanges, "Year Level", otpRequest.getPendingYearLevel());
        appendRequestedChange(requestedChanges, "Contact Number", otpRequest.getPendingPhone());
        appendRequestedChange(requestedChanges, "Address", otpRequest.getPendingAddress());
        appendRequestedChange(
                requestedChanges,
                "Birth Date",
                otpRequest.getPendingDateOfBirth() == null ? null : otpRequest.getPendingDateOfBirth().toString()
        );

        if (requestedChanges.length() == 0) {
            requestedChanges.append("<li>No profile fields were listed in this request.</li>");
        }

        return """
                <div style="margin:0;padding:24px;background:#f4faf6;font-family:Segoe UI,Arial,sans-serif;color:#163322;">
                  <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #d5eadc;border-radius:24px;overflow:hidden;box-shadow:0 18px 44px rgba(18,77,47,0.12);">
                    <div style="padding:24px 32px;background:linear-gradient(135deg,#0f7a36,#34c66a);color:#ffffff;">
                      <div style="font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.88;">Latte and Letters</div>
                      <h1 style="margin:10px 0 4px;font-size:28px;line-height:1.2;">Profile Update Verification</h1>
                      <p style="margin:0;font-size:15px;opacity:0.92;">Use the one-time verification code below to confirm the changes to your student profile.</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="margin:0 0 16px;font-size:15px;line-height:1.7;">Hello %s,</p>
                      <p style="margin:0 0 20px;font-size:15px;line-height:1.7;">We received a request to update your Latte and Letters student profile. Enter this code in the system to continue. For your security, do not share this code with anyone.</p>
                      <div style="margin:0 0 24px;padding:18px 20px;border-radius:20px;background:#effaf2;border:1px solid #c9ead1;text-align:center;">
                        <div style="font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;color:#2f7d49;">Verification Code</div>
                        <div style="margin-top:8px;font-size:34px;font-weight:800;letter-spacing:0.34em;color:#0f7a36;">%s</div>
                      </div>
                      <div style="margin:0 0 24px;padding:20px;border-radius:18px;background:#fbfefd;border:1px solid #e0efe4;">
                        <div style="font-size:15px;font-weight:700;color:#18452d;margin-bottom:12px;">Request Details</div>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;line-height:1.6;">
                          <tr><td style="padding:6px 0;color:#5f7b69;">Student ID</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#5f7b69;">Registered Email</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#5f7b69;">Code Expires</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#5f7b69;">Resend Available</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                        </table>
                      </div>
                      <div style="margin:0 0 24px;padding:20px;border-radius:18px;background:#ffffff;border:1px solid #e0efe4;">
                        <div style="font-size:15px;font-weight:700;color:#18452d;margin-bottom:10px;">Requested Profile Changes</div>
                        <ul style="margin:0;padding-left:18px;color:#2b4936;font-size:14px;line-height:1.7;">%s</ul>
                      </div>
                      <div style="padding:16px 18px;border-radius:16px;background:#fff8ea;border:1px solid #f1ddb1;color:#6b5112;font-size:13px;line-height:1.7;">
                        If you did not request this profile update, you can safely ignore this email. No changes will be applied unless the correct verification code is entered before it expires.
                      </div>
                    </div>
                    <div style="padding:18px 32px;background:#f6fbf7;border-top:1px solid #e1efe5;font-size:12px;line-height:1.7;color:#6c8375;">
                      This is an automated message from Latte and Letters. Please do not reply to this email.
                    </div>
                  </div>
                </div>
                """.formatted(
                escapeHtml(student.getUser().getName()),
                escapeHtml(otpCode),
                escapeHtml(student.getStudentId()),
                escapeHtml(student.getUser().getEmail()),
                escapeHtml(DATE_TIME_FORMATTER.format(otpRequest.getExpiresAt())),
                escapeHtml(DATE_TIME_FORMATTER.format(otpRequest.getResendAvailableAt())),
                requestedChanges
        );
    }

    private void appendRequestedChange(StringBuilder html, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        html.append("<li><strong>")
                .append(escapeHtml(label))
                .append(":</strong> ")
                .append(escapeHtml(value))
                .append("</li>");
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

    private long secondsRemaining(LocalDateTime now, LocalDateTime targetTime) {
        if (targetTime == null || !targetTime.isAfter(now)) {
            return 0L;
        }
        return java.time.Duration.between(now, targetTime).getSeconds();
    }
}
