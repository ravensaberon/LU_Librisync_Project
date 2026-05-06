<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Student Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260504-student-tabs-switch">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Student Portal</span>
            <div class="brand-title mt-2">Welcome, ${student.user.name}</div>
        </div>
        <div class="nav-links">
            <a class="nav-pill active" href="${pageContext.request.contextPath}/student/dashboard">Dashboard</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/catalog">Catalog</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/reservations">Pickup requests</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/profile">Profile</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/history">Borrowed books</a>
            <form method="post" action="${pageContext.request.contextPath}/logout">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <button class="nav-pill warm border-0" type="submit" aria-label="Logout" title="Logout"><span class="nav-pill-icon"><i class="bi bi-power" aria-hidden="true"></i></span><span class="nav-pill-label">Logout</span></button>
            </form>
        </div>
    </div>

    <section class="dashboard-tab-shell mb-4" data-student-dashboard-tabs>
        <div class="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
                <div class="section-title mb-2">Student workspace tabs</div>
            </div>
        </div>

        <div class="dashboard-tab-nav" role="tablist" aria-label="Student dashboard views">
            <button class="dashboard-tab-button" type="button" data-student-dashboard-reset-button>
                <i class="bi bi-door-open-fill"></i>
                <span>Library Access</span>
            </button>
            <button class="dashboard-tab-button" type="button" role="tab" id="student-standing-tab" aria-selected="false" aria-controls="student-standing-panel" data-student-dashboard-tab-button data-student-dashboard-tab-target="student-standing-panel">
                <i class="bi bi-person-check-fill"></i>
                <span>Standing</span>
            </button>
            <button class="dashboard-tab-button" type="button" role="tab" id="student-popular-tab" aria-selected="false" aria-controls="student-popular-panel" data-student-dashboard-tab-button data-student-dashboard-tab-target="student-popular-panel">
                <i class="bi bi-stars"></i>
                <span>Popular</span>
            </button>
            <button class="dashboard-tab-button" type="button" role="tab" id="student-fines-tab" aria-selected="false" aria-controls="student-fines-panel" data-student-dashboard-tab-button data-student-dashboard-tab-target="student-fines-panel">
                <i class="bi bi-receipt-cutoff"></i>
                <span>Fines</span>
            </button>
            <button class="dashboard-tab-button" type="button" role="tab" id="student-schedule-tab" aria-selected="false" aria-controls="student-schedule-panel" data-student-dashboard-tab-button data-student-dashboard-tab-target="student-schedule-panel">
                <i class="bi bi-calendar-week-fill"></i>
                <span>Schedule</span>
            </button>
        </div>
    </section>

    <div class="dashboard-default-view" data-student-dashboard-default-view>
        <section class="hero-card mb-4">
            <div class="row g-4 align-items-center">
                <div class="col-md-8">
                    <h1 class="fw-bold mb-2">Library access at a glance</h1>
                    <p class="muted-text mb-0">Student ID: <strong>${student.studentId}</strong> | Course: <strong>${student.course}</strong> | Year level: <strong>${student.yearLevel}</strong></p>
                </div>
                <div class="col-md-4 text-md-end">
                    <span class="tag-chip">Digital-ready account</span>
                </div>
            </div>
        </section>

        <section class="stat-grid student-dashboard-stat-grid mb-4">
            <div class="metric-card">
                <div class="metric-value">${activeCount}</div>
                <div class="metric-label">Currently borrowed</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">${overdueCount}</div>
                <div class="metric-label">Overdue items</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">${historyCount}</div>
                <div class="metric-label">Total issue history</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">${reservationCount}</div>
                <div class="metric-label">Active reservations</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">${outstandingFineTotal}</div>
                <div class="metric-label">Outstanding fines</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">${borrowerStanding.remainingLoanSlots}</div>
                <div class="metric-label">Remaining loan slots</div>
            </div>
        </section>
    </div>

    <section class="dashboard-tab-panels mb-4" data-student-dashboard-panel-shell hidden>
        <div class="dashboard-tab-panel" id="student-standing-panel" role="tabpanel" aria-labelledby="student-standing-tab" data-student-dashboard-tab-panel hidden>
            <div class="panel-card">
                <div class="section-title">Borrowing standing</div>
                <div class="support-item">
                    <strong>${borrowerStanding.statusLabel}</strong>
                    <span>
                        Active loans: ${borrowerStanding.activeLoansCount}/${borrowerStanding.maxActiveLoans}
                        | Overdue items: ${borrowerStanding.overdueCount}
                        | Outstanding fines: ${borrowerStanding.outstandingFineAmount}
                    </span>
                </div>
                <c:if test="${borrowerStanding.blocked}">
                    <div class="support-list mt-3">
                        <c:forEach items="${borrowerStanding.blockers}" var="blocker">
                            <div class="support-item">
                                <strong>Action needed</strong>
                                <span>${blocker}</span>
                            </div>
                        </c:forEach>
                    </div>
                </c:if>
            </div>
        </div>

        <div class="dashboard-tab-panel" id="student-popular-panel" role="tabpanel" aria-labelledby="student-popular-tab" data-student-dashboard-tab-panel hidden>
            <div class="panel-card">
                <div class="section-title">Popular books</div>
                <ul class="list-clean">
                    <c:forEach items="${popularBooks}" var="bookStat">
                        <li class="popular-book-item">
                            <div>
                                <strong>${bookStat.title}</strong>
                                <div class="muted-text">Most borrowed in the current catalog</div>
                            </div>
                            <span class="tag-chip">${bookStat.borrowCount} borrow(s)</span>
                        </li>
                    </c:forEach>
                    <c:if test="${empty popularBooks}">
                        <li class="muted-text">Popular titles will appear here once circulation activity builds up.</li>
                    </c:if>
                </ul>
            </div>
        </div>

        <div class="dashboard-tab-panel" id="student-fines-panel" role="tabpanel" aria-labelledby="student-fines-tab" data-student-dashboard-tab-panel hidden>
            <div class="panel-card">
                <div class="section-title">Recent fine activity</div>
                <ul class="list-clean">
                    <c:forEach items="${studentFines}" var="fine" end="4">
                        <li class="popular-book-item">
                            <div>
                                <strong>${fine.issueRecord.book.title}</strong>
                                <div class="muted-text">${fine.status}</div>
                            </div>
                            <span class="tag-chip">${fine.amount}</span>
                        </li>
                    </c:forEach>
                    <c:if test="${empty studentFines}">
                        <li class="muted-text">No fines are currently recorded for your account.</li>
                    </c:if>
                </ul>
            </div>
        </div>

        <div class="dashboard-tab-panel" id="student-schedule-panel" role="tabpanel" aria-labelledby="student-schedule-tab" data-student-dashboard-tab-panel hidden>
            <div class="panel-card">
                <div class="section-title">Issued books and return schedule</div>
                <div class="table-responsive">
                    <table class="table align-middle">
                        <thead>
                        <tr>
                            <th>Book</th>
                            <th>Issue date</th>
                            <th>Due date</th>
                            <th>Status</th>
                            <th>Fine</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${issueRecords}" var="issue">
                            <tr>
                                <td>${issue.book.title}</td>
                                <td>${issue.issueDateDisplay}</td>
                                <td>${issue.dueDateDisplay}</td>
                                <td><span class="tag-chip">${issue.status}</span></td>
                                <td>${issue.fineAmount}</td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty issueRecords}">
                            <tr>
                                <td colspan="5" class="text-center muted-text">No borrowed books yet.</td>
                            </tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </section>
