package com.lulibrisync.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Entity
@Table(name = "reservations")
public class Reservation {

    private static final String DESK_QR_PREFIX = "LU-RES";
    private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "queue_position", nullable = false)
    private Integer queuePosition = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 20)
    private ReservationRequestType requestType = ReservationRequestType.RESERVATION;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "preferred_pickup_date")
    private LocalDate preferredPickupDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (reservedAt == null) {
            reservedAt = now;
        }
        createdAt = now;
    }

    public boolean isActive() {
        return ReservationStatus.PENDING.equals(status) || ReservationStatus.PENDING_APPROVAL.equals(status) || ReservationStatus.READY.equals(status);
    }

    public boolean isBorrowRequest() {
        return ReservationRequestType.BORROW.equals(requestType);
    }

    public boolean isQueueReservation() {
        return ReservationRequestType.RESERVATION.equals(requestType);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Integer getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(Integer queuePosition) {
        this.queuePosition = queuePosition;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public ReservationRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(ReservationRequestType requestType) {
        this.requestType = requestType;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(LocalDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }

    public String getReservedAtDisplay() {
        return reservedAt == null ? "" : DISPLAY_DATE_TIME_FORMATTER.format(reservedAt);
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getExpiresAtDisplay() {
        return expiresAt == null ? "" : DISPLAY_DATE_TIME_FORMATTER.format(expiresAt);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getPreferredPickupDate() {
        return preferredPickupDate;
    }

    public void setPreferredPickupDate(LocalDate preferredPickupDate) {
        this.preferredPickupDate = preferredPickupDate;
    }

    public String getPreferredPickupDateDisplay() {
        return preferredPickupDate == null ? "" : DISPLAY_DATE_FORMATTER.format(preferredPickupDate);
    }

    public String getDeskQrCode() {
        if (id == null || student == null || requestType == null) {
            return "";
        }

        return DESK_QR_PREFIX
                + "-" + id
                + "-" + sanitizeDeskQrSegment(student.getStudentId())
                + "-" + requestType.name();
    }

    private String sanitizeDeskQrSegment(String value) {
        if (value == null || value.isBlank()) {
            return "STUDENT";
        }
        return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ENGLISH);
    }
}
