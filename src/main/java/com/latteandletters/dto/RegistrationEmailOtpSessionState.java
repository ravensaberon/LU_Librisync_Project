package com.latteandletters.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class RegistrationEmailOtpSessionState implements Serializable {

    private String email;
    private String maskedEmail;
    private String otpHash;
    private LocalDateTime expiresAt;
    private LocalDateTime resendAvailableAt;
    private boolean verified;

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

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
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

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
