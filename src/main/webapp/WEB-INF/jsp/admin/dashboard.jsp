<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Admin Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260504-global-side-nav-flush3">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.3/dist/chart.umd.min.js"></script>
    <style>
        @media (max-width: 768px) {
            .app-nav {
                top: 56px !important;
                left: 0 !important;
                bottom: 0 !important;
                width: min(282px, calc(100vw - 54px)) !important;
                padding: 0 12px 12px !important;
                border: 0 !important;
                border-radius: 0 !important;
                background: rgba(255, 255, 255, 0.99) !important;
                box-shadow: 14px 0 32px rgba(15, 127, 52, 0.10) !important;
            }

            .app-nav > div:first-child {
                padding: 18px 10px 16px !important;
                margin: 0 0 10px !important;
                background: transparent !important;
                border-bottom: 1px solid rgba(15, 127, 52, 0.10) !important;
            }

            .app-nav > div:first-child .tag-chip {
                padding: 0 !important;
                border-radius: 0 !important;
                background: transparent !important;
                color: #7b8480 !important;
                font-size: 0.82rem !important;
                letter-spacing: 0.04em !important;
            }

            .app-nav > div:first-child .brand-title {
                margin-top: 6px !important;
                color: #1f2c22 !important;
                font-size: 1.15rem !important;
                line-height: 1.35 !important;
            }

            .nav-links {
                gap: 8px !important;
            }

            .nav-pill,
            .nav-links form,
            .nav-links form .nav-pill {
                width: 100% !important;
            }

            .nav-pill {
                min-height: 46px !important;
                padding: 11px 14px !important;
                border-radius: 12px !important;
                color: #6f7c75 !important;
            }

            .nav-pill.active {
                border-radius: 16px !important;
                box-shadow: none !important;
            }

            .nav-pill.warm {
                margin-top: auto !important;
                border-radius: 14px !important;
            }
        }
    </style>
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Admin Console</span>
            <div class="brand-title mt-2">Latte and Letters Dashboard</div>
        </div>
        <div class="nav-links">
            <a class="nav-pill active" href="${pageContext.request.contextPath}/admin/dashboard">Dashboard</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/books">Books</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/issues">Issue / Return</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/reservations">Reservations</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/students">Students</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/fines">Fines</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/reports">Reports</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/references">Categories / Authors</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/profile">Profile</a>
            <form method="post" action="${pageContext.request.contextPath}/logout">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <button class="nav-pill warm border-0" type="submit" aria-label="Logout" title="Logout"><span class="nav-pill-icon"><i class="bi bi-power" aria-hidden="true"></i></span><span class="nav-pill-label">Logout</span></button>
            </form>
        </div>
    </div>

    <section class="dashboard-tab-shell mb-4" data-dashboard-tabs>
        <div class="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
                <div class="section-title mb-2">Admin workspace tabs</div>
            </div>
        </div>

        <div class="dashboard-tab-nav" role="tablist" aria-label="Admin dashboard views">
            <button class="dashboard-tab-button" type="button" data-dashboard-reset-button>
                <i class="bi bi-house-door-fill"></i>
                <span>Library Overview</span>
            </button>
            <button class="dashboard-tab-button" type="button" role="tab" id="admin-ops-tab" aria-selected="false" aria-controls="admin-ops-panel" data-dashboard-tab-button data-dashboard-tab-target="admin-ops-panel">
                <i class="bi bi-grid-1x2-fill"></i>
                <span>Operations</span>
            </button>
            <button class="dashboard-tab-button" type="button" role="tab" id="admin-circulation-tab" aria-selected="false" aria-controls="admin-circulation-panel" data-dashboard-tab-button data-dashboard-tab-target="admin-circulation-panel">
                <i class="bi bi-bar-chart-line-fill"></i>
                <span>Circulation</span>
            </button>
            <button class="dashboard-tab-button" type="button" role="tab" id="admin-borrowing-tab" aria-selected="false" aria-controls="admin-borrowing-panel" data-dashboard-tab-button data-dashboard-tab-target="admin-borrowing-panel">
                <i class="bi bi-journal-check"></i>
                <span>Borrowing</span>
            </button>
            <button class="dashboard-tab-button" type="button" role="tab" id="admin-fines-tab" aria-selected="false" aria-controls="admin-fines-panel" data-dashboard-tab-button data-dashboard-tab-target="admin-fines-panel">
                <i class="bi bi-receipt-cutoff"></i>
                <span>Fines</span>
            </button>
        </div>
    </section>

    <div class="dashboard-default-view" data-dashboard-default-view>
        <section class="hero-card mb-4">
            <div class="hero-card-grid">
                <div>
                    <span class="tag-chip">Library Health</span>
                    <h1 class="fw-bold mt-3 mb-2">Library overview</h1>
                    <p class="muted-text mb-0">Monitor circulation, student demand, and account risk from one clearer dashboard.</p>
                </div>
                <div class="hero-side-note">
                    <div class="hero-side-title">Needs attention</div>
                    <strong class="hero-side-value">${overdueCount + blockedBorrowerCount}</strong>
                    <span class="hero-side-caption">${overdueCount} overdue case(s) and ${blockedBorrowerCount} blocked borrower(s) currently need follow-up.</span>
                </div>
            </div>
        </section>

        <section class="dashboard-kpi-grid mb-4">
            <article class="panel-card dashboard-kpi-card dashboard-kpi-card-feature">
                <span class="dashboard-kpi-label">Active circulation</span>
                <strong class="dashboard-kpi-value">${issuedCount}</strong>
                <p class="dashboard-kpi-copy">Books currently issued to borrowers.</p>
            </article>
            <article class="panel-card dashboard-kpi-card">
                <span class="dashboard-kpi-label">Overdue cases</span>
                <strong class="dashboard-kpi-value">${overdueCount}</strong>
                <p class="dashboard-kpi-copy">${overdueRate}% of active desk records are overdue.</p>
            </article>
            <article class="panel-card dashboard-kpi-card">
                <span class="dashboard-kpi-label">Pending reservations</span>
                <strong class="dashboard-kpi-value">${pendingReservationCount}</strong>
                <p class="dashboard-kpi-copy">${readyReservationCount} reservation(s) already ready for claim.</p>
            </article>
            <article class="panel-card dashboard-kpi-card">
                <span class="dashboard-kpi-label">Unpaid balance</span>
                <strong class="dashboard-kpi-value">${outstandingFineTotal}</strong>
                <p class="dashboard-kpi-copy">${outstandingFineCount} open fine record(s) still unpaid.</p>
            </article>
        </section>

        <section class="panel-card mb-4">
            <div class="dashboard-summary-grid">
                <div class="dashboard-summary-item">
                    <span class="dashboard-summary-label">Catalog</span>
                    <strong class="dashboard-summary-value">${bookCount}</strong>
                    <span class="dashboard-summary-copy">${availableCount} available for issue</span>
                </div>
                <div class="dashboard-summary-item">
                    <span class="dashboard-summary-label">Borrowers</span>
                    <strong class="dashboard-summary-value">${studentCount}</strong>
                    <span class="dashboard-summary-copy">${blockedBorrowerCount} blocked account(s)</span>
                </div>
                <div class="dashboard-summary-item">
                    <span class="dashboard-summary-label">Reservations</span>
                    <strong class="dashboard-summary-value">${pendingReservationCount + readyReservationCount}</strong>
                    <span class="dashboard-summary-copy">${readyReservationCount} ready for claim</span>
                </div>
                <div class="dashboard-summary-item">
                    <span class="dashboard-summary-label">Risk level</span>
                    <strong class="dashboard-summary-value">${overdueRate}%</strong>
                    <span class="dashboard-summary-copy">Overdue rate across active circulation</span>
                </div>
            </div>
        </section>
    </div>

    <section class="dashboard-tab-panels mb-4" data-dashboard-panel-shell hidden>
        <div class="dashboard-tab-panel" id="admin-ops-panel" role="tabpanel" aria-labelledby="admin-ops-tab" data-dashboard-tab-panel hidden>
            <div class="panel-card">
                <div class="section-title mb-2">Admin operations center</div>
                <p class="helper-copy mb-4">Jump straight into the main staff workspaces without digging through long descriptions.</p>

                <div class="dashboard-action-grid">
                    <a class="dashboard-action-card" href="${pageContext.request.contextPath}/admin/books">
                        <span class="dashboard-action-icon"><i class="bi bi-journals"></i></span>
                        <div class="dashboard-action-copy">
                            <h3>Catalog and inventory</h3>
                            <p>Books, ISBN, copies, and shelf records.</p>
                        </div>
                        <span class="dashboard-action-meta">${bookCount} books</span>
                    </a>
                    <a class="dashboard-action-card" href="${pageContext.request.contextPath}/admin/issues">
                        <span class="dashboard-action-icon"><i class="bi bi-arrow-left-right"></i></span>
                        <div class="dashboard-action-copy">
                            <h3>Circulation desk</h3>
                            <p>Issue, return, and due date handling.</p>
                        </div>
                        <span class="dashboard-action-meta">${issuedCount} active</span>
                    </a>
                    <a class="dashboard-action-card" href="${pageContext.request.contextPath}/admin/students">
                        <span class="dashboard-action-icon"><i class="bi bi-people"></i></span>
                        <div class="dashboard-action-copy">
                            <h3>Borrower accounts</h3>
                            <p>Student records, status, and password resets.</p>
                        </div>
                        <span class="dashboard-action-meta">${studentCount} students</span>
                    </a>
                    <a class="dashboard-action-card" href="${pageContext.request.contextPath}/admin/issues#borrow-requests">
                        <span class="dashboard-action-icon"><i class="bi bi-bookmark-check"></i></span>
                        <div class="dashboard-action-copy">
                            <h3>Reservation queue</h3>
                            <p>Pending holds and release-ready pickups.</p>
                        </div>
                        <span class="dashboard-action-meta">${pendingReservationCount} pending</span>
                    </a>
                    <a class="dashboard-action-card" href="${pageContext.request.contextPath}/admin/fines">
                        <span class="dashboard-action-icon"><i class="bi bi-receipt"></i></span>
                        <div class="dashboard-action-copy">
                            <h3>Fine ledger</h3>
                            <p>Payments, waivers, and open balances.</p>
                        </div>
                        <span class="dashboard-action-meta">${outstandingFineTotal} unpaid</span>
                    </a>
                    <a class="dashboard-action-card" href="${pageContext.request.contextPath}/admin/reports">
                        <span class="dashboard-action-icon"><i class="bi bi-bar-chart"></i></span>
                        <div class="dashboard-action-copy">
                            <h3>Reports and exports</h3>
                            <p>Analytics, summaries, and CSV output.</p>
                        </div>
                        <span class="dashboard-action-meta">Open reports</span>
                    </a>
                    <a class="dashboard-action-card" href="${pageContext.request.contextPath}/admin/references">
                        <span class="dashboard-action-icon"><i class="bi bi-tags"></i></span>
                        <div class="dashboard-action-copy">
                            <h3>Categories and authors</h3>
                            <p>Reference data for cleaner catalog search.</p>
                        </div>
                        <span class="dashboard-action-meta">Maintain data</span>
                    </a>
                    <a class="dashboard-action-card" href="${pageContext.request.contextPath}/admin/profile">
                        <span class="dashboard-action-icon"><i class="bi bi-shield-lock"></i></span>
                        <div class="dashboard-action-copy">
                            <h3>Admin security</h3>
                            <p>Profile settings and password protection.</p>
                        </div>
                        <span class="dashboard-action-meta">Account settings</span>
                    </a>
                </div>
            </div>
        </div>

        <div class="dashboard-tab-panel" id="admin-circulation-panel" role="tabpanel" aria-labelledby="admin-circulation-tab" data-dashboard-tab-panel hidden>
            <div class="dashboard-tab-content-grid">
                <div class="panel-card chart-card">
                    <div class="chart-header">
                        <div class="chart-copy">
                            <div class="section-title mb-2" id="circulationChartTitle">Interactive circulation graph</div>
                            <p id="circulationChartDescription">Switch between day, week, month, and year views to monitor borrowing activity at the level you need.</p>
                        </div>
                        <div class="chart-toolbar">
                            <div class="chart-range-switcher" role="tablist" aria-label="Circulation chart range">
                                <c:forEach items="${circulationChartSeries}" var="series" varStatus="status">
                                    <button class="chart-range-button <c:if test='${status.first}'>is-active</c:if>"
                                            type="button"
                                            role="tab"
                                            aria-selected="${status.first ? 'true' : 'false'}"
                                            data-circulation-range-button
                                            data-circulation-range="${series.key}">
                                            ${series.label}
                                    </button>
                                </c:forEach>
                            </div>
                            <div class="chart-legend">
                                <span class="legend-pill"><span class="legend-dot issued"></span>Issued</span>
                                <span class="legend-pill"><span class="legend-dot returned"></span>Returned</span>
                            </div>
                        </div>
                    </div>
                    <div class="chart-layout">
                        <div class="chart-canvas-shell">
                            <canvas id="circulationChart" aria-label="Interactive circulation chart"></canvas>
                        </div>
                        <div class="chart-summary-grid">
                            <div class="chart-summary-card">
                                <span class="chart-summary-label">Issued in view</span>
                                <strong class="chart-summary-value" id="circulationIssuedTotal">0</strong>
                                <span class="chart-summary-note" id="circulationIssuedNote">Books issued in the selected range.</span>
                            </div>
                            <div class="chart-summary-card">
                                <span class="chart-summary-label">Returned in view</span>
                                <strong class="chart-summary-value" id="circulationReturnedTotal">0</strong>
                                <span class="chart-summary-note" id="circulationReturnedNote">Books returned in the selected range.</span>
                            </div>
                            <div class="chart-summary-card">
                                <span class="chart-summary-label" id="circulationPeakLabel">Peak issued in a day</span>
                                <strong class="chart-summary-value" id="circulationPeakValue">0</strong>
                                <span class="chart-summary-note" id="circulationPeakNote">Highest borrowing spike inside this view.</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="dashboard-insight-grid">
                    <div class="panel-card dashboard-insight-card">
                        <div class="section-title">Most borrowed books</div>
                        <ul class="list-clean dashboard-ranked-list">
                            <c:forEach items="${mostBorrowedBooks}" var="item">
                                <li class="d-flex justify-content-between align-items-center">
                                    <span>${item.title}</span>
                                    <span class="tag-chip warn">${item.borrowCount} borrow(s)</span>
                                </li>
                            </c:forEach>
                            <c:if test="${empty mostBorrowedBooks}">
                                <li class="muted-text">Analytics will appear after circulation data grows.</li>
                            </c:if>
                        </ul>

                        <div class="section-title dashboard-mini-heading">Admin focus areas</div>
                        <div class="support-list dashboard-mini-focus">
                            <div class="support-item">
                                <strong>Interactive circulation monitoring</strong>
                                <span>Switch between daily, weekly, monthly, and yearly views to spot spikes, slow returns, and longer-term borrowing patterns quickly.</span>
                            </div>
                        </div>
                    </div>

                    <div class="panel-card dashboard-insight-card">
                        <div class="section-title">Recent audit trail</div>
                        <div class="audit-timeline dashboard-audit-timeline">
                            <c:forEach items="${recentAuditLogsPage.items}" var="log">
                                <div class="audit-item">
                                    <div class="audit-item-badge"><i class="bi bi-shield-check"></i></div>
                                    <div>
                                        <div class="audit-item-heading">${log.summary}</div>
                                        <div class="audit-item-meta">${log.action} | ${empty log.actorName ? 'System' : log.actorName} | ${log.createdAtDisplay}</div>
                                        <c:if test="${not empty log.details}">
                                            <div class="audit-item-copy">${log.details}</div>
                                        </c:if>
                                    </div>
                                </div>
                            </c:forEach>
                            <c:if test="${empty recentAuditLogsPage.items}">
                                <div class="muted-text">Audit trail data will appear after admin and system actions are recorded.</div>
                            </c:if>
                        </div>
                        <c:if test="${recentAuditLogsPage.totalPages > 1}">
                            <nav class="mt-4" aria-label="Recent audit trail pages">
                                <ul class="pagination justify-content-center mb-0">
                                    <li class="page-item <c:if test='${!recentAuditLogsPage.hasPrevious}'>disabled</c:if>">
                                        <a class="page-link" href="${pageContext.request.contextPath}/admin/dashboard?auditPage=${recentAuditLogsPage.previousPage}#admin-circulation-panel">Previous</a>
                                    </li>
                                    <c:forEach begin="${recentAuditLogsPage.startPage}" end="${recentAuditLogsPage.endPage}" var="pageNumber">
                                        <li class="page-item <c:if test='${pageNumber == recentAuditLogsPage.page}'>active</c:if>">
                                            <a class="page-link" href="${pageContext.request.contextPath}/admin/dashboard?auditPage=${pageNumber}#admin-circulation-panel">${pageNumber}</a>
                                        </li>
                                    </c:forEach>
                                    <li class="page-item <c:if test='${!recentAuditLogsPage.hasNext}'>disabled</c:if>">
                                        <a class="page-link" href="${pageContext.request.contextPath}/admin/dashboard?auditPage=${recentAuditLogsPage.nextPage}#admin-circulation-panel">Next</a>
                                    </li>
                                </ul>
                            </nav>
                        </c:if>
                    </div>
                </div>
            </div>
        </div>

        <div class="dashboard-tab-panel" id="admin-borrowing-panel" role="tabpanel" aria-labelledby="admin-borrowing-tab" data-dashboard-tab-panel hidden>
            <div class="panel-card">
                <div class="section-title">Recent borrowing activity</div>
                <div class="table-responsive">
                    <table class="table align-middle">
                        <thead>
                        <tr>
                            <th>Book</th>
                            <th>Student</th>
                            <th>Due date</th>
                            <th>Status</th>
                            <th>Fine</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${recentIssues}" var="issue">
                            <tr>
                                <td>${issue.book.title}</td>
                                <td>${issue.student.user.name}</td>
                                <td>${issue.dueDateDisplay}</td>
                                <td><span class="tag-chip">${issue.status}</span></td>
                                <td>${issue.fineAmount}</td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty recentIssues}">
                            <tr>
                                <td colspan="5" class="text-center muted-text">No issue records yet.</td>
                            </tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div class="dashboard-tab-panel" id="admin-fines-panel" role="tabpanel" aria-labelledby="admin-fines-tab" data-dashboard-tab-panel hidden>
            <div class="panel-card">
                <div class="section-title">Recent outstanding fines</div>
                <div class="table-responsive">
                    <table class="table align-middle">
                        <thead>
                        <tr>
                            <th>Student</th>
                            <th>Book</th>
                            <th>Amount</th>
                            <th>Recorded</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${recentOutstandingFines}" var="fine">
                            <tr>
                                <td>${fine.student.studentId} - ${fine.student.user.name}</td>
                                <td>${fine.issueRecord.book.title}</td>
                                <td>${fine.amount}</td>
                                <td>${fine.calculatedAtDisplay}</td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty recentOutstandingFines}">
                            <tr>
                                <td colspan="4" class="text-center muted-text">No outstanding fines are recorded right now.</td>
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
        var tabRoot = document.querySelector("[data-dashboard-tabs]");
        if (!tabRoot) {
            return;
        }

        var buttons = document.querySelectorAll("[data-dashboard-tab-button]");
        var panels = document.querySelectorAll("[data-dashboard-tab-panel]");
        var defaultView = document.querySelector("[data-dashboard-default-view]");
        var panelShell = document.querySelector("[data-dashboard-panel-shell]");
        var resetButtons = document.querySelectorAll("[data-dashboard-reset-button]");

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
                var isActive = button.getAttribute("data-dashboard-tab-target") === targetId;
                button.classList.toggle("is-active", isActive);
                button.setAttribute("aria-selected", isActive ? "true" : "false");
                button.setAttribute("tabindex", isActive ? "0" : "-1");
            });

            Array.prototype.forEach.call(panels, function (panel) {
                var isActive = panel.id === targetId;
                panel.hidden = !isActive;
                panel.classList.toggle("is-active", isActive);
            });

            document.dispatchEvent(new CustomEvent("dashboard:tabchange", {
                detail: {
                    panelId: targetId
                }
            }));
        }

        Array.prototype.forEach.call(buttons, function (button) {
            button.addEventListener("click", function () {
                activateTab(button.getAttribute("data-dashboard-tab-target"));
            });
        });

        Array.prototype.forEach.call(resetButtons, function (button) {
            button.addEventListener("click", function () {
                resetView();
            });
        });

        var activeTabFromHash = window.location.hash ? window.location.hash.replace("#", "") : "";
        if (activeTabFromHash && document.getElementById(activeTabFromHash)) {
            activateTab(activeTabFromHash);
        } else {
            resetView();
        }
    })();
