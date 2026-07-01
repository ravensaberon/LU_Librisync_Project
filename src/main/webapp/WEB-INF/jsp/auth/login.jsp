<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Latte and Letters Login</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260430-auth-back-link">
</head>
<body>
<div class="auth-shell auth-shell-library">
    <div class="auth-card hero-card">
        <section class="auth-story auth-story-visual" aria-hidden="true"></section>

        <section class="auth-form-wrap auth-form-panel">
            <div class="auth-panel-heading">
                <h2 class="auth-panel-title">Library <span class="auth-panel-title-accent">Login</span></h2>
                <p class="auth-panel-copy">Access the Latte and Letters library module for the integrated Library, Attendance, and Cafe system.</p>
            </div>

            <div class="auth-role-label">College Student</div>

            <c:if test="${not empty param.error}">
                <div class="alert alert-danger">Invalid email or password.</div>
            </c:if>
            <c:if test="${not empty param.logout}">
                <div class="alert alert-success">You have been logged out.</div>
            </c:if>
            <c:if test="${not empty param.registered}">
                <div class="alert alert-success">Registration complete. Check your email for the temporary password, then sign in and update it on first login.</div>
            </c:if>

            <form method="post" action="${pageContext.request.contextPath}/login">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">

                <div class="mb-3">
                    <label class="form-label" for="email">Email address</label>
                    <div class="auth-input-shell">
                        <span class="auth-input-icon"><i class="bi bi-person-fill"></i></span>
                        <input class="form-control" id="email" name="email" type="email" placeholder="reader@latteandletters.local" value="${param.email}" required>
                    </div>
                </div>

                <div class="mb-4">
                    <label class="form-label" for="password">Password</label>
                    <div class="auth-input-shell">
                        <span class="auth-input-icon"><i class="bi bi-lock-fill"></i></span>
                        <input class="form-control" id="password" name="password" type="password" placeholder="Enter your password" required>
                        <button class="auth-password-toggle" id="togglePassword" type="button" aria-label="Show password">
                            <i class="bi bi-eye-fill"></i>
                        </button>
                    </div>
                </div>

                <button class="btn btn-brand auth-primary-btn w-100" type="submit">Login</button>
            </form>

            <c:if test="${not empty param.resetSuccess}">
                <div class="alert alert-success mt-3 mb-0">Password reset complete. You can now sign in with your new password.</div>
            </c:if>

            <div class="auth-support-row">
                <a class="btn auth-support-button" href="${pageContext.request.contextPath}/forgot-password">Recover my account</a>
                <a class="auth-support-link" href="${pageContext.request.contextPath}/register">Create account</a>
            </div>
        </section>
    </div>
</div>
<script>
    (function () {
        var passwordInput = document.getElementById("password");
        var toggleButton = document.getElementById("togglePassword");

        if (!passwordInput || !toggleButton) {
            return;
        }

        toggleButton.addEventListener("click", function () {
            var showingPassword = passwordInput.type === "text";
            passwordInput.type = showingPassword ? "password" : "text";
            toggleButton.innerHTML = showingPassword
                ? '<i class="bi bi-eye-fill"></i>'
                : '<i class="bi bi-eye-slash-fill"></i>';
            toggleButton.setAttribute("aria-label", showingPassword ? "Show password" : "Hide password");
        });
    })();
</script>
</body>
</html>
