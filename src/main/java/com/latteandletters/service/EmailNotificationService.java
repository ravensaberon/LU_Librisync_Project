package com.latteandletters.service;

import com.latteandletters.model.AdminNotificationType;
import com.latteandletters.model.EmailNotification;
import com.latteandletters.model.EmailNotificationStatus;
import com.latteandletters.model.EmailNotificationType;
import com.latteandletters.model.Fine;
import com.latteandletters.model.FineStatus;
import com.latteandletters.model.IssueRecord;
import com.latteandletters.model.IssueStatus;
import com.latteandletters.model.Reservation;
import com.latteandletters.model.ReservationStatus;
import com.latteandletters.model.User;
import com.latteandletters.repository.EmailNotificationRepository;
import com.latteandletters.repository.FineRepository;
import com.latteandletters.repository.IssueRecordRepository;
import com.latteandletters.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class EmailNotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a", Locale.ENGLISH);

    private final EmailNotificationRepository emailNotificationRepository;
    private final IssueRecordRepository issueRecordRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;
    private final AdminNotificationService adminNotificationService;
    private final Path outboxRoot;
    private final String smtpHost;
    private final String smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String smtpFrom;
    private final String smtpSsl;

    public EmailNotificationService(EmailNotificationRepository emailNotificationRepository,
                                    IssueRecordRepository issueRecordRepository,
                                    ReservationRepository reservationRepository,
                                    FineRepository fineRepository,
                                    AdminNotificationService adminNotificationService,
                                    @Value("${latteandletters.smtp.host:}") String smtpHost,
                                    @Value("${latteandletters.smtp.port:587}") String smtpPort,
                                    @Value("${latteandletters.smtp.username:latteandletters@gmail.com}") String smtpUsername,
                                    @Value("${latteandletters.smtp.password:}") String smtpPassword,
                                    @Value("${latteandletters.smtp.from:latteandletters@gmail.com}") String smtpFrom,
                                    @Value("${latteandletters.smtp.ssl:true}") String smtpSsl,
                                    @Value("${latteandletters.notification.outbox-root:${latteandletters.storage.root:${user.dir}/storage}/email-outbox}") String outboxRootPath) {
        this.emailNotificationRepository = emailNotificationRepository;
        this.issueRecordRepository = issueRecordRepository;
        this.reservationRepository = reservationRepository;
        this.fineRepository = fineRepository;
        this.adminNotificationService = adminNotificationService;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword == null ? "" : smtpPassword.replaceAll("\\s+", "");
        this.smtpFrom = smtpFrom;
        this.smtpSsl = smtpSsl;
        this.outboxRoot = Path.of(outboxRootPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.outboxRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize email notification outbox.", exception);
        }
    }

    @Transactional
    public void queueDueReminder(IssueRecord issueRecord) {
        if (issueRecord == null || issueRecord.getStudent() == null || issueRecord.getStudent().getUser() == null) {
            return;
        }

        User recipient = issueRecord.getStudent().getUser();
        LocalDateTime dueDate = issueRecord.getDueDate();
        LocalDate dueDateLocal = dueDate.toLocalDate();
        LocalDate today = LocalDate.now();

        // 3-day reminder — scheduled at 8 AM, 3 days before due date
        if (!today.isAfter(dueDateLocal.minusDays(3))) {
            String subject3 = "Due in 3 Days | Issue #" + issueRecord.getId() + " | " + issueRecord.getBook().getTitle();
            LocalDateTime scheduledAt3 = dueDateLocal.minusDays(3).atTime(8, 0);
            if (scheduledAt3.isBefore(LocalDateTime.now())) {
                scheduledAt3 = LocalDateTime.now().plusMinutes(1);
            }
            upsertPendingNotification(recipient, EmailNotificationType.DUE_REMINDER_3_DAYS, subject3,
                    buildDueReminderEmailBody(recipient, issueRecord, 3), scheduledAt3);
        }

        // 1-day reminder — scheduled at 8 AM, 1 day before due date
        if (!today.isAfter(dueDateLocal.minusDays(1))) {
            String subject1 = "Due Tomorrow | Issue #" + issueRecord.getId() + " | " + issueRecord.getBook().getTitle();
            LocalDateTime scheduledAt1 = dueDateLocal.minusDays(1).atTime(8, 0);
            if (scheduledAt1.isBefore(LocalDateTime.now())) {
                scheduledAt1 = LocalDateTime.now().plusMinutes(1);
            }
            upsertPendingNotification(recipient, EmailNotificationType.DUE_REMINDER_1_DAY, subject1,
                    buildDueReminderEmailBody(recipient, issueRecord, 1), scheduledAt1);
        }

        // On-due-date reminder — scheduled at 8 AM on the due date itself
        if (!today.isAfter(dueDateLocal)) {
            String subjectOn = "Due Today | Issue #" + issueRecord.getId() + " | " + issueRecord.getBook().getTitle();
            LocalDateTime scheduledAtOn = dueDateLocal.atTime(8, 0);
            if (scheduledAtOn.isBefore(LocalDateTime.now())) {
                scheduledAtOn = LocalDateTime.now().plusMinutes(1);
            }
            upsertPendingNotification(recipient, EmailNotificationType.DUE_REMINDER_ON_DATE, subjectOn,
                    buildDueReminderEmailBody(recipient, issueRecord, 0), scheduledAtOn);
        }

        // Legacy DUE_REMINDER kept for backward-compat (1-day, same as before)
        String legacySubject = "Due Reminder | Issue #" + issueRecord.getId() + " | " + issueRecord.getBook().getTitle();
        LocalDateTime legacyScheduledAt = dueDate.minusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
        if (legacyScheduledAt.isBefore(LocalDateTime.now())) {
            legacyScheduledAt = LocalDateTime.now().plusMinutes(1);
        }
        upsertPendingNotification(recipient, EmailNotificationType.DUE_REMINDER, legacySubject,
                buildDueReminderEmailBody(recipient, issueRecord, 1), legacyScheduledAt);
    }

    private String buildDueReminderEmailBody(User recipient, IssueRecord issueRecord, int daysUntilDue) {
        String headline;
        String subheadline;
        String bodyText;
        String urgencyColor;

        if (daysUntilDue >= 3) {
            headline = "Book Due in 3 Days";
            subheadline = "Your borrowed book is due in 3 days. Plan your return ahead of time.";
            bodyText = "This is an early reminder that the book you borrowed is due in <strong>3 days</strong>. Please make sure to return it on or before the due date to avoid fines.";
            urgencyColor = "#2e7d32";
        } else if (daysUntilDue == 1) {
            headline = "Book Due Tomorrow";
            subheadline = "Your borrowed book is due tomorrow. Please return it on time to avoid fines.";
            bodyText = "This is a reminder that the book you borrowed is due <strong>tomorrow</strong>. Please return it to the library on or before the due date to avoid additional fines.";
            urgencyColor = "#e65100";
        } else {
            headline = "Book Due Today";
            subheadline = "Your borrowed book is due today. Return it before the library closes.";
            bodyText = "This is your final reminder that the book you borrowed is due <strong>today</strong>. Please return it to the library before closing time to avoid fines.";
            urgencyColor = "#c0392b";
        }

        return """
                <div style="margin:0;padding:24px;background:#f4faf6;font-family:Segoe UI,Arial,sans-serif;color:#163322;">
                  <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #d5eadc;border-radius:24px;overflow:hidden;box-shadow:0 18px 44px rgba(18,77,47,0.12);">
                    <div style="padding:24px 32px;background:linear-gradient(135deg,#0f7a36,#34c66a);color:#ffffff;">
                      <div style="font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.88;">Latte and Letters</div>
                      <h1 style="margin:10px 0 4px;font-size:28px;line-height:1.2;">%s</h1>
                      <p style="margin:0;font-size:15px;opacity:0.92;">%s</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="margin:0 0 16px;font-size:15px;line-height:1.7;">Hello %s,</p>
                      <p style="margin:0 0 20px;font-size:15px;line-height:1.7;">%s</p>
                      <div style="margin:0 0 24px;padding:20px;border-radius:18px;background:#fbfefd;border:1px solid #e0efe4;">
                        <div style="font-size:15px;font-weight:700;color:#18452d;margin-bottom:12px;">Loan Details</div>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;line-height:1.6;">
                          <tr><td style="padding:6px 0;color:#5f7b69;">Book Title</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#5f7b69;">Student ID</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#5f7b69;">Due Date</td><td style="padding:6px 0;text-align:right;font-weight:600;color:%s;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#5f7b69;">Issue Code</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                        </table>
                      </div>
                      <div style="padding:16px 18px;border-radius:16px;background:#fff8ea;border:1px solid #f1ddb1;color:#6b5112;font-size:13px;line-height:1.7;">
                        Returning the book late will incur a daily fine. If you need more time, please contact the library directly.
                      </div>
                    </div>
                    <div style="padding:18px 32px;background:#f6fbf7;border-top:1px solid #e1efe5;font-size:12px;line-height:1.7;color:#6c8375;">
                      This is an automated message from Latte and Letters. Please do not reply to this email.
                    </div>
                  </div>
                </div>
                """.formatted(
                escapeHtml(headline),
                escapeHtml(subheadline),
                escapeHtml(recipient.getName()),
                bodyText,
                escapeHtml(issueRecord.getBook().getTitle()),
                escapeHtml(issueRecord.getStudent().getStudentId()),
                urgencyColor,
                escapeHtml(DATE_TIME_FORMATTER.format(issueRecord.getDueDate())),
                escapeHtml(issueRecord.getQrIssueCode())
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

    @Transactional
    public void cancelDueReminder(IssueRecord issueRecord) {
        if (issueRecord == null || issueRecord.getStudent() == null || issueRecord.getStudent().getUser() == null) {
            return;
        }
        Long userId = issueRecord.getStudent().getUser().getId();
        cancelPendingBySubjectPrefix(userId, EmailNotificationType.DUE_REMINDER,
                "Due Reminder | Issue #" + issueRecord.getId() + " | " + issueRecord.getBook().getTitle());
        cancelPendingBySubjectPrefix(userId, EmailNotificationType.DUE_REMINDER_3_DAYS,
                "Due in 3 Days | Issue #" + issueRecord.getId() + " | " + issueRecord.getBook().getTitle());
        cancelPendingBySubjectPrefix(userId, EmailNotificationType.DUE_REMINDER_1_DAY,
                "Due Tomorrow | Issue #" + issueRecord.getId() + " | " + issueRecord.getBook().getTitle());
        cancelPendingBySubjectPrefix(userId, EmailNotificationType.DUE_REMINDER_ON_DATE,
                "Due Today | Issue #" + issueRecord.getId() + " | " + issueRecord.getBook().getTitle());
    }

    private void cancelPendingBySubjectPrefix(Long userId, EmailNotificationType type, String subject) {
        emailNotificationRepository.findByUser_IdAndNotificationTypeAndSubjectAndStatus(
                        userId, type, subject, EmailNotificationStatus.PENDING)
                .ifPresent(emailNotificationRepository::delete);
    }

    @Transactional
    public void queueReservationReadyNotification(Reservation reservation) {
        if (reservation == null || reservation.getStudent() == null || reservation.getStudent().getUser() == null) {
            return;
        }

        User recipient = reservation.getStudent().getUser();
        String subject;
        String body;

        if (reservation.isBorrowRequest()) {
            subject = "Borrow Request Ready | Request #" + reservation.getId() + " | " + reservation.getBook().getTitle();
            String expiresLabel = reservation.getExpiresAt() == null ? "As soon as possible" : DATE_TIME_FORMATTER.format(reservation.getExpiresAt());
            body = """
                    <div style="margin:0;padding:24px;background:#f4faf6;font-family:Segoe UI,Arial,sans-serif;color:#163322;">
                      <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #d5eadc;border-radius:24px;overflow:hidden;box-shadow:0 18px 44px rgba(18,77,47,0.12);">
                        <div style="padding:24px 32px;background:linear-gradient(135deg,#0f7a36,#34c66a);color:#ffffff;">
                          <div style="font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.88;">Latte and Letters</div>
                          <h1 style="margin:10px 0 4px;font-size:28px;line-height:1.2;">Borrow Request Ready</h1>
                          <p style="margin:0;font-size:15px;opacity:0.92;">Your borrow request is now active at the circulation desk.</p>
                        </div>
                        <div style="padding:32px;">
                          <p style="margin:0 0 16px;font-size:15px;line-height:1.7;">Hello %s,</p>
                          <p style="margin:0 0 20px;font-size:15px;line-height:1.7;">Your borrow request is now active. Please proceed to the circulation desk and present your student ID before the hold window expires.</p>
                          <div style="margin:0 0 24px;padding:20px;border-radius:18px;background:#fbfefd;border:1px solid #e0efe4;">
                            <div style="font-size:15px;font-weight:700;color:#18452d;margin-bottom:12px;">Request Details</div>
                            <table style="width:100%%;border-collapse:collapse;font-size:14px;line-height:1.6;">
                              <tr><td style="padding:6px 0;color:#5f7b69;">Book Title</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                              <tr><td style="padding:6px 0;color:#5f7b69;">Student ID</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                              <tr><td style="padding:6px 0;color:#5f7b69;">Show ID Before</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#c0392b;">%s</td></tr>
                            </table>
                          </div>
                          <div style="padding:16px 18px;border-radius:16px;background:#fff8ea;border:1px solid #f1ddb1;color:#6b5112;font-size:13px;line-height:1.7;">
                            Please go to the circulation desk before the hold window expires. Unclaimed requests may be released to other borrowers.
                          </div>
                        </div>
                        <div style="padding:18px 32px;background:#f6fbf7;border-top:1px solid #e1efe5;font-size:12px;line-height:1.7;color:#6c8375;">
                          This is an automated message from Latte and Letters. Please do not reply to this email.
                        </div>
                      </div>
                    </div>
                    """.formatted(
                    escapeHtml(recipient.getName()),
                    escapeHtml(reservation.getBook().getTitle()),
                    escapeHtml(reservation.getStudent().getStudentId()),
                    escapeHtml(expiresLabel)
            );
        } else {
            subject = "Reservation Ready | Reservation #" + reservation.getId() + " | " + reservation.getBook().getTitle();
            String claimUntilLabel = reservation.getExpiresAt() == null ? "As soon as possible" : DATE_TIME_FORMATTER.format(reservation.getExpiresAt());
            body = """
                    <div style="margin:0;padding:24px;background:#f4faf6;font-family:Segoe UI,Arial,sans-serif;color:#163322;">
                      <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #d5eadc;border-radius:24px;overflow:hidden;box-shadow:0 18px 44px rgba(18,77,47,0.12);">
                        <div style="padding:24px 32px;background:linear-gradient(135deg,#0f7a36,#34c66a);color:#ffffff;">
                          <div style="font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.88;">Latte and Letters</div>
                          <h1 style="margin:10px 0 4px;font-size:28px;line-height:1.2;">Reservation Ready</h1>
                          <p style="margin:0;font-size:15px;opacity:0.92;">Your reserved book is now available for claiming at the library.</p>
                        </div>
                        <div style="padding:32px;">
                          <p style="margin:0 0 16px;font-size:15px;line-height:1.7;">Hello %s,</p>
                          <p style="margin:0 0 20px;font-size:15px;line-height:1.7;">Great news! Your reserved library book is now ready for claiming. Please visit the library before the claim window expires.</p>
                          <div style="margin:0 0 24px;padding:20px;border-radius:18px;background:#fbfefd;border:1px solid #e0efe4;">
                            <div style="font-size:15px;font-weight:700;color:#18452d;margin-bottom:12px;">Reservation Details</div>
                            <table style="width:100%%;border-collapse:collapse;font-size:14px;line-height:1.6;">
                              <tr><td style="padding:6px 0;color:#5f7b69;">Book Title</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                              <tr><td style="padding:6px 0;color:#5f7b69;">Student ID</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                              <tr><td style="padding:6px 0;color:#5f7b69;">Queue Position</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                              <tr><td style="padding:6px 0;color:#5f7b69;">Claim Until</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#c0392b;">%s</td></tr>
                            </table>
                          </div>
                          <div style="padding:16px 18px;border-radius:16px;background:#fff8ea;border:1px solid #f1ddb1;color:#6b5112;font-size:13px;line-height:1.7;">
                            Please visit the library before the claim window expires. Unclaimed reservations may be released to the next person in queue.
                          </div>
                        </div>
                        <div style="padding:18px 32px;background:#f6fbf7;border-top:1px solid #e1efe5;font-size:12px;line-height:1.7;color:#6c8375;">
                          This is an automated message from Latte and Letters. Please do not reply to this email.
                        </div>
                      </div>
                    </div>
                    """.formatted(
                    escapeHtml(recipient.getName()),
                    escapeHtml(reservation.getBook().getTitle()),
                    escapeHtml(reservation.getStudent().getStudentId()),
                    escapeHtml(String.valueOf(reservation.getQueuePosition())),
                    escapeHtml(claimUntilLabel)
            );
        }

        upsertPendingNotification(recipient, EmailNotificationType.RESERVATION_READY, subject, body, LocalDateTime.now().plusMinutes(1));
    }

    @Transactional
    public void cancelReservationReadyNotification(Reservation reservation) {
        if (reservation == null || reservation.getStudent() == null || reservation.getStudent().getUser() == null) {
            return;
        }
        String subject = reservation.isBorrowRequest()
                ? "Borrow Request Ready | Request #" + reservation.getId() + " | " + reservation.getBook().getTitle()
                : "Reservation Ready | Reservation #" + reservation.getId() + " | " + reservation.getBook().getTitle();
        emailNotificationRepository.findByUser_IdAndNotificationTypeAndSubjectAndStatus(
                        reservation.getStudent().getUser().getId(),
                        EmailNotificationType.RESERVATION_READY,
                        subject,
                        EmailNotificationStatus.PENDING
                )
                .ifPresent(emailNotificationRepository::delete);
    }

    @Transactional
    public void queueUnpaidFineNotification(Fine fine) {
        if (fine == null || !fine.isOutstanding() || fine.getStudent() == null || fine.getStudent().getUser() == null) {
            return;
        }

        User recipient = fine.getStudent().getUser();
        upsertPendingNotification(
                recipient,
                EmailNotificationType.UNPAID_FINE,
                buildUnpaidFineSubject(fine),
                buildUnpaidFineEmailBody(recipient, fine),
                LocalDateTime.now().plusMinutes(1)
        );
    }

    @Transactional
    public void cancelUnpaidFineNotification(Fine fine) {
        if (fine == null || fine.getStudent() == null || fine.getStudent().getUser() == null) {
            return;
        }

        emailNotificationRepository.findByUser_IdAndNotificationTypeAndSubjectAndStatus(
                        fine.getStudent().getUser().getId(),
                        EmailNotificationType.UNPAID_FINE,
                        buildUnpaidFineSubject(fine),
                        EmailNotificationStatus.PENDING
                )
                .ifPresent(emailNotificationRepository::delete);
    }

    private String buildUnpaidFineSubject(Fine fine) {
        String fineId = fine.getId() == null ? "pending" : fine.getId().toString();
        String bookTitle = fine.getIssueRecord() != null && fine.getIssueRecord().getBook() != null
                ? fine.getIssueRecord().getBook().getTitle()
                : "Library item";
        return "Unpaid Fine | Fine #" + fineId + " | " + bookTitle;
    }

    private String buildUnpaidFineEmailBody(User recipient, Fine fine) {
        IssueRecord issueRecord = fine.getIssueRecord();
        String bookTitle = issueRecord != null && issueRecord.getBook() != null
                ? issueRecord.getBook().getTitle()
                : "Library item";
        String issueCode = issueRecord == null || issueRecord.getQrIssueCode() == null
                ? "Not available"
                : issueRecord.getQrIssueCode();
        String calculatedAt = fine.getCalculatedAt() == null
                ? "Not recorded"
                : DATE_TIME_FORMATTER.format(fine.getCalculatedAt());
        String dueDate = issueRecord == null || issueRecord.getDueDate() == null
                ? "Not recorded"
                : DATE_TIME_FORMATTER.format(issueRecord.getDueDate());
        String amount = fine.getAmount() == null ? "PHP 0.00" : "PHP " + fine.getAmount().toPlainString();

        return """
                <div style="margin:0;padding:24px;background:#fff8f4;font-family:Segoe UI,Arial,sans-serif;color:#362012;">
                  <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #f0dccd;border-radius:24px;overflow:hidden;box-shadow:0 18px 44px rgba(105,48,18,0.12);">
                    <div style="padding:24px 32px;background:linear-gradient(135deg,#b45309,#f59e0b);color:#ffffff;">
                      <div style="font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.88;">Latte and Letters</div>
                      <h1 style="margin:10px 0 4px;font-size:28px;line-height:1.2;">Unpaid Library Fine</h1>
                      <p style="margin:0;font-size:15px;opacity:0.92;">Please settle your outstanding fine at the circulation desk.</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="margin:0 0 16px;font-size:15px;line-height:1.7;">Hello %s,</p>
                      <p style="margin:0 0 20px;font-size:15px;line-height:1.7;">Our records show an unpaid fine on your Latte and Letters account. Please settle this balance with the library staff to keep your borrowing privileges clear.</p>
                      <div style="margin:0 0 24px;padding:20px;border-radius:18px;background:#fffdfa;border:1px solid #f0dccd;">
                        <div style="font-size:15px;font-weight:700;color:#5b3213;margin-bottom:12px;">Fine Details</div>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;line-height:1.6;">
                          <tr><td style="padding:6px 0;color:#86634a;">Book Title</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#321b0e;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#86634a;">Student ID</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#321b0e;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#86634a;">Issue Code</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#321b0e;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#86634a;">Due Date</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#321b0e;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#86634a;">Calculated At</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#321b0e;">%s</td></tr>
                          <tr><td style="padding:10px 0 0;color:#86634a;">Amount Due</td><td style="padding:10px 0 0;text-align:right;font-size:22px;font-weight:800;color:#b45309;">%s</td></tr>
                        </table>
                      </div>
                      <div style="padding:16px 18px;border-radius:16px;background:#fff8ea;border:1px solid #f1ddb1;color:#6b5112;font-size:13px;line-height:1.7;">
                        Unpaid fines may temporarily block new borrow requests until they are paid or waived by the library staff.
                      </div>
                    </div>
                    <div style="padding:18px 32px;background:#fffaf5;border-top:1px solid #f0dccd;font-size:12px;line-height:1.7;color:#8a6d5a;">
                      This is an automated message from Latte and Letters. Please do not reply to this email.
                    </div>
                  </div>
                </div>
                """.formatted(
                escapeHtml(recipient.getName()),
                escapeHtml(bookTitle),
                fine.getStudent() == null ? "" : escapeHtml(fine.getStudent().getStudentId()),
                escapeHtml(issueCode),
                escapeHtml(dueDate),
                escapeHtml(calculatedAt),
                escapeHtml(amount)
        );
    }

    @Transactional
    public void queueReservationExpiredNotification(Reservation reservation) {
        if (reservation == null || reservation.getStudent() == null || reservation.getStudent().getUser() == null) {
            return;
        }

        User recipient = reservation.getStudent().getUser();
        String subject = reservation.isBorrowRequest()
                ? "Borrow Request Expired | Request #" + reservation.getId() + " | " + reservation.getBook().getTitle()
                : "Reservation Expired | Reservation #" + reservation.getId() + " | " + reservation.getBook().getTitle();

        String headline = reservation.isBorrowRequest() ? "Borrow Request Expired" : "Reservation Expired";
        String subheadline = reservation.isBorrowRequest()
                ? "Your borrow request hold window has passed without a pickup."
                : "Your reservation hold window has passed without a claim.";
        String bodyText = reservation.isBorrowRequest()
                ? "Your borrow request hold window has expired. The copy has been released back to the circulation queue. You may submit a new borrow request if you still need this book."
                : "Your reservation hold window has expired. The copy has been released to the next person in queue. You may place a new reservation if you still need this book.";

        String body = """
                <div style="margin:0;padding:24px;background:#f4faf6;font-family:Segoe UI,Arial,sans-serif;color:#163322;">
                  <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #d5eadc;border-radius:24px;overflow:hidden;box-shadow:0 18px 44px rgba(18,77,47,0.12);">
                    <div style="padding:24px 32px;background:linear-gradient(135deg,#b71c1c,#e53935);color:#ffffff;">
                      <div style="font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.88;">Latte and Letters</div>
                      <h1 style="margin:10px 0 4px;font-size:28px;line-height:1.2;">%s</h1>
                      <p style="margin:0;font-size:15px;opacity:0.92;">%s</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="margin:0 0 16px;font-size:15px;line-height:1.7;">Hello %s,</p>
                      <p style="margin:0 0 20px;font-size:15px;line-height:1.7;">%s</p>
                      <div style="margin:0 0 24px;padding:20px;border-radius:18px;background:#fbfefd;border:1px solid #e0efe4;">
                        <div style="font-size:15px;font-weight:700;color:#18452d;margin-bottom:12px;">Details</div>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;line-height:1.6;">
                          <tr><td style="padding:6px 0;color:#5f7b69;">Book Title</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#5f7b69;">Student ID</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#5f7b69;">Status</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#c0392b;">Expired &amp; Cancelled</td></tr>
                        </table>
                      </div>
                      <div style="padding:16px 18px;border-radius:16px;background:#fff8ea;border:1px solid #f1ddb1;color:#6b5112;font-size:13px;line-height:1.7;">
                        If you still need this book, visit the catalog to place a new request. Contact the library if you have any questions.
                      </div>
                    </div>
                    <div style="padding:18px 32px;background:#f6fbf7;border-top:1px solid #e1efe5;font-size:12px;line-height:1.7;color:#6c8375;">
                      This is an automated message from Latte and Letters. Please do not reply to this email.
                    </div>
                  </div>
                </div>
                """.formatted(
                escapeHtml(headline),
                escapeHtml(subheadline),
                escapeHtml(recipient.getName()),
                escapeHtml(bodyText),
                escapeHtml(reservation.getBook().getTitle()),
                escapeHtml(reservation.getStudent().getStudentId())
        );

        EmailNotification notification = new EmailNotification();
        notification.setUser(recipient);
        notification.setNotificationType(EmailNotificationType.RESERVATION_EXPIRED);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setScheduledAt(LocalDateTime.now());
        notification.setStatus(EmailNotificationStatus.PENDING);
        emailNotificationRepository.save(notification);
    }

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void processPendingNotifications() {
        ensureReminderQueueCoverage();

        List<EmailNotification> dueNotifications = emailNotificationRepository
                .findTop25ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(EmailNotificationStatus.PENDING, LocalDateTime.now());

        for (EmailNotification notification : dueNotifications) {
            boolean isHtml = notification.getNotificationType() == EmailNotificationType.DUE_REMINDER
                    || notification.getNotificationType() == EmailNotificationType.DUE_REMINDER_3_DAYS
                    || notification.getNotificationType() == EmailNotificationType.DUE_REMINDER_1_DAY
                    || notification.getNotificationType() == EmailNotificationType.DUE_REMINDER_ON_DATE
                    || notification.getNotificationType() == EmailNotificationType.RESERVATION_READY
                    || notification.getNotificationType() == EmailNotificationType.RESERVATION_EXPIRED
                    || notification.getNotificationType() == EmailNotificationType.UNPAID_FINE;
            boolean sent = sendEmail(notification.getUser().getEmail(), notification.getSubject(), notification.getBody(), isHtml);
            notification.setSentAt(LocalDateTime.now());
            notification.setStatus(sent ? EmailNotificationStatus.SENT : EmailNotificationStatus.FAILED);
            emailNotificationRepository.save(notification);
        }
    }

    public boolean sendImmediateEmail(String toEmail, String subject, String body) {
        return sendEmail(toEmail, subject, body);
    }

    public boolean sendImmediateHtmlEmail(String toEmail, String subject, String body) {
        return sendEmail(toEmail, subject, body, true);
    }

    public boolean sendRegistrationTemporaryPassword(User recipient, String temporaryPassword) {
        if (recipient == null || !StringUtils.hasText(recipient.getEmail()) || !StringUtils.hasText(temporaryPassword)) {
            return false;
        }
        return sendImmediateHtmlEmail(
                recipient.getEmail(),
                "Latte and Letters - Your Temporary Password",
                buildRegistrationTemporaryPasswordEmailBody(recipient, temporaryPassword)
        );
    }

    private void ensureReminderQueueCoverage() {
        List<IssueRecord> activeIssues = issueRecordRepository.findByStatusInOrderByIssueDateDesc(List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE));
        for (IssueRecord issueRecord : activeIssues) {
            if (!issueRecord.isReturned()) {
                queueDueReminder(issueRecord);
                queueDueReminderInAppNotification(issueRecord);
            }
        }

        List<Reservation> readyReservations = reservationRepository.findByStatusInOrderByReservedAtAsc(List.of(ReservationStatus.READY));
        for (Reservation reservation : readyReservations) {
            queueReservationReadyNotification(reservation);
        }

        List<Fine> unpaidFines = fineRepository.findByStatusOrderByCalculatedAtDesc(FineStatus.UNPAID);
        for (Fine fine : unpaidFines) {
            queueUnpaidFineNotification(fine);
        }
    }

    private String buildRegistrationTemporaryPasswordEmailBody(User recipient, String temporaryPassword) {
        return """
                <div style="margin:0;padding:24px;background:#f4faf6;font-family:Segoe UI,Arial,sans-serif;color:#163322;">
                  <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #d5eadc;border-radius:24px;overflow:hidden;box-shadow:0 18px 44px rgba(18,77,47,0.12);">
                    <div style="padding:24px 32px;background:linear-gradient(135deg,#0f7a36,#34c66a);color:#ffffff;">
                      <div style="font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.88;">Latte and Letters</div>
                      <h1 style="margin:10px 0 4px;font-size:28px;line-height:1.2;">Your Temporary Password</h1>
                      <p style="margin:0;font-size:15px;opacity:0.92;">Your student account is ready. Use this password for your first sign-in.</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="margin:0 0 16px;font-size:15px;line-height:1.7;">Hello %s,</p>
                      <p style="margin:0 0 20px;font-size:15px;line-height:1.7;">Your Latte and Letters account has been created successfully. For security, your generated password is only sent through email and is no longer shown on the website.</p>
                      <div style="margin:0 0 24px;padding:18px 20px;border-radius:20px;background:#fffbea;border:1px solid #f1ddb1;">
                        <div style="font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;color:#7c4a00;margin-bottom:8px;">Temporary Password</div>
                        <div style="font-family:monospace;font-size:22px;font-weight:800;letter-spacing:0.18em;color:#7c4a00;word-break:break-all;">%s</div>
                        <div style="margin-top:8px;font-size:12px;color:#7c4a00;opacity:0.85;">Sign in with this password, then set a new personal password immediately when the system asks you on first login.</div>
                      </div>
                      <div style="margin:0 0 24px;padding:20px;border-radius:18px;background:#fbfefd;border:1px solid #e0efe4;">
                        <div style="font-size:15px;font-weight:700;color:#18452d;margin-bottom:12px;">Account Details</div>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;line-height:1.6;">
                          <tr><td style="padding:6px 0;color:#5f7b69;">Email</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#5f7b69;">Account Type</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#173522;">Student</td></tr>
                        </table>
                      </div>
                      <div style="padding:16px 18px;border-radius:16px;background:#fff8ea;border:1px solid #f1ddb1;color:#6b5112;font-size:13px;line-height:1.7;">
                        Keep this password private. If you did not create this account, contact the library immediately.
                      </div>
                    </div>
                    <div style="padding:18px 32px;background:#f6fbf7;border-top:1px solid #e1efe5;font-size:12px;line-height:1.7;color:#6c8375;">
                      This is an automated message from Latte and Letters. Please do not reply to this email.
                    </div>
                  </div>
                </div>
                """.formatted(
                escapeHtml(recipient.getName()),
                escapeHtml(temporaryPassword),
                escapeHtml(recipient.getEmail())
        );
    }

    private void queueDueReminderInAppNotification(IssueRecord issueRecord) {
        if (issueRecord.getStudent() == null || issueRecord.getStudent().getUser() == null) {
            return;
        }
        String studentEmail = issueRecord.getStudent().getUser().getEmail();
        LocalDate dueDate = issueRecord.getDueDate().toLocalDate();
        LocalDate today = LocalDate.now();
        long daysUntilDue = today.until(dueDate).getDays();

        String bookTitle = issueRecord.getBook().getTitle();
        String dueDateLabel = DATE_TIME_FORMATTER.format(issueRecord.getDueDate());

        if (daysUntilDue == 3) {
            adminNotificationService.notifyUserIfAbsent(
                    studentEmail,
                    AdminNotificationType.DUE_REMINDER,
                    "Book due in 3 days",
                    "\"" + bookTitle + "\" is due on " + dueDateLabel + ". Please plan your return.",
                    "/student/history"
            );
        } else if (daysUntilDue == 1) {
            adminNotificationService.notifyUserIfAbsent(
                    studentEmail,
                    AdminNotificationType.DUE_REMINDER,
                    "Book due tomorrow",
                    "\"" + bookTitle + "\" is due tomorrow (" + dueDateLabel + "). Return it on time to avoid fines.",
                    "/student/history"
            );
        } else if (daysUntilDue == 0) {
            adminNotificationService.notifyUserIfAbsent(
                    studentEmail,
                    AdminNotificationType.DUE_REMINDER,
                    "Book due today",
                    "\"" + bookTitle + "\" is due today (" + dueDateLabel + "). Return it before the library closes.",
                    "/student/history"
            );
        }
    }

    private void upsertPendingNotification(User recipient,
                                           EmailNotificationType notificationType,
                                           String subject,
                                           String body,
                                           LocalDateTime scheduledAt) {
        EmailNotification notification = emailNotificationRepository
                .findTopByUser_IdAndNotificationTypeAndSubjectOrderByCreatedAtDesc(
                        recipient.getId(),
                        notificationType,
                        subject
                )
                .orElseGet(EmailNotification::new);

        if (notification.getId() != null
                && notificationType.equals(notification.getNotificationType())
                && notification.getUser() != null
                && recipient.getId().equals(notification.getUser().getId())
                && subject.equals(notification.getSubject())) {
            if (EmailNotificationStatus.SENT.equals(notification.getStatus()) && body.equals(notification.getBody())) {
                return;
            }
            if (EmailNotificationStatus.PENDING.equals(notification.getStatus())) {
                notification.setBody(body);
                notification.setScheduledAt(scheduledAt);
                notification.setSentAt(null);
                emailNotificationRepository.save(notification);
                return;
            }
            if (EmailNotificationStatus.FAILED.equals(notification.getStatus())) {
                notification.setBody(body);
                notification.setScheduledAt(scheduledAt);
                notification.setStatus(EmailNotificationStatus.PENDING);
                notification.setSentAt(null);
                emailNotificationRepository.save(notification);
                return;
            }
        }

        notification.setUser(recipient);
        notification.setNotificationType(notificationType);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setScheduledAt(scheduledAt);
        notification.setStatus(EmailNotificationStatus.PENDING);
        notification.setSentAt(null);
        emailNotificationRepository.save(notification);
    }

    private boolean sendEmail(String toEmail, String subject, String body) {
        return sendEmail(toEmail, subject, body, false);
    }

    private boolean sendEmail(String toEmail, String subject, String body, boolean htmlBody) {
        if (!StringUtils.hasText(smtpHost) || !StringUtils.hasText(smtpFrom)) {
            writeOutboxCopy(toEmail, subject, body, htmlBody);
            return false;
        }
        if (requiresAuthentication() && !StringUtils.hasText(smtpPassword)) {
            writeOutboxCopy(
                    toEmail,
                    subject,
                    body + System.lineSeparator() + System.lineSeparator()
                            + "Send error:" + System.lineSeparator()
                            + "SMTP authentication is enabled but no SMTP password is configured.",
                    htmlBody
            );
            return false;
        }

        String script = buildPowerShellMailScript(smtpHost, smtpPort, smtpUsername, smtpPassword, smtpFrom, smtpSsl, toEmail, subject, body, htmlBody);
        ProcessBuilder processBuilder = new ProcessBuilder("powershell", "-NoProfile", "-Command", script);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(45, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                writeOutboxCopy(
                        toEmail,
                        subject,
                        body + System.lineSeparator() + System.lineSeparator()
                                + "Send error:" + System.lineSeparator()
                                + "SMTP send timed out after 45 seconds." + System.lineSeparator() + output,
                        htmlBody
                );
                return false;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return true;
            }
            writeOutboxCopy(
                    toEmail,
                    subject,
                    body + System.lineSeparator() + System.lineSeparator() + "Send error:" + System.lineSeparator() + output,
                    htmlBody
            );
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            writeOutboxCopy(toEmail, subject, body, htmlBody);
        }
        return false;
    }

    private boolean requiresAuthentication() {
        return StringUtils.hasText(smtpUsername);
    }

    private String buildPowerShellMailScript(String smtpHost,
                                             String smtpPort,
                                             String smtpUsername,
                                             String smtpPassword,
                                             String smtpFrom,
                                             String smtpSsl,
                                             String toEmail,
                                             String subject,
                                             String body,
                                             boolean htmlBody) {
        String encodedSubject = encodeBase64(subject);
        String encodedBody = encodeBase64(body);

        return """
                [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;
                $utf8 = [System.Text.Encoding]::UTF8;
                $subject = $utf8.GetString([System.Convert]::FromBase64String('%s'));
                $body = $utf8.GetString([System.Convert]::FromBase64String('%s'));
                $credential = New-Object System.Management.Automation.PSCredential('%s', (ConvertTo-SecureString '%s' -AsPlainText -Force));
                Send-MailMessage -SmtpServer '%s' -Port %s -UseSsl -Credential $credential -From '%s' -To '%s' -Subject $subject -Body $body -BodyAsHtml:$%s -Encoding UTF8;
                """.formatted(
                encodedSubject,
                encodedBody,
                escapePowerShell(smtpUsername),
                escapePowerShell(smtpPassword),
                escapePowerShell(smtpHost),
                escapePowerShell(smtpPort),
                escapePowerShell(smtpFrom),
                escapePowerShell(toEmail),
                htmlBody
        );
    }

    private String encodeBase64(String value) {
        return Base64.getEncoder().encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private String escapePowerShell(String value) {
        return (value == null ? "" : value).replace("'", "''");
    }

    private void writeOutboxCopy(String toEmail, String subject, String body, boolean htmlBody) {
        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + "-notification.txt";
        Path targetFile = outboxRoot.resolve(fileName);
        String output = "To: " + toEmail + System.lineSeparator()
                + "Subject: " + subject + System.lineSeparator()
                + "Content-Type: " + (htmlBody ? "text/html" : "text/plain") + System.lineSeparator()
                + System.lineSeparator()
                + body;
        try {
            Files.writeString(targetFile, output);
        } catch (IOException ignored) {
            // Fallback writing is best-effort only.
        }
    }
}
