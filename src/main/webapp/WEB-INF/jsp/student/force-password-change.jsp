<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Set Your Password | Latte and Letters</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260504-password-setup-refresh">
    <style>
        .password-setup-shell {
            position: relative;
            isolation: isolate;
        }
        .password-setup-shell::before {
            content: "";
            position: absolute;
            inset: 0;
            background:
                radial-gradient(circle at 10% 18%, rgba(255, 255, 255, 0.18), transparent 18%),
                linear-gradient(120deg, rgba(3, 18, 7, 0.72) 0%, rgba(7, 34, 13, 0.34) 42%, rgba(255, 255, 255, 0.06) 100%);
            z-index: -1;
        }
        .password-setup-story {
            min-height: clamp(600px, 80vh, 860px);
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            gap: 28px;
        }
        .password-setup-kicker {
            display: inline-flex;
            align-items: center;
            gap: 10px;
            width: fit-content;
            padding: 10px 16px;
            border-radius: 999px;
            background: rgba(255, 255, 255, 0.14);
            border: 1px solid rgba(255, 255, 255, 0.14);
            color: #f5fff7;
            font-size: 0.8rem;
            font-weight: 800;
            letter-spacing: 0.12em;
            text-transform: uppercase;
        }
        .password-setup-headline {
            max-width: 560px;
            margin: 18px 0 14px;
            font-size: clamp(2.7rem, 4.6vw, 4.8rem);
            line-height: 0.98;
            letter-spacing: -0.05em;
            font-weight: 900;
            color: #ffffff;
            font-family: "Poppins", "Manrope", sans-serif;
        }
        .password-setup-headline span {
            color: #9ff1c8;
        }
        .password-setup-lead {
            max-width: 520px;
            margin: 0;
            color: rgba(255, 255, 255, 0.86);
            font-size: 1.08rem;
            line-height: 1.75;
        }
        .password-setup-story-grid {
            display: grid;
            gap: 14px;
            max-width: 560px;
        }
        .password-setup-story-card {
            display: grid;
            grid-template-columns: 54px minmax(0, 1fr);
            gap: 16px;
            align-items: start;
            padding: 18px 20px;
            border-radius: 24px;
            background: linear-gradient(180deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.08));
            border: 1px solid rgba(255, 255, 255, 0.14);
            backdrop-filter: blur(10px);
            box-shadow: 0 20px 34px rgba(0, 0, 0, 0.12);
        }
        .password-setup-story-icon {
            width: 54px;
            height: 54px;
            display: grid;
            place-items: center;
            border-radius: 18px;
            background: rgba(255, 255, 255, 0.14);
            color: #ffffff;
            font-size: 1.18rem;
        }
        .password-setup-story-card strong {
            display: block;
            margin-bottom: 4px;
            color: #ffffff;
            font-size: 1rem;
            font-weight: 800;
        }
        .password-setup-story-card span {
            color: rgba(255, 255, 255, 0.82);
            line-height: 1.65;
            font-size: 0.93rem;
        }
        .password-setup-panel {
            position: relative;
            overflow: hidden;
        }
        .password-setup-panel::before {
            content: "";
            position: absolute;
            inset: 0 0 auto 0;
            height: 7px;
            background: linear-gradient(90deg, #0f7f34, #37c487, #8ae5ba);
        }
        .password-setup-panel > * {
            position: relative;
        }
        .password-setup-header {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;
            gap: 18px;
            margin-bottom: 24px;
        }
        .password-setup-badge {
            min-width: 58px;
            height: 58px;
            display: grid;
            place-items: center;
            border-radius: 20px;
            background: linear-gradient(135deg, rgba(15, 127, 52, 0.14), rgba(55, 196, 135, 0.2));
            color: #149048;
            font-size: 1.32rem;
            box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.45);
        }
        .password-setup-eyebrow {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 12px;
            color: #149048;
            font-size: 0.82rem;
            font-weight: 800;
            letter-spacing: 0.18em;
            text-transform: uppercase;
        }
        .password-setup-copy {
            max-width: 430px;
            margin-top: 12px;
            color: #667182;
            font-size: 1rem;
            line-height: 1.65;
        }
        .temp-pw-box {
            position: relative;
            padding: 20px 22px;
            border-radius: 24px;
            background: linear-gradient(180deg, #fffdf7, #fff5db);
            border: 1px solid #f0d59b;
            margin-bottom: 26px;
            box-shadow: 0 16px 28px rgba(124, 74, 0, 0.08);
        }
        .temp-pw-meta {
            display: flex;
            flex-wrap: wrap;
            gap: 8px 12px;
            margin-bottom: 12px;
            color: #8a5d12;
            font-size: 0.79rem;
            font-weight: 800;
            letter-spacing: 0.08em;
            text-transform: uppercase;
        }
        .temp-pw-value {
            font-family: monospace;
            display: inline-flex;
            align-items: center;
            max-width: 100%;
            padding: 10px 14px;
            border-radius: 14px;
            background: rgba(124, 74, 0, 0.08);
            font-size: 1.16rem;
            font-weight: 700;
            letter-spacing: 0.08em;
            color: #7c4a00;
            word-break: break-all;
        }
        .password-setup-form-grid {
            display: grid;
            gap: 22px;
        }
        .password-setup-form-label {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            margin-bottom: 10px;
        }
        .password-setup-form-label span {
            font-weight: 800;
            color: #2d3848;
            letter-spacing: 0.08em;
            text-transform: uppercase;
            font-size: 0.86rem;
        }
        .password-setup-form-hint {
            color: #7b8798;
            font-size: 0.8rem;
            font-weight: 700;
        }
        .pw-strength-bar {
            height: 8px;
            border-radius: 999px;
            background: #e5ebf2;
            overflow: hidden;
            margin-top: 10px;
        }
        .pw-strength-fill {
            height: 100%;
            border-radius: 999px;
            width: 0;
            transition: width 0.3s ease, background 0.3s ease;
        }
        .pw-strength-label {
            font-size: 0.84rem;
            margin-top: 7px;
            font-weight: 700;
        }
        .pw-req-list {
            list-style: none;
            padding: 0;
            margin: 14px 0 0;
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px 16px;
        }
        .pw-req-list li {
            font-size: 0.84rem;
            color: #667182;
            display: flex;
            align-items: center;
            gap: 6px;
        }
        .pw-req-list li.met {
            color: #1b6a35;
        }
        .pw-req-list li i {
            font-size: 0.75rem;
        }
        .password-setup-actions {
            display: grid;
            gap: 14px;
            margin-top: 10px;
        }
        .password-setup-later {
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            justify-content: space-between;
            gap: 14px;
            padding: 16px 18px;
            border-radius: 20px;
            border: 1px solid rgba(15, 127, 52, 0.1);
            background: linear-gradient(180deg, rgba(246, 252, 247, 0.96), rgba(255, 255, 255, 0.98));
        }
        .password-setup-later-copy strong {
            display: block;
            color: #2a3340;
            font-size: 0.96rem;
            margin-bottom: 4px;
        }
        .password-setup-later-copy span {
            color: #6e7989;
            font-size: 0.88rem;
            line-height: 1.55;
        }
        .password-setup-later-button {
            min-height: 48px;
            padding: 0 18px;
            border-radius: 999px;
            border: 1px solid rgba(15, 127, 52, 0.14);
            background: #ffffff;
            color: var(--primary-900);
            font-weight: 800;
        }
        @media (max-width: 991.98px) {
            .password-setup-story {
                min-height: auto;
                gap: 22px;
            }
            .password-setup-headline {
                max-width: 100%;
            }
        }
        @media (max-width: 767.98px) {
            .password-setup-header {
                flex-direction: column;
            }
            .pw-req-list {
                grid-template-columns: 1fr;
            }
            .password-setup-later {
                align-items: flex-start;
            }
            .password-setup-later form,
            .password-setup-later-button {
                width: 100%;
            }
        }
    </style>
</head>
<body>
<div class="auth-shell auth-shell-library password-setup-shell">
    <div class="auth-card hero-card" style="grid-template-columns:1fr;max-width:680px;">
        <section class="auth-story auth-story-visual" aria-hidden="true" style="display:none;"></section>

        <section class="auth-form-wrap auth-form-panel password-setup-panel" style="width:100%;">
            <div class="password-setup-header">
                <div class="auth-panel-heading mb-0">
                    <div class="password-setup-eyebrow">
                        <i class="bi bi-stars"></i>
                        Set your password
                    </div>
                    <h2 class="auth-panel-title">Welcome, <span class="auth-panel-title-accent">${student.user.firstName}</span></h2>
                    <p class="password-setup-copy">
                        Your account is ready. Check your email for the temporary password, then set a new personal password before using the system.
                    </p>
                </div>
                <div class="password-setup-badge" aria-hidden="true">
                    <i class="bi bi-key-fill"></i>
                </div>
            </div>

            <div class="auth-role-label">First Login</div>

            <c:if test="${not empty success}">
                <div class="alert alert-success">${success}</div>
            </c:if>
            <c:if test="${not empty error}">
                <div class="alert alert-danger">${error}</div>
            </c:if>

            <div class="temp-pw-box">
                <div class="temp-pw-meta">
                    <span><i class="bi bi-envelope-check me-1"></i>Temporary password sent by email</span>
                    <span>Student ID: ${student.studentId}</span>
                </div>
                <div class="small mt-1" style="color:#8a5d12;opacity:0.92;line-height:1.65;">
                    For security, the generated password is no longer shown on this page. Use the email copy for first sign-in only, then replace it here.
                </div>
            </div>

            <form id="forcePasswordForm" method="post" action="${pageContext.request.contextPath}/student/password/change-temporary" novalidate>
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">

                <div class="password-setup-form-grid">
                    <%-- New + Confirm side by side --%>
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="password-setup-form-label" for="newPassword">
                                <span>New password</span>
                            </label>
                            <div class="auth-input-shell">
                                <span class="auth-input-icon"><i class="bi bi-lock-fill"></i></span>
                                <input class="form-control" id="newPassword" name="newPassword"
                                       type="password" maxlength="100"
                                       autocomplete="new-password">
                                <button class="auth-password-toggle" id="toggleNewPassword" type="button" aria-label="Show password">
                                    <i class="bi bi-eye-fill"></i>
                                </button>
                            </div>
                            <p class="field-error" id="newPasswordError" style="min-height:20px;margin-top:6px;color:#9d2f2a;font-size:0.84rem;"></p>
                        </div>
                        <div class="col-md-6">
                            <label class="password-setup-form-label" for="confirmPassword">
                                <span>Confirm password</span>
                            </label>
                            <div class="auth-input-shell">
                                <span class="auth-input-icon"><i class="bi bi-lock-fill"></i></span>
                                <input class="form-control" id="confirmPassword" name="confirmPassword"
                                       type="password" maxlength="100"
                                       autocomplete="new-password">
                                <button class="auth-password-toggle" id="toggleConfirmPassword" type="button" aria-label="Show confirm password">
                                    <i class="bi bi-eye-fill"></i>
                                </button>
                            </div>
                            <p class="field-error" id="confirmPasswordError" style="min-height:20px;margin-top:6px;color:#9d2f2a;font-size:0.84rem;"></p>
                        </div>
                    </div>

                    <%-- Strength bar + requirements below both fields --%>
                    <div>
                        <div class="pw-strength-bar"><div class="pw-strength-fill" id="pwStrengthFill"></div></div>
                        <div class="pw-strength-label" id="pwStrengthLabel" style="color:var(--muted);">Enter a password</div>
                        <ul class="pw-req-list" id="pwReqList">
                            <li id="req-length"><i class="bi bi-x-circle-fill"></i> At least 12 characters</li>
                            <li id="req-upper"><i class="bi bi-x-circle-fill"></i> Uppercase letter</li>
                            <li id="req-lower"><i class="bi bi-x-circle-fill"></i> Lowercase letter</li>
                            <li id="req-number"><i class="bi bi-x-circle-fill"></i> Number</li>
                            <li id="req-special"><i class="bi bi-x-circle-fill"></i> Special character</li>
                        </ul>
                    </div>

                    <div class="password-setup-actions">
                        <button class="btn btn-brand auth-primary-btn w-100" type="submit" id="submitBtn">
                            <i class="bi bi-check2-circle me-2"></i>Set password and continue
                        </button>
                    </div>
                </div>
            </form>

        </section>
    </div>
</div>

<script>
    (function () {
        var newPassword = document.getElementById("newPassword");
        var confirmPassword = document.getElementById("confirmPassword");
        var newPasswordError = document.getElementById("newPasswordError");
        var confirmPasswordError = document.getElementById("confirmPasswordError");
        var strengthFill = document.getElementById("pwStrengthFill");
        var strengthLabel = document.getElementById("pwStrengthLabel");
        var submitBtn = document.getElementById("submitBtn");

        function wireToggle(inputId, btnId) {
            var input = document.getElementById(inputId);
            var btn = document.getElementById(btnId);
            if (!input || !btn) return;
            btn.addEventListener("click", function () {
                var showing = input.type === "text";
                input.type = showing ? "password" : "text";
                btn.innerHTML = showing
                    ? '<i class="bi bi-eye-fill"></i>'
                    : '<i class="bi bi-eye-slash-fill"></i>';
                btn.setAttribute("aria-label", showing ? "Show password" : "Hide password");
            });
        }

        wireToggle("newPassword", "toggleNewPassword");
        wireToggle("confirmPassword", "toggleConfirmPassword");

        var PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d\s]).{12,100}$/;

        function checkReq(id, met) {
            var el = document.getElementById(id);
            if (!el) return;
            el.classList.toggle("met", met);
            el.querySelector("i").className = met ? "bi bi-check-circle-fill" : "bi bi-x-circle-fill";
        }

        function scorePassword(pw) {
            if (!pw) return 0;
            var score = 0;
            if (pw.length >= 12) score++;
            if (/[A-Z]/.test(pw)) score++;
            if (/[a-z]/.test(pw)) score++;
            if (/\d/.test(pw)) score++;
            if (/[^A-Za-z\d\s]/.test(pw)) score++;
            return score;
        }

        function updateStrengthBar(pw) {
            var score = scorePassword(pw);
            var pct = pw ? (score / 5) * 100 : 0;
            var color;
            var label;

            if (!pw) {
                color = "#e2e8f0";
                label = "Enter a password";
            } else if (score <= 2) {
                color = "#e53e3e";
                label = "Weak";
            } else if (score === 3) {
                color = "#dd6b20";
                label = "Fair";
            } else if (score === 4) {
                color = "#d69e2e";
                label = "Good";
            } else {
                color = "#38a169";
                label = "Strong";
            }

            strengthFill.style.width = pct + "%";
            strengthFill.style.background = color;
            strengthLabel.textContent = label;
            strengthLabel.style.color = color;
        }

        function validateNewPassword() {
            var pw = newPassword.value;

            checkReq("req-length", pw.length >= 12);
            checkReq("req-upper", /[A-Z]/.test(pw));
            checkReq("req-lower", /[a-z]/.test(pw));
            checkReq("req-number", /\d/.test(pw));
            checkReq("req-special", /[^A-Za-z\d\s]/.test(pw));
            updateStrengthBar(pw);

            if (!pw) {
                newPasswordError.textContent = "New password is required.";
                return false;
            }
            if (!PASSWORD_PATTERN.test(pw)) {
                newPasswordError.textContent = "Password must be at least 12 characters with uppercase, lowercase, number, and special character.";
                return false;
            }

            newPasswordError.textContent = "";
            return true;
        }

        function validateConfirmPassword() {
            var pw = newPassword.value;
            var cpw = confirmPassword.value;

            if (!cpw) {
                confirmPasswordError.textContent = "Please confirm your new password.";
                return false;
            }
            if (pw !== cpw) {
                confirmPasswordError.textContent = "Passwords do not match.";
                return false;
            }

            confirmPasswordError.textContent = "";
            return true;
        }

        newPassword.addEventListener("input", function () {
            validateNewPassword();
            if (confirmPassword.value) {
                validateConfirmPassword();
            }
        });

        confirmPassword.addEventListener("input", validateConfirmPassword);

        document.getElementById("forcePasswordForm").addEventListener("submit", function (e) {
            var ok = validateNewPassword() & validateConfirmPassword();
            if (!ok) {
                e.preventDefault();
                return;
            }
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Saving...';
        });
    })();
</script>
</body>
</html>