</div>
<script>
    (function () {
        var tabRoot = document.querySelector("[data-student-dashboard-tabs]");
        if (!tabRoot) {
            return;
        }

        var buttons = document.querySelectorAll("[data-student-dashboard-tab-button]");
        var panels = document.querySelectorAll("[data-student-dashboard-tab-panel]");
        var defaultView = document.querySelector("[data-student-dashboard-default-view]");
        var panelShell = document.querySelector("[data-student-dashboard-panel-shell]");
        var resetButtons = document.querySelectorAll("[data-student-dashboard-reset-button]");

        function resetView() {
            Array.prototype.forEach.call(buttons, function (button) {
                button.classList.remove("is-active");
                button.setAttribute("aria-selected", "false");
                button.setAttribute("tabindex", "0");
            });

            Array.prototype.forEach.call(resetButtons, function (button) {
                button.classList.add("is-active");
            });

            Array.prototype.forEach.call(panels, function (panel) {
                panel.hidden = true;
                panel.classList.remove("is-active");
            });

            if (defaultView) {
                defaultView.hidden = false;
            }

            if (panelShell) {
                panelShell.hidden = true;
            }
        }

        function activateTab(targetId) {
            Array.prototype.forEach.call(resetButtons, function (button) {
                button.classList.remove("is-active");
            });

            if (defaultView) {
                defaultView.hidden = true;
            }

            if (panelShell) {
                panelShell.hidden = false;
            }

            Array.prototype.forEach.call(buttons, function (button) {
                var isActive = button.getAttribute("data-student-dashboard-tab-target") === targetId;
                button.classList.toggle("is-active", isActive);
                button.setAttribute("aria-selected", isActive ? "true" : "false");
                button.setAttribute("tabindex", isActive ? "0" : "-1");
            });

            Array.prototype.forEach.call(panels, function (panel) {
                var isActive = panel.id === targetId;
                panel.hidden = !isActive;
                panel.classList.toggle("is-active", isActive);
            });
        }

        Array.prototype.forEach.call(buttons, function (button) {
            button.addEventListener("click", function () {
                activateTab(button.getAttribute("data-student-dashboard-tab-target"));
            });
        });

        Array.prototype.forEach.call(resetButtons, function (button) {
            button.addEventListener("click", function () {
                resetView();
            });
        });

        resetView();
    })();
</script>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>


