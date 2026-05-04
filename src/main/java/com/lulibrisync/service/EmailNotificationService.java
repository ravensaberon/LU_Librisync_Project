package com.lulibrisync.service;

import com.lulibrisync.model.AdminNotificationType;
import com.lulibrisync.model.EmailNotification;
import com.lulibrisync.model.EmailNotificationStatus;
import com.lulibrisync.model.EmailNotificationType;
import com.lulibrisync.model.IssueRecord;
import com.lulibrisync.model.IssueStatus;
import com.lulibrisync.model.Reservation;
import com.lulibrisync.model.ReservationStatus;
import com.lulibrisync.model.User;
import com.lulibrisync.repository.EmailNotificationRepository;
import com.lulibrisync.repository.IssueRecordRepository;
import com.lulibrisync.repository.ReservationRepository;
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

@Service
public class EmailNotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a", Locale.ENGLISH);

    private final EmailNotificationRepository emailNotificationRepository;
    private final IssueRecordRepository issueRecordRepository;
    private final ReservationRepository reservationRepository;
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
                                    AdminNotificationService adminNotificationService,
                                    @Value("${lulibrisync.smtp.host:}") String smtpHost,
                                    @Value("${lulibrisync.smtp.port:587}") String smtpPort,
                                    @Value("${lulibrisync.smtp.username:lulibrisync@gmail.com}") String smtpUsername,
                                    @Value("${lulibrisync.smtp.password:}") String smtpPassword,
                                    @Value("${lulibrisync.smtp.from:lulibrisync@gmail.com}") String smtpFrom,
                                    @Value("${lulibrisync.smtp.ssl:true}") String smtpSsl,
                                    @Value("${lulibrisync.notification.outbox-root:${lulibrisync.storage.root:${user.dir}/storage}/email-outbox}") String outboxRootPath) {
        this.emailNotificationRepository = emailNotificationRepository;
        this.issueRecordRepository = issueRecordRepository;
        this.reservationRepository = reservationRepository;
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
                      <div style="font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.88;">LU Librisync</div>
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
                      This is an automated message from LU Librisync. Please do not reply to this email.
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
                          <div style="font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.88;">LU Librisync</div>
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
                          This is an automated message from LU Librisync. Please do not reply to this email.
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
                          <div style="font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.88;">LU Librisync</div>
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
                          This is an automated message from LU Librisync. Please do not reply to this email.
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
                    || notification.getNotificationType() == EmailNotificationType.RESERVATION_READY;
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
                && recipient.getId().equals(notification.getUser().getId())
                && subject.equals(notification.getSubject())
                && body.equals(notification.getBody())
                && scheduledAt.equals(notification.getScheduledAt())
                && (EmailNotificationStatus.PENDING.equals(notification.getStatus())
                || EmailNotificationStatus.SENT.equals(notification.getStatus())
                || EmailNotificationStatus.FAILED.equals(notification.getStatus()))) {
            return;
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
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
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
        String credentialsBlock = "";
        if (StringUtils.hasText(smtpUsername)) {
            credentialsBlock = "$client.Credentials = New-Object System.Net.NetworkCredential('" + escapePowerShell(smtpUsername) + "', '" + escapePowerShell(smtpPassword) + "');";
        }

        return """
                $utf8 = [System.Text.Encoding]::UTF8;
                $message = New-Object System.Net.Mail.MailMessage;
                $message.From = '%s';
                $message.To.Add('%s');
                $message.Subject = $utf8.GetString([System.Convert]::FromBase64String('%s'));
                $message.Body = $utf8.GetString([System.Convert]::FromBase64String('%s'));
                $message.IsBodyHtml = [System.Convert]::ToBoolean('%s');
                $client = New-Object System.Net.Mail.SmtpClient('%s', %s);
                $client.EnableSsl = [System.Convert]::ToBoolean('%s');
                %s
                $client.Send($message);
                """.formatted(
                escapePowerShell(smtpFrom),
                escapePowerShell(toEmail),
                encodedSubject,
                encodedBody,
                htmlBody,
                escapePowerShell(smtpHost),
                escapePowerShell(smtpPort),
                escapePowerShell(smtpSsl),
                credentialsBlock
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
