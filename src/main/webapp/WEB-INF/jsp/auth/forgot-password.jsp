<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Forgot Password | LU Librisync</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>
<div class="auth-shell">
    <div class="auth-card hero-card">
        <section class="auth-story">
            <span class="tag-chip warn">Account Recovery</span>
            <h1 class="mt-3 mb-3 fw-bold">Reset your LU Librisync password</h1>
            <p class="fs-5 mb-0">Enter your registered email to continue.</p>
        </section>

        <section class="auth-form-wrap">
            <h2 class="fw-bold mb-2">Forgot password</h2>
            <p class="muted-text mb-4">Enter your email to request a password reset OTP.</p>

            <c:if test="${not empty success}">
                <div class="alert alert-success">${success}</div>
            </c:if>
            <c:if test="${not empty info}">
                <div class="alert alert-info">${info}</div>
            </c:if>
            <c:if test="${not empty error}">
                <div class="alert alert-danger">${error}</div>
            </c:if>

            <form method="post" action="${pageContext.request.contextPath}/forgot-password/request-otp" class="mb-4">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">

                <div class="mb-3">
                    <label class="form-label" for="email">Registered email</label>
                    <input class="form-control form-control-lg" id="email" name="email" type="email" value="${emailValue}" required>
                </div>

                <button class="btn btn-brand w-100" type="submit">Send OTP</button>
            </form>

            <c:if test="${hasPendingResetOtp or openResetPanel}">
                <div class="panel-card">
                    <div class="section-title mb-2">Complete password reset</div>
                    <c:if test="${hasPendingResetOtp}">
                        <div class="otp-panel mb-3">
                            <div class="otp-panel-icon"><i class="bi bi-envelope-paper"></i></div>
                            <div>
                                <strong>${empty maskedResetEmail ? 'Registered email' : maskedResetEmail}</strong>
                                <div class="small muted-text">
                                    OTP expires in <strong id="resetOtpExpiryCountdown">calculating...</strong>
                                    <span class="mx-1">|</span>
                                    New OTP in <strong id="resetOtpResendCountdown">calculating...</strong>
                                </div>
                            </div>
                        </div>
                    </c:if>

                    <form method="post" action="${pageContext.request.contextPath}/forgot-password/reset" class="mb-3">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <input type="hidden" name="email" value="${emailValue}">

                        <div class="mb-3">
                            <label class="form-label" for="otpCode">6-digit OTP</label>
                            <input class="form-control form-control-lg otp-input" id="otpCode" name="otpCode" maxlength="6" inputmode="numeric" pattern="[0-9]{6}" placeholder="Enter OTP" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label" for="newPassword">New password</label>
                            <input class="form-control form-control-lg" id="newPassword" name="newPassword" type="password" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label" for="confirmPassword">Confirm new password</label>
                            <input class="form-control form-control-lg" id="confirmPassword" name="confirmPassword" type="password" required>
                        </div>
                        <button class="btn btn-brand w-100" type="submit">Verify OTP and reset password</button>
                    </form>

                    <form method="post" action="${pageContext.request.contextPath}/forgot-password/resend-otp">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <input type="hidden" name="email" value="${emailValue}">
                        <button class="btn btn-warm w-100" type="submit" id="resendResetOtpButton">Resend OTP</button>
                    </form>
                </div>
            </c:if>

            <p class="mb-0 mt-4">Remembered your password? <a href="${pageContext.request.contextPath}/login">Back to login</a>.</p>
        </section>
    </div>
</div>

<script>
    (function () {
        var otpExpiresAtEpochMs = ${empty resetOtpExpiresAtEpochMs ? 'null' : resetOtpExpiresAtEpochMs};
        var otpResendAvailableAtEpochMs = ${empty resetOtpResendAvailableAtEpochMs ? 'null' : resetOtpResendAvailableAtEpochMs};
        var resendButton = document.getElementById("resendResetOtpButton");

        function formatCountdown(targetEpochMs) {
            if (!targetEpochMs) {
                return "not active";
            }

            var remainingMs = targetEpochMs - Date.now();
            if (remainingMs <= 0) {
                return "00:00";
            }

            var totalSeconds = Math.floor(remainingMs / 1000);
            var minutes = Math.floor(totalSeconds / 60);
            var seconds = totalSeconds % 60;
            return String(minutes).padStart(2, "0") + ":" + String(seconds).padStart(2, "0");
        }

        function updateCountdowns() {
            var expiryLabel = document.getElementById("resetOtpExpiryCountdown");
            var resendLabel = document.getElementById("resetOtpResendCountdown");

            if (expiryLabel) {
                expiryLabel.textContent = formatCountdown(otpExpiresAtEpochMs);
            }
            if (resendLabel) {
                resendLabel.textContent = formatCountdown(otpResendAvailableAtEpochMs);
            }
            if (resendButton) {
                resendButton.disabled = otpResendAvailableAtEpochMs && otpResendAvailableAtEpochMs > Date.now();
            }
        }

        updateCountdowns();
        window.setInterval(updateCountdowns, 1000);
    })();
</script>
</body>
</html>
