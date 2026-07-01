package com.lulibrisync.dto;

public class StudentProfileOtpDispatchResult {

    private final StudentProfileOtpState otpState;
    private final boolean sent;
    private final boolean delivered;
    private final boolean cooldownActive;
    private final long resendSecondsRemaining;

    public StudentProfileOtpDispatchResult(StudentProfileOtpState otpState,
                                           boolean sent,
                                           boolean delivered,
                                           boolean cooldownActive,
                                           long resendSecondsRemaining) {
        this.otpState = otpState;
        this.sent = sent;
        this.delivered = delivered;
        this.cooldownActive = cooldownActive;
        this.resendSecondsRemaining = resendSecondsRemaining;
    }

    public StudentProfileOtpState getOtpState() {
        return otpState;
    }

    public boolean isSent() {
        return sent;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public boolean isCooldownActive() {
        return cooldownActive;
    }

    public long getResendSecondsRemaining() {
        return resendSecondsRemaining;
    }
}
