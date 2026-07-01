package com.latteandletters.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class StudentProfileOtpState implements Serializable {

    private StudentProfileUpdateRequest updateRequest;
    private LocalDateTime expiresAt;
    private String destinationEmail;
    private LocalDateTime resendAvailableAt;

    public StudentProfileOtpState() {
    }

    public StudentProfileOtpState(StudentProfileUpdateRequest updateRequest,
                                  LocalDateTime expiresAt,
                                  String destinationEmail,
                                  LocalDateTime resendAvailableAt) {
        this.updateRequest = updateRequest;
        this.expiresAt = expiresAt;
        this.destinationEmail = destinationEmail;
        this.resendAvailableAt = resendAvailableAt;
    }

    public StudentProfileUpdateRequest getUpdateRequest() {
        return updateRequest;
    }

    public void setUpdateRequest(StudentProfileUpdateRequest updateRequest) {
        this.updateRequest = updateRequest;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getDestinationEmail() {
        return destinationEmail;
    }

    public void setDestinationEmail(String destinationEmail) {
        this.destinationEmail = destinationEmail;
    }

    public LocalDateTime getResendAvailableAt() {
        return resendAvailableAt;
    }

    public void setResendAvailableAt(LocalDateTime resendAvailableAt) {
        this.resendAvailableAt = resendAvailableAt;
    }
}