</script>
<script>
    (function () {
        var chartCanvas = document.getElementById("circulationChart");
        if (!chartCanvas || typeof Chart === "undefined") {
            return;
        }

        var chartInstance;
        var circulationPanel = document.getElementById("admin-circulation-panel");
        var rangeButtons = document.querySelectorAll("[data-circulation-range-button]");
        var titleNode = document.getElementById("circulationChartTitle");
        var descriptionNode = document.getElementById("circulationChartDescription");
        var issuedTotalNode = document.getElementById("circulationIssuedTotal");
        var returnedTotalNode = document.getElementById("circulationReturnedTotal");
        var peakLabelNode = document.getElementById("circulationPeakLabel");
        var peakValueNode = document.getElementById("circulationPeakValue");
        var peakNoteNode = document.getElementById("circulationPeakNote");
        var activeRange = "day";

        var chartSeries = {
            <c:forEach items="${circulationChartSeries}" var="series" varStatus="seriesStatus">
            "${series.key}": {
                label: "${series.label}",
                title: "${series.title}",
                description: "${series.description}",
                bucketLabel: "${series.bucketLabel}",
                issuedTotal: ${series.issuedTotal},
                returnedTotal: ${series.returnedTotal},
                peakIssued: ${series.peakIssued},
                peakReturned: ${series.peakReturned},
                points: [
                    <c:forEach items="${series.points}" var="point" varStatus="pointStatus">
                    {
                        label: "${point.label}",
                        issuedCount: ${point.issuedCount},
                        returnedCount: ${point.returnedCount}
                    }<c:if test="${!pointStatus.last}">,</c:if>
                    </c:forEach>
                ]
            }<c:if test="${!seriesStatus.last}">,</c:if>
            </c:forEach>
        };

        function bucketLabelWord(bucketLabel, amount) {
            if (amount === 1) {
                return bucketLabel;
            }
            if (bucketLabel === "day") {
                return "days";
            }
            if (bucketLabel === "week") {
                return "weeks";
            }
            if (bucketLabel === "month") {
                return "months";
            }
            return "years";
        }

        function updateSummary(series) {
            if (!series) {
                return;
            }

            if (titleNode) {
                titleNode.textContent = series.title;
            }

            if (descriptionNode) {
                descriptionNode.textContent = series.description;
            }

            if (issuedTotalNode) {
                issuedTotalNode.textContent = series.issuedTotal;
            }

            if (returnedTotalNode) {
                returnedTotalNode.textContent = series.returnedTotal;
            }

            if (peakLabelNode) {
                peakLabelNode.textContent = "Peak issued in a " + series.bucketLabel;
            }

            if (peakValueNode) {
                peakValueNode.textContent = series.peakIssued;
            }

            if (peakNoteNode) {
                peakNoteNode.textContent = "Highest borrowing spike across the selected " + bucketLabelWord(series.bucketLabel, 2) + ".";
            }
        }

        function syncRangeButtons(nextRange) {
            Array.prototype.forEach.call(rangeButtons, function (button) {
                var isActive = button.getAttribute("data-circulation-range") === nextRange;
                button.classList.toggle("is-active", isActive);
                button.setAttribute("aria-selected", isActive ? "true" : "false");
            });
        }

        function buildDataset(series) {
            return {
                labels: series.points.map(function (point) {
                    return point.label;
                }),
                datasets: [
                    {
                        type: "bar",
                        label: "Issued",
                        data: series.points.map(function (point) {
                            return point.issuedCount;
                        }),
                        backgroundColor: "rgba(15, 127, 52, 0.88)",
                        borderRadius: 12,
                        borderSkipped: false,
                        maxBarThickness: 34
                    },
                    {
                        type: "bar",
                        label: "Returned",
                        data: series.points.map(function (point) {
                            return point.returnedCount;
                        }),
                        backgroundColor: "rgba(145, 213, 166, 0.96)",
                        borderRadius: 12,
                        borderSkipped: false,
                        maxBarThickness: 34
                    }
                ]
            };
        }

        function renderChart() {
            var currentSeries = chartSeries[activeRange] || chartSeries.day;
            if (!currentSeries) {
                return;
            }

            var chartData = buildDataset(currentSeries);
            updateSummary(currentSeries);
            syncRangeButtons(activeRange);

            if (chartInstance) {
                chartInstance.data.labels = chartData.labels;
                chartInstance.data.datasets[0].data = chartData.datasets[0].data;
                chartInstance.data.datasets[1].data = chartData.datasets[1].data;
                chartInstance.update();
                return;
            }

            chartInstance = new Chart(chartCanvas, {
                type: "bar",
                data: chartData,
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    interaction: {
                        mode: "index",
                        intersect: false
                    },
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            backgroundColor: "rgba(23, 42, 28, 0.96)",
                            padding: 12,
                            titleFont: {
                                family: "Manrope"
                            },
                            bodyFont: {
                                family: "Manrope"
                            }
                        }
                    },
                    scales: {
                        x: {
                            grid: {
                                display: false
                            },
                            ticks: {
                                color: "#5d7065",
                                font: {
                                    family: "Manrope",
                                    weight: "700"
                                }
                            }
                        },
                        y: {
                            beginAtZero: true,
                            ticks: {
                                precision: 0,
                                color: "#6d7a70",
                                font: {
                                    family: "Manrope"
                                }
                            },
                            grid: {
                                color: "rgba(15, 127, 52, 0.10)"
                            },
                            border: {
                                display: false
                            }
                        }
                    }
                }
            });
        }

        Array.prototype.forEach.call(rangeButtons, function (button) {
            button.addEventListener("click", function () {
                activeRange = button.getAttribute("data-circulation-range") || "day";
                renderChart();
            });
        });

        document.addEventListener("dashboard:tabchange", function (event) {
            if (!event.detail || event.detail.panelId !== "admin-circulation-panel") {
                return;
            }

            window.requestAnimationFrame(renderChart);
        });

        if (!circulationPanel || !circulationPanel.hidden) {
            renderChart();
        }
    })();
</script>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>


