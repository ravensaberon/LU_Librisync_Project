<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Set Your Password | LU Librisync</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260430-auth-back-link">
    <style>
        .pw-strength-bar {
            height: 6px;
            border-radius: 999px;
            background: #e2e8f0;
            overflow: hidden;
            margin-top: 8px;
        }
        .pw-strength-fill {
            height: 100%;
            border-radius: 999px;
            width: 0;
            transition: width 0.3s ease, background 0.3s ease;
        }
        .pw-strength-label {
            font-size: 0.8rem;
            margin-top: 5px;
            font-weight: 700;
        }
        .pw-req-list {
            list-style: none;
            padding: 0;
            margin: 10px 0 0;
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 4px 12px;
        }
        .pw-req-list li {
            font-size: 0.8rem;
            color: var(--muted);
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
        .temp-pw-box {
            padding: 14px 18px;
            border-radius: 16px;
            background: #fffbea;
            border: 1px solid #f1ddb1;
            margin-bottom: 24px;
        }
        .temp-pw-value {
            font-family: monospace;
            font-size: 1.1rem;
            font-weight: 700;
            letter-spacing: 0.08em;
            color: #7c4a00;
            word-break: break-all;
        }
    </style>
</head>
<body>
<div class="auth-shell auth-shell-library">
    <div class="auth-card hero-card">

        <%-- Left visual panel --%>
        <section class="auth-story auth-story-visual" aria-hidden="true">
            <span class="tag-chip warn">First Login</span>
            <h1 class="mt-3 mb-3 fw-bold" style="font-size:clamp(1.9rem,2.8vw,2.8rem);line-height:1.1;">
                Set your own password before you continue.
            </h1>
            <p style="font-size:1.05rem;opacity:0.9;line-height:1.6;max-width:340px;">
                Your account was created with a temporary password. Choose a strong personal password to secure your LU Librisync account.
            </p>
            <div style="margin-top:32px;display:flex;flex-direction:column;gap:14px;">
                <div style="display:flex;align-items:flex-start;gap:14px;">
                    <div style="flex-shrink:0;width:40px;height:40px;border-radius:12px;background:rgba(255,255,255,0.14);display:flex;align-items:center;justify-content:center;font-size:1.1rem;">
                        <i class="bi bi-lock-fill"></i>
                    </div>
                    <div>
                        <strong style="display:block;font-size:0.95rem;font-weight:700;margin-bottom:2px;">Minimum 12 characters</strong>
                        <span style="font-size:0.84rem;opacity:0.82;line-height:1.5;">Include uppercase, lowercase, a number, and a special character.</span>
                    </div>
                </div>
                <div style="display:flex;align-items:flex-start;gap:14px;">
                    <div style="flex-shrink:0;width:40px;height:40px;border-radius:12px;background:rgba(255,255,255,0.14);display:flex;align-items:center;justify-content:center;font-size:1.1rem;">
                        <i class="bi bi-person-lock"></i>
                    </div>
                    <div>
                        <strong style="display:block;font-size:0.95rem;font-weight:700;margin-bottom:2px;">Keep it personal</strong>
                        <span style="font-size:0.84rem;opacity:0.82;line-height:1.5;">Don't share your password with anyone, including library staff.</span>
                    </div>
                </div>
                <div style="display:flex;align-items:flex-start;gap:14px;">
                    <div style="flex-shrink:0;width:40px;height:40px;border-radius:12px;background:rgba(255,255,255,0.14);display:flex;align-items:center;justify-content:center;font-size:1.1rem;">
                        <i class="bi bi-arrow-right-circle-fill"></i>
                    </div>
                    <div>
                        <strong style="display:block;font-size:0.95rem;font-weight:700;margin-bottom:2px;">One-time step</strong>
                        <span style="font-size:0.84rem;opacity:0.82;line-height:1.5;">You only need to do this once. After this, you'll go straight to your dashboard.</span>
                    </div>
                </div>
            </div>
        </section>

        <%-- Right form panel --%>
        <section class="auth-form-wrap auth-form-panel">
            <div class="auth-panel-heading">
                <h2 class="auth-panel-title">Set your <span class="auth-panel-title-accent">password</span></h2>
                <p class="auth-panel-copy">Welcome, ${student.user.name}. Create a strong password to secure your account.</p>
            </div>

            <div class="auth-role-label">First Login</div>

            <c:if test="${not empty success}">
                <div class="alert alert-success">${success}</div>
            </c:if>
            <c:if test="${not empty error}">
                <div class="alert alert-danger">${error}</div>
            </c:if>

            <%-- Temporary password reminder --%>
            <div class="temp-pw-box">
                <div class="small fw-bold mb-1" style="color:#7c4a00;">
                    <i class="bi bi-info-circle me-1"></i>Your temporary password
                </div>
                <div class="temp-pw-value" id="tempPwDisplay">
                    <%-- Shown only if passed via flash; otherwise show a placeholder --%>
                    <c:choose>
                        <c:when test="${not empty temporaryPassword}">${temporaryPassword}</c:when>
                        <c:otherwise><span style="opacity:0.5;font-size:0.85rem;">Not available — check your registration email or contact the library.</span></c:otherwise>
                    </c:choose>
                </div>
                <div class="small mt-2" style="color:#7c4a00;opacity:0.8;">
                    Student ID: <strong>${student.studentId}</strong> &nbsp;|&nbsp;
                    You must set a new password different from the temporary one.
                </div>
            </div>

            <form id="forcePasswordForm" method="post" action="${pageContext.request.contextPath}/student/password/change-temporary" novalidate>
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">

                <div class="mb-3">
                    <label class="form-label" for="newPassword">New password</label>
                    <div class="auth-input-shell">
                        <span class="auth-input-icon"><i class="bi bi-lock-fill"></i></span>
                        <input class="form-control" id="newPassword" name="newPassword"
                               type="password" maxlength="100"
                               autocomplete="new-password" required>
                        <button class="auth-password-toggle" id="toggleNewPassword" type="button" aria-label="Show password">
                            <i class="bi bi-eye-fill"></i>
                        </button>
                    </div>
                    <div class="pw-strength-bar"><div class="pw-strength-fill" id="pwStrengthFill"></div></div>
                    <div class="pw-strength-label" id="pwStrengthLabel" style="color:var(--muted);">Enter a password</div>
                    <ul class="pw-req-list" id="pwReqList">
                        <li id="req-length"><i class="bi bi-x-circle-fill"></i> At least 12 characters</li>
                        <li id="req-upper"><i class="bi bi-x-circle-fill"></i> Uppercase letter</li>
                        <li id="req-lower"><i class="bi bi-x-circle-fill"></i> Lowercase letter</li>
                        <li id="req-number"><i class="bi bi-x-circle-fill"></i> Number</li>
                        <li id="req-special"><i class="bi bi-x-circle-fill"></i> Special character</li>
                        <li id="req-nottemp"><i class="bi bi-x-circle-fill"></i> Different from temp password</li>
                    </ul>
                    <p class="field-error" id="newPasswordError" style="min-height:20px;margin-top:6px;color:#9d2f2a;font-size:0.84rem;"></p>
                </div>

                <div class="mb-4">
                    <label class="form-label" for="confirmPassword">Confirm new password</label>
                    <div class="auth-input-shell">
                        <span class="auth-input-icon"><i class="bi bi-lock-fill"></i></span>
                        <input class="form-control" id="confirmPassword" name="confirmPassword"
                               type="password" maxlength="100"
                               autocomplete="new-password" required>
                        <button class="auth-password-toggle" id="toggleConfirmPassword" type="button" aria-label="Show confirm password">
                            <i class="bi bi-eye-fill"></i>
                        </button>
                    </div>
                    <p class="field-error" id="confirmPasswordError" style="min-height:20px;margin-top:6px;color:#9d2f2a;font-size:0.84rem;"></p>
                </div>

                <button class="btn btn-brand auth-primary-btn w-100" type="submit" id="submitBtn">
                    <i class="bi bi-check2-circle me-2"></i>Set password and continue
                </button>
            </form>
        </section>
    </div>
</div>

<script>
    (function () {
        var newPassword     = document.getElementById("newPassword");
        var confirmPassword = document.getElementById("confirmPassword");
        var newPasswordError     = document.getElementById("newPasswordError");
        var confirmPasswordError = document.getElementById("confirmPasswordError");
        var strengthFill  = document.getElementById("pwStrengthFill");
        var strengthLabel = document.getElementById("pwStrengthLabel");
        var submitBtn     = document.getElementById("submitBtn");

        // ── Toggle visibility ─────────────────────────────────────────────
        function wireToggle(inputId, btnId) {
            var input = document.getElementById(inputId);
            var btn   = document.getElementById(btnId);
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

        // ── Temp password (for "different from temp" check) ───────────────
        var tempPwDisplay = document.getElementById("tempPwDisplay");
        var tempPwRaw = tempPwDisplay ? (tempPwDisplay.textContent || "").trim() : "";

        // ── Requirement checks ────────────────────────────────────────────
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
            return score; // 0–5
        }

        function updateStrengthBar(pw) {
            var score = scorePassword(pw);
            var pct   = pw ? (score / 5) * 100 : 0;
            var color, label;
            if (!pw)        { color = "#e2e8f0"; label = "Enter a password"; }
            else if (score <= 2) { color = "#e53e3e"; label = "Weak"; }
            else if (score === 3) { color = "#dd6b20"; label = "Fair"; }
            else if (score === 4) { color = "#d69e2e"; label = "Good"; }
            else                  { color = "#38a169"; label = "Strong"; }

            strengthFill.style.width   = pct + "%";
            strengthFill.style.background = color;
            strengthLabel.textContent  = label;
            strengthLabel.style.color  = color;
        }

        function validateNewPassword() {
            var pw = newPassword.value;
            var notTemp = !tempPwRaw || pw !== tempPwRaw;

            checkReq("req-length",  pw.length >= 12);
            checkReq("req-upper",   /[A-Z]/.test(pw));
            checkReq("req-lower",   /[a-z]/.test(pw));
            checkReq("req-number",  /\d/.test(pw));
            checkReq("req-special", /[^A-Za-z\d\s]/.test(pw));
            checkReq("req-nottemp", pw.length > 0 && notTemp);
            updateStrengthBar(pw);

            if (!pw) {
                newPasswordError.textContent = "New password is required.";
                return false;
            }
            if (!PASSWORD_PATTERN.test(pw)) {
                newPasswordError.textContent = "Password must be at least 12 characters with uppercase, lowercase, number, and special character.";
                return false;
            }
            if (!notTemp) {
                newPasswordError.textContent = "Choose a different password from the temporary one.";
                return false;
            }
            newPasswordError.textContent = "";
            return true;
        }

        function validateConfirmPassword() {
            var pw  = newPassword.value;
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
            if (confirmPassword.value) validateConfirmPassword();
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
