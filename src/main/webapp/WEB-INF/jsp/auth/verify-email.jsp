<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Verify Email | Latte and Letters</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260430-auth-back-link">
</head>
<body>
<div class="auth-shell auth-shell-library">
    <div class="auth-card hero-card">

        <section class="auth-story auth-story-visual" aria-hidden="true">
            <span class="tag-chip warn">Email Verification</span>
            <h1 class="mt-3 mb-3 fw-bold" style="font-size:clamp(1.9rem,2.8vw,2.8rem);line-height:1.1;">
                One last step to activate your account.
            </h1>
            <p style="font-size:1.05rem;opacity:0.9;line-height:1.6;max-width:340px;">
                We sent a 6-digit code to your email. Enter it on this page to verify your identity and activate your Latte and Letters student account.
            </p>
            <div style="margin-top:32px;display:flex;flex-direction:column;gap:14px;">
                <div style="display:flex;align-items:flex-start;gap:14px;">
                    <div style="flex-shrink:0;width:40px;height:40px;border-radius:12px;background:rgba(255,255,255,0.14);display:flex;align-items:center;justify-content:center;font-size:1.1rem;">
                        <i class="bi bi-envelope-check-fill"></i>
                    </div>
                    <div>
                        <strong style="display:block;font-size:0.95rem;font-weight:700;margin-bottom:2px;">Check your inbox</strong>
                        <span style="font-size:0.84rem;opacity:0.82;line-height:1.5;">The code was sent to the email you registered with.</span>
                    </div>
                </div>
                <div style="display:flex;align-items:flex-start;gap:14px;">
                    <div style="flex-shrink:0;width:40px;height:40px;border-radius:12px;background:rgba(255,255,255,0.14);display:flex;align-items:center;justify-content:center;font-size:1.1rem;">
                        <i class="bi bi-clock-fill"></i>
                    </div>
                    <div>
                        <strong style="display:block;font-size:0.95rem;font-weight:700;margin-bottom:2px;">Code expires in 15 minutes</strong>
                        <span style="font-size:0.84rem;opacity:0.82;line-height:1.5;">Request a new code if yours has expired.</span>
                    </div>
                </div>
                <div style="display:flex;align-items:flex-start;gap:14px;">
                    <div style="flex-shrink:0;width:40px;height:40px;border-radius:12px;background:rgba(255,255,255,0.14);display:flex;align-items:center;justify-content:center;font-size:1.1rem;">
                        <i class="bi bi-shield-lock-fill"></i>
                    </div>
                    <div>
                        <strong style="display:block;font-size:0.95rem;font-weight:700;margin-bottom:2px;">Keep it private</strong>
                        <span style="font-size:0.84rem;opacity:0.82;line-height:1.5;">Never share your verification code with anyone.</span>
                    </div>
                </div>
            </div>
        </section>

        <section class="auth-form-wrap auth-form-panel">
            <div class="auth-utility-bar">
                <a class="auth-back-link" href="${pageContext.request.contextPath}/register">
                    <i class="bi bi-arrow-left"></i>
                    <span>Back to registration</span>
                </a>
            </div>

            <div class="auth-panel-heading">
                <h2 class="auth-panel-title">Verify your <span class="auth-panel-title-accent">email</span></h2>
                <p class="auth-panel-copy">Enter the 6-digit code we sent to your email address to activate your account.</p>
            </div>

            <div class="auth-role-label">Email Verification</div>

            <c:if test="${not empty success}">
                <div class="alert alert-success">${success}</div>
            </c:if>
            <c:if test="${not empty error}">
                <div class="alert alert-danger">${error}</div>
            </c:if>

            <%-- OTP status strip --%>
            <div class="otp-panel mb-4">
                <div class="otp-panel-icon"><i class="bi bi-envelope-paper"></i></div>
                <div>
                    <strong>${empty maskedEmail ? 'Your registered email' : maskedEmail}</strong>
                    <div class="small muted-text">
                        Code expires in <strong id="otpExpiryCountdown">calculating...</strong>
                        <span class="mx-1">|</span>
                        Resend in <strong id="otpResendCountdown">calculating...</strong>
                    </div>
                </div>
            </div>

            <%-- Verify form --%>
            <form method="post" action="${pageContext.request.contextPath}/register/verify" class="mb-3">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <input type="hidden" name="email" value="${emailValue}">

                <div class="mb-4">
                    <label class="form-label" for="otpCode">Verification code</label>
                    <input class="form-control form-control-lg otp-input"
                           id="otpCode"
                           name="otpCode"
                           type="text"
                           inputmode="numeric"
                           pattern="[0-9]{6}"
                           maxlength="6"
                           placeholder="Enter 6-digit code"
                           autocomplete="one-time-code"
                           required>
                </div>

                <button class="btn btn-brand auth-primary-btn w-100 mb-3" type="submit">
                    <i class="bi bi-check2-circle"></i>
                    Verify and activate account
                </button>
            </form>

            <%-- Resend form --%>
            <form method="post" action="${pageContext.request.contextPath}/register/resend-otp" class="mb-4">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <input type="hidden" name="email" value="${emailValue}">
                <button class="btn btn-warm w-100" type="submit" id="resendOtpButton">
                    <i class="bi bi-arrow-repeat"></i>
                    Resend verification code
                </button>
            </form>

            <div class="auth-support-row">
                <span class="text-muted">Wrong email?</span>
                <a class="auth-support-link" href="${pageContext.request.contextPath}/register">Register again</a>
            </div>
        </section>
    </div>
</div>

<script>
    (function () {
        var otpExpiresAtEpochMs = ${empty otpExpiresAtEpochMs ? 'null' : otpExpiresAtEpochMs};
        var otpResendAvailableAtEpochMs = ${empty otpResendAvailableAtEpochMs ? 'null' : otpResendAvailableAtEpochMs};
        var resendButton = document.getElementById("resendOtpButton");

        function formatCountdown(targetEpochMs) {
            if (!targetEpochMs) return "—";
            var remainingMs = targetEpochMs - Date.now();
            if (remainingMs <= 0) return "00:00";
            var totalSeconds = Math.floor(remainingMs / 1000);
            var minutes = Math.floor(totalSeconds / 60);
            var seconds = totalSeconds % 60;
            return String(minutes).padStart(2, "0") + ":" + String(seconds).padStart(2, "0");
        }

        function updateCountdowns() {
            var expiryEl = document.getElementById("otpExpiryCountdown");
            var resendEl = document.getElementById("otpResendCountdown");
            if (expiryEl) expiryEl.textContent = formatCountdown(otpExpiresAtEpochMs);
            if (resendEl) resendEl.textContent = formatCountdown(otpResendAvailableAtEpochMs);
            if (resendButton) {
                resendButton.disabled = !!(otpResendAvailableAtEpochMs && otpResendAvailableAtEpochMs > Date.now());
            }
        }

        updateCountdowns();
        window.setInterval(updateCountdowns, 1000);

        // Auto-submit when 6 digits are entered
        var otpInput = document.getElementById("otpCode");
        if (otpInput) {
            otpInput.addEventListener("input", function () {
                if (otpInput.value.replace(/\D/g, "").length === 6) {
                    otpInput.value = otpInput.value.replace(/\D/g, "");
                    otpInput.closest("form").submit();
                }
            });
        }
    })();
</script>
</body>
</html>
