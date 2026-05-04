<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Operational Reports</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260504-global-side-nav-flush3">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Reports Center</span>
            <div class="brand-title mt-2">Analytics, exports, and audit trail</div>
        </div>
        <div class="nav-links">
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/dashboard">Dashboard</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/books">Books</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/issues">Issue / Return</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/reservations">Reservations</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/students">Students</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/fines">Fines</a>
            <a class="nav-pill active" href="${pageContext.request.contextPath}/admin/reports">Reports</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/references">Categories / Authors</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/profile">Profile</a>
            <form method="post" action="${pageContext.request.contextPath}/logout">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <button class="nav-pill warm border-0" type="submit" aria-label="Logout" title="Logout"><span class="nav-pill-icon"><i class="bi bi-power" aria-hidden="true"></i></span><span class="nav-pill-label">Logout</span></button>
            </form>
        </div>
    </div>

    <section class="hero-card mb-4">
        <div class="hero-card-grid">
            <div>
                <span class="tag-chip">Institutional Reporting</span>
                <h1 class="fw-bold mt-3 mb-2">Review operations with export-ready reports</h1>
                <p class="muted-text mb-0">Monitor circulation volume, overdue pressure, fine activity, reservation flow, and the admin audit trail from one reporting center.</p>
            </div>
            <div class="hero-side-note">
                <div class="hero-side-title">Date coverage</div>
                <strong class="hero-side-value">
                    <c:choose>
                        <c:when test="${not empty dateFrom or not empty dateTo}">
                            Filtered range
                        </c:when>
                        <c:otherwise>
                            All records
                        </c:otherwise>
                    </c:choose>
                </strong>
                <span class="hero-side-caption">
                    <c:choose>
                        <c:when test="${not empty dateFrom or not empty dateTo}">
                            ${empty dateFrom ? 'Beginning' : dateFrom} to ${empty dateTo ? 'Today' : dateTo}
                        </c:when>
                        <c:otherwise>
                            Viewing the full operational history available in the system.
                        </c:otherwise>
                    </c:choose>
                </span>
            </div>
        </div>
    </section>

    <section class="panel-card mb-4">
        <div class="section-title">Reporting filters</div>
        <form method="get" action="${pageContext.request.contextPath}/admin/reports" class="row g-3 align-items-end">
            <div class="col-md-4">
                <label class="form-label" for="dateFrom">Date from</label>
                <input class="form-control" id="dateFrom" name="dateFrom" type="date" value="${dateFrom}">
            </div>
            <div class="col-md-4">
                <label class="form-label" for="dateTo">Date to</label>
                <input class="form-control" id="dateTo" name="dateTo" type="date" value="${dateTo}">
            </div>
            <div class="col-md-4 d-flex flex-wrap gap-2">
                <button class="btn btn-brand" type="submit">
                    <i class="bi bi-funnel me-2"></i>Apply range
                </button>
                <a class="btn btn-warm" href="${pageContext.request.contextPath}/admin/reports">Clear filters</a>
            </div>
        </form>
    </section>

    <section class="stat-grid mb-4">
        <div class="metric-card">
            <div class="metric-value">${circulationCount}</div>
            <div class="metric-label">Circulation records</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${activeIssueCount}</div>
            <div class="metric-label">Active issue records</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${returnedCount}</div>
            <div class="metric-label">Returned items</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${overdueCount}</div>
            <div class="metric-label">Overdue items</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${fineRecordCount}</div>
            <div class="metric-label">Fine records</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${auditRecordCount}</div>
            <div class="metric-label">Audit events</div>
        </div>
    </section>

    <section class="panel-card mb-4">
        <div class="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3">
            <div>
                <div class="section-title mb-2">Export report files</div>
                <p class="helper-copy">Generate styled Excel exports for record review, submission, printing, or further spreadsheet analysis.</p>
            </div>
        </div>
        <div class="module-grid export-grid">
            <a class="module-card export-card" href="${pageContext.request.contextPath}/admin/reports/export?type=circulation&dateFrom=${dateFrom}&dateTo=${dateTo}">
                <h3><i class="bi bi-arrow-left-right me-2"></i>Circulation Report</h3>
                <p>Issued, returned, and overdue transactions with issue codes, dates, and fines.</p>
                <span class="action-link">Download Excel</span>
            </a>
            <a class="module-card export-card" href="${pageContext.request.contextPath}/admin/reports/export?type=overdue&dateFrom=${dateFrom}&dateTo=${dateTo}">
                <h3><i class="bi bi-exclamation-triangle me-2"></i>Overdue Report</h3>
                <p>Current overdue borrowers, days late, fine amounts, and issuing staff context.</p>
                <span class="action-link">Download Excel</span>
            </a>
            <a class="module-card export-card" href="${pageContext.request.contextPath}/admin/reports/export?type=fines&dateFrom=${dateFrom}&dateTo=${dateTo}">
                <h3><i class="bi bi-receipt me-2"></i>Fine Report</h3>
                <p>Unpaid, paid, and waived penalty records tied to each issue transaction.</p>
                <span class="action-link">Download Excel</span>
            </a>
            <a class="module-card export-card" href="${pageContext.request.contextPath}/admin/reports/export?type=reservations&dateFrom=${dateFrom}&dateTo=${dateTo}">
                <h3><i class="bi bi-hourglass-split me-2"></i>Reservation Report</h3>
                <p>Queue position, ready-claim windows, and reservation outcomes for each title.</p>
                <span class="action-link">Download Excel</span>
            </a>
            <a class="module-card export-card" href="${pageContext.request.contextPath}/admin/reports/export?type=audit&dateFrom=${dateFrom}&dateTo=${dateTo}">
                <h3><i class="bi bi-shield-check me-2"></i>Audit Report</h3>
                <p>Admin and system actions recorded for books, students, fines, reservations, and security events.</p>
                <span class="action-link">Download Excel</span>
            </a>
        </div>
    </section>

    <section class="panel-grid mb-4">
        <div class="panel-card">
            <div class="section-title">Collection and borrower insights</div>
            <div class="insight-split-grid">
                <div class="insight-panel">
                    <div class="insight-panel-title">Top borrowed titles</div>
                    <ul class="list-clean">
                        <c:forEach items="${topTitles}" var="entry">
                            <li class="d-flex justify-content-between align-items-center">
                                <span>${entry.key}</span>
                                <span class="tag-chip">${entry.value} borrow(s)</span>
                            </li>
                        </c:forEach>
                        <c:if test="${empty topTitles}">
                            <li class="muted-text">No title data is available for the current date range.</li>
                        </c:if>
                    </ul>
                </div>
                <div class="insight-panel">
                    <div class="insight-panel-title">Most active borrowers</div>
                    <ul class="list-clean">
                        <c:forEach items="${topBorrowers}" var="entry">
                            <li class="d-flex justify-content-between align-items-center">
                                <span>${entry.key}</span>
                                <span class="tag-chip">${entry.value} loan(s)</span>
                            </li>
                        </c:forEach>
                        <c:if test="${empty topBorrowers}">
                            <li class="muted-text">Borrower activity will appear once circulation data exists.</li>
                        </c:if>
                    </ul>
                </div>
            </div>
        </div>

        <div class="panel-card">
            <div class="section-title">Fine summary</div>
            <div class="chart-summary-grid">
                <div class="chart-summary-card">
                    <span class="chart-summary-label">Outstanding</span>
                    <strong class="chart-summary-value">${unpaidFineTotal}</strong>
                    <span class="chart-summary-note">Current unpaid balance within the selected reporting range.</span>
                </div>
                <div class="chart-summary-card">
                    <span class="chart-summary-label">Collected</span>
                    <strong class="chart-summary-value">${paidFineTotal}</strong>
                    <span class="chart-summary-note">Fine amounts already marked as paid by admin staff.</span>
                </div>
                <div class="chart-summary-card">
                    <span class="chart-summary-label">Waived</span>
                    <strong class="chart-summary-value">${waivedFineTotal}</strong>
                    <span class="chart-summary-note">Charges cleared through waiver decisions or admin discretion.</span>
                </div>
            </div>
        </div>
    </section>

    <section class="panel-grid panel-grid-equal mb-4">
        <div class="panel-card">
            <div class="section-title">Overdue snapshot</div>
            <div class="table-responsive">
                <table class="table align-middle">
                    <thead>
                    <tr>
                        <th>Student</th>
                        <th>Book</th>
                        <th>Due date</th>
                        <th>Fine</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${overdueRecords}" var="issue">
                        <tr>
                            <td>${issue.student.studentId} - ${issue.student.user.name}</td>
                            <td>${issue.book.title}</td>
                            <td>${issue.dueDateDisplay}</td>
                            <td>${issue.fineAmount}</td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty overdueRecords}">
                        <tr>
                            <td colspan="4" class="text-center muted-text">No overdue records matched the current filters.</td>
                        </tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="panel-card">
            <div class="section-title">Reservation snapshot</div>
            <div class="info-grid mb-3">
                <div class="info-tile">
                    <span class="info-tile-label">Pending</span>
                    <span class="info-tile-value">${pendingReservationCount}</span>
                </div>
                <div class="info-tile">
                    <span class="info-tile-label">Ready</span>
                    <span class="info-tile-value">${readyReservationCount}</span>
                </div>
                <div class="info-tile">
                    <span class="info-tile-label">Claimed</span>
                    <span class="info-tile-value">${claimedReservationCount}</span>
                </div>
                <div class="info-tile">
                    <span class="info-tile-label">Cancelled</span>
                    <span class="info-tile-value">${cancelledReservationCount}</span>
                </div>
            </div>
            <div class="table-responsive">
                <table class="table align-middle">
                    <thead>
                    <tr>
                        <th>Book</th>
                        <th>Borrower</th>
                        <th>Status</th>
                        <th>Queue</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${reservationRecords}" var="reservation">
                        <tr>
                            <td>${reservation.book.title}</td>
                            <td>${reservation.student.studentId} - ${reservation.student.user.name}</td>
                            <td><span class="tag-chip">${reservation.status}</span></td>
                            <td>${reservation.queuePosition}</td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty reservationRecords}">
                        <tr>
                            <td colspan="4" class="text-center muted-text">No reservation records matched the current filters.</td>
                        </tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </section>

    <section class="panel-grid panel-grid-equal">
        <div class="panel-card">
            <div class="section-title">Recent fine activity</div>
            <div class="table-responsive">
                <table class="table align-middle">
                    <thead>
                    <tr>
                        <th>Student</th>
                        <th>Amount</th>
                        <th>Status</th>
                        <th>Calculated</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${fineRecords}" var="fine">
                        <tr>
                            <td>${fine.student.studentId} - ${fine.student.user.name}</td>
                            <td>${fine.amount}</td>
                            <td><span class="tag-chip">${fine.status}</span></td>
                            <td>${fine.calculatedAtDisplay}</td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty fineRecords}">
                        <tr>
                            <td colspan="4" class="text-center muted-text">No fine activity was found for the selected range.</td>
                        </tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="panel-card">
            <div class="section-title">Recent audit events</div>
            <div class="audit-timeline">
                <c:forEach items="${auditRecords}" var="log">
                    <div class="audit-item">
                        <div class="audit-item-badge"><i class="bi bi-activity"></i></div>
                        <div>
                            <div class="audit-item-heading">${log.summary}</div>
                            <div class="audit-item-meta">${log.action} | ${empty log.actorName ? 'System' : log.actorName} | ${log.createdAtDisplay}</div>
                            <c:if test="${not empty log.details}">
                                <div class="audit-item-copy">${log.details}</div>
                            </c:if>
                        </div>
                    </div>
                </c:forEach>
                <c:if test="${empty auditRecords}">
                    <div class="muted-text">No audit events matched the selected report range.</div>
                </c:if>
            </div>
        </div>
    </section>
</div>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>


