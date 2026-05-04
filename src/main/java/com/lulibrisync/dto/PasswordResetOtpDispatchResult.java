package com.lulibrisync.dto;

public class PasswordResetOtpDispatchResult {

    private final PasswordResetOtpState otpState;
    private final boolean sent;
    private final boolean delivered;
    private final boolean cooldownActive;

    public PasswordResetOtpDispatchResult(PasswordResetOtpState otpState,
                                          boolean sent,
                                          boolean delivered,
                                          boolean cooldownActive) {
        this.otpState = otpState;
        this.sent = sent;
        this.delivered = delivered;
        this.cooldownActive = cooldownActive;
    }

    public PasswordResetOtpState getOtpState() {
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
}
