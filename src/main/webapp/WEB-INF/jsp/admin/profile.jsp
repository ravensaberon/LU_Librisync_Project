<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Admin Profile</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260504-global-side-nav-flush3">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Admin Profile</span>
            <div class="brand-title mt-2">Account and security settings</div>
        </div>
        <div class="nav-links">
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/dashboard">Dashboard</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/books">Books</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/issues">Issue / Return</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/reservations">Reservations</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/students">Students</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/fines">Fines</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/reports">Reports</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/references">Categories / Authors</a>
            <a class="nav-pill active" href="${pageContext.request.contextPath}/admin/profile">Profile</a>
            <form method="post" action="${pageContext.request.contextPath}/logout">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <button class="nav-pill warm border-0" type="submit" aria-label="Logout" title="Logout"><span class="nav-pill-icon"><i class="bi bi-power" aria-hidden="true"></i></span><span class="nav-pill-label">Logout</span></button>
            </form>
        </div>
    </div>

    <c:if test="${not empty success}">
        <div class="alert alert-success">${success}</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-danger">${error}</div>
    </c:if>

    <section class="hero-card mb-4">
        <div class="row g-4 align-items-center">
            <div class="col-md-8">
                <h1 class="fw-bold mb-2">${adminUser.name}</h1>
                <p class="muted-text mb-0">Manage your admin identity, keep your display information current, and protect access with a stronger password.</p>
            </div>
            <div class="col-md-4 text-md-end">
                <span class="tag-chip">Role: ${adminUser.role}</span>
            </div>
        </div>
    </section>

    <section class="info-grid mb-4">
        <div class="info-tile">
            <span class="info-tile-label">Email</span>
            <span class="info-tile-value">${adminUser.email}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Status</span>
            <span class="info-tile-value">${adminUser.status}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Transactions Managed</span>
            <span class="info-tile-value">${transactionsManaged}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Active Circulation</span>
            <span class="info-tile-value">${activeCirculation}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Registered Students</span>
            <span class="info-tile-value">${studentCount}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Created</span>
            <span class="info-tile-value info-tile-value-stack">
                <span>${adminUser.createdAtDateDisplay}</span>
                <span>${adminUser.createdAtTimeDisplay}</span>
            </span>
        </div>
    </section>

    <section class="profile-grid">
        <div class="panel-card">
            <div class="section-title">Update profile</div>
            <p class="helper-copy mb-4">Use a clear display name so circulation records and issued transactions remain easy to track.</p>
            <form method="post" action="${pageContext.request.contextPath}/admin/profile">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <div class="mb-3">
                    <label class="form-label" for="name">Display name</label>
                    <input class="form-control" id="name" name="name" value="${adminUser.name}" required>
                </div>
                <div class="mb-4">
                    <label class="form-label" for="email">Email address</label>
                    <input class="form-control" id="email" value="${adminUser.email}" disabled>
                </div>
                <button class="btn btn-brand" type="submit">Save profile</button>
            </form>
        </div>

        <div class="panel-card" id="password-security">
            <div class="section-title">Change password</div>
            <p class="helper-copy mb-4">This secures your admin access and protects book circulation, student records, and catalog updates.</p>
            <form method="post" action="${pageContext.request.contextPath}/admin/profile/password">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <div class="mb-3">
                    <label class="form-label" for="currentPassword">Current password</label>
                    <input class="form-control" id="currentPassword" name="currentPassword" type="password" required>
                </div>
                <div class="mb-3">
                    <label class="form-label" for="newPassword">New password</label>
                    <input class="form-control" id="newPassword" name="newPassword" type="password" required>
                </div>
                <div class="mb-4">
                    <label class="form-label" for="confirmPassword">Confirm new password</label>
                    <input class="form-control" id="confirmPassword" name="confirmPassword" type="password" required>
                </div>
                <button class="btn btn-warm" type="submit">Update password</button>
            </form>
        </div>
    </section>
</div>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>

