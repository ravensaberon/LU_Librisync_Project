package com.lulibrisync.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PasswordResetOtpState implements Serializable {

    private String email;
    private String maskedEmail;
    private LocalDateTime expiresAt;
    private LocalDateTime resendAvailableAt;

    public PasswordResetOtpState() {
    }

    public PasswordResetOtpState(String email,
                                 String maskedEmail,
                                 LocalDateTime expiresAt,
                                 LocalDateTime resendAvailableAt) {
        this.email = email;
        this.maskedEmail = maskedEmail;
        this.expiresAt = expiresAt;
        this.resendAvailableAt = resendAvailableAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMaskedEmail() {
        return maskedEmail;
    }

    public void setMaskedEmail(String maskedEmail) {
        this.maskedEmail = maskedEmail;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getResendAvailableAt() {
        return resendAvailableAt;
    }

    public void setResendAvailableAt(LocalDateTime resendAvailableAt) {
        this.resendAvailableAt = resendAvailableAt;
    }
}
