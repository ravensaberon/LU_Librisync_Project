package com.latteandletters.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_profile_otp_requests")
public class StudentProfileOtpRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "pending_name", nullable = false, length = 100)
    private String pendingName;

    @Column(name = "pending_course", length = 100)
    private String pendingCourse;

    @Column(name = "pending_year_level", length = 60)
    private String pendingYearLevel;

    @Column(name = "pending_phone", length = 30)
    private String pendingPhone;

    @Column(name = "pending_address", length = 200)
    private String pendingAddress;

    @Column(name = "pending_date_of_birth")
    private LocalDate pendingDateOfBirth;

    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Column(name = "destination_email", nullable = false, length = 100)
    private String destinationEmail;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    @Column(name = "resend_available_at", nullable = false)
    private LocalDateTime resendAvailableAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getPendingName() {
        return pendingName;
    }

    public void setPendingName(String pendingName) {
        this.pendingName = pendingName;
    }

    public String getPendingCourse() {
        return pendingCourse;
    }

    public void setPendingCourse(String pendingCourse) {
        this.pendingCourse = pendingCourse;
    }

    public String getPendingYearLevel() {
        return pendingYearLevel;
    }

    public void setPendingYearLevel(String pendingYearLevel) {
        this.pendingYearLevel = pendingYearLevel;
    }

    public String getPendingPhone() {
        return pendingPhone;
    }

    public void setPendingPhone(String pendingPhone) {
        this.pendingPhone = pendingPhone;
    }

    public String getPendingAddress() {
        return pendingAddress;
    }

    public void setPendingAddress(String pendingAddress) {
        this.pendingAddress = pendingAddress;
    }

    public LocalDate getPendingDateOfBirth() {
        return pendingDateOfBirth;
    }

    public void setPendingDateOfBirth(LocalDate pendingDateOfBirth) {
        this.pendingDateOfBirth = pendingDateOfBirth;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public String getDestinationEmail() {
        return destinationEmail;
    }

    public void setDestinationEmail(String destinationEmail) {
        this.destinationEmail = destinationEmail;
    }

    public LocalDateTime getLastSentAt() {
        return lastSentAt;
    }

    public void setLastSentAt(LocalDateTime lastSentAt) {
        this.lastSentAt = lastSentAt;
    }

    public LocalDateTime getResendAvailableAt() {
        return resendAvailableAt;
    }

    public void setResendAvailableAt(LocalDateTime resendAvailableAt) {
        this.resendAvailableAt = resendAvailableAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
