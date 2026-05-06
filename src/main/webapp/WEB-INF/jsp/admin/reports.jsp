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
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.3/dist/chart.umd.min.js"></script>
</head>
<body>
<c:set var="activeReportTab" value="${empty reportTab ? 'insights' : reportTab}" />
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

    <section class="stat-grid reports-stat-grid mb-4">
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

    <section class="dashboard-tab-shell mb-4" data-report-tabs data-report-initial-tab="${activeReportTab}">
        <div class="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
                <div class="section-title mb-2">Report views</div>
                <p class="helper-copy mb-0">Switch between focused report groups instead of scrolling through one long page.</p>
            </div>
        </div>

        <div class="dashboard-tab-nav" role="tablist" aria-label="Report center views">
            <button class="dashboard-tab-button" type="button" role="tab" id="reports-insights-tab" aria-selected="true" aria-controls="reports-insights-panel" data-report-tab-button data-report-tab-target="reports-insights-panel">
                <i class="bi bi-bar-chart-line"></i>
                <span>Insights</span>
            </button>
            <button class="dashboard-tab-button" type="button" role="tab" id="reports-borrowing-tab" aria-selected="false" aria-controls="reports-borrowing-panel" data-report-tab-button data-report-tab-target="reports-borrowing-panel">
                <i class="bi bi-journal-richtext"></i>
                <span>Borrowing</span>
            </button>
            <button class="dashboard-tab-button" type="button" role="tab" id="reports-audit-tab" aria-selected="false" aria-controls="reports-audit-panel" data-report-tab-button data-report-tab-target="reports-audit-panel">
                <i class="bi bi-shield-check"></i>
                <span>Audit & fines</span>
            </button>
            <button class="dashboard-tab-button" type="button" role="tab" id="reports-exports-tab" aria-selected="false" aria-controls="reports-exports-panel" data-report-tab-button data-report-tab-target="reports-exports-panel">
                <i class="bi bi-download"></i>
                <span>Exports</span>
            </button>
        </div>
    </section>

    <section class="dashboard-tab-panels" data-report-panel-shell>
        <div class="dashboard-tab-panel" id="reports-exports-panel" role="tabpanel" aria-labelledby="reports-exports-tab" data-report-tab-panel>
            <section class="panel-card">
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
        </div>

        <div class="dashboard-tab-panel" id="reports-insights-panel" role="tabpanel" aria-labelledby="reports-insights-tab" data-report-tab-panel hidden>
            <section class="panel-grid">
                <div class="panel-card chart-card">
                    <div class="chart-header">
                        <div class="chart-copy">
                            <div class="section-title mb-2" id="reportsCirculationChartTitle">Interactive circulation graph</div>
                            <p id="reportsCirculationChartDescription">Switch between day, week, month, and year views to monitor borrowing activity at the level you need.</p>
                        </div>
                        <div class="chart-toolbar">
                            <div class="chart-range-switcher" role="tablist" aria-label="Reports circulation chart range">
                                <c:forEach items="${circulationChartSeries}" var="series" varStatus="status">
                                    <button class="chart-range-button <c:if test='${status.first}'>is-active</c:if>"
                                            type="button"
                                            role="tab"
                                            aria-selected="${status.first ? 'true' : 'false'}"
                                            data-reports-circulation-range-button
                                            data-reports-circulation-range="${series.key}">
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
                            <canvas id="reportsCirculationChart" aria-label="Interactive circulation chart for reports"></canvas>
                        </div>
                        <div class="chart-summary-grid">
                            <div class="chart-summary-card">
                                <span class="chart-summary-label">Issued in view</span>
                                <strong class="chart-summary-value" id="reportsCirculationIssuedTotal">0</strong>
                                <span class="chart-summary-note">Books issued in the selected range.</span>
                            </div>
                            <div class="chart-summary-card">
                                <span class="chart-summary-label">Returned in view</span>
                                <strong class="chart-summary-value" id="reportsCirculationReturnedTotal">0</strong>
                                <span class="chart-summary-note">Books returned in the selected range.</span>
                            </div>
                            <div class="chart-summary-card">
                                <span class="chart-summary-label" id="reportsCirculationPeakLabel">Peak issued in a day</span>
                                <strong class="chart-summary-value" id="reportsCirculationPeakValue">0</strong>
                                <span class="chart-summary-note" id="reportsCirculationPeakNote">Highest borrowing spike inside this view.</span>
                            </div>
                        </div>
                    </div>
                </div>

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
        </div>

        <div class="dashboard-tab-panel" id="reports-borrowing-panel" role="tabpanel" aria-labelledby="reports-borrowing-tab" data-report-tab-panel hidden>
            <section class="panel-grid panel-grid-equal">
                <div class="panel-card" data-table-search-section data-table-search-empty="No overdue rows matched your search on this page.">
                    <div class="table-search-header">
                        <div class="section-title">Overdue snapshot</div>
                        <div class="table-search-actions">
                            <span class="table-search-meta" data-table-search-count></span>
                            <label class="table-search-shell" aria-label="Search overdue snapshot">
                                <i class="bi bi-search" aria-hidden="true"></i>
                                <input class="table-search-input" type="search" placeholder="Search this table" data-table-search-input>
                            </label>
                        </div>
                    </div>
                    <div class="table-responsive">
                        <table class="table align-middle" data-table-search-table>
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
                    <c:if test="${overdueRecordsPage.totalPages > 1}">
                        <nav class="mt-3" aria-label="Overdue snapshot pages">
                            <ul class="pagination justify-content-center mb-0">
                                <li class="page-item <c:if test='${!overdueRecordsPage.hasPrevious}'>disabled</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/admin/reports?reportTab=borrowing&dateFrom=${dateFrom}&dateTo=${dateTo}&overduePage=${overdueRecordsPage.previousPage}&reservationPage=${reservationRecordsPage.page}&finePage=${fineRecordsPage.page}&auditPage=${auditRecordsPage.page}">Previous</a>
                                </li>
                                <c:forEach begin="${overdueRecordsPage.startPage}" end="${overdueRecordsPage.endPage}" var="pageNumber">
                                    <li class="page-item <c:if test='${pageNumber == overdueRecordsPage.page}'>active</c:if>">
                                        <a class="page-link" href="${pageContext.request.contextPath}/admin/reports?reportTab=borrowing&dateFrom=${dateFrom}&dateTo=${dateTo}&overduePage=${pageNumber}&reservationPage=${reservationRecordsPage.page}&finePage=${fineRecordsPage.page}&auditPage=${auditRecordsPage.page}">${pageNumber}</a>
                                    </li>
                                </c:forEach>
                                <li class="page-item <c:if test='${!overdueRecordsPage.hasNext}'>disabled</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/admin/reports?reportTab=borrowing&dateFrom=${dateFrom}&dateTo=${dateTo}&overduePage=${overdueRecordsPage.nextPage}&reservationPage=${reservationRecordsPage.page}&finePage=${fineRecordsPage.page}&auditPage=${auditRecordsPage.page}">Next</a>
                                </li>
                            </ul>
                        </nav>
                    </c:if>
                </div>

                <div class="panel-card reservation-snapshot-card" data-table-search-section data-table-search-empty="No reservation rows matched your search on this page.">
                    <div class="table-search-header">
                        <div class="section-title">Reservation snapshot</div>
                        <div class="table-search-actions">
                            <span class="table-search-meta" data-table-search-count></span>
                            <label class="table-search-shell" aria-label="Search reservation snapshot">
                                <i class="bi bi-search" aria-hidden="true"></i>
                                <input class="table-search-input" type="search" placeholder="Search this table" data-table-search-input>
                            </label>
                        </div>
                    </div>
                    <div class="info-grid reservation-summary-grid mb-3">
                        <div class="info-tile reservation-summary-tile">
                            <span class="info-tile-label">Pending</span>
                            <span class="info-tile-value">${pendingReservationCount}</span>
                        </div>
                        <div class="info-tile reservation-summary-tile">
                            <span class="info-tile-label">Ready</span>
                            <span class="info-tile-value">${readyReservationCount}</span>
                        </div>
                        <div class="info-tile reservation-summary-tile">
                            <span class="info-tile-label">Claimed</span>
                            <span class="info-tile-value">${claimedReservationCount}</span>
                        </div>
                        <div class="info-tile reservation-summary-tile">
                            <span class="info-tile-label">Cancelled</span>
                            <span class="info-tile-value">${cancelledReservationCount}</span>
                        </div>
                    </div>
                    <div class="table-responsive reservation-table-wrap">
                        <table class="table align-middle reservation-table" data-table-search-table>
                            <colgroup>
                                <col class="reservation-col-book">
                                <col class="reservation-col-borrower">
                                <col class="reservation-col-status">
                                <col class="reservation-col-queue">
                            </colgroup>
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
                                    <td class="reservation-book-cell">${reservation.book.title}</td>
                                    <td class="reservation-borrower-cell">${reservation.student.studentId} - ${reservation.student.user.name}</td>
                                    <td><span class="tag-chip reservation-status-chip">${reservation.status}</span></td>
                                    <td class="reservation-queue-cell">${reservation.queuePosition}</td>
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
                    <c:if test="${reservationRecordsPage.totalPages > 1}">
                        <nav class="mt-3" aria-label="Reservation snapshot pages">
                            <ul class="pagination justify-content-center mb-0">
                                <li class="page-item <c:if test='${!reservationRecordsPage.hasPrevious}'>disabled</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/admin/reports?reportTab=borrowing&dateFrom=${dateFrom}&dateTo=${dateTo}&overduePage=${overdueRecordsPage.page}&reservationPage=${reservationRecordsPage.previousPage}&finePage=${fineRecordsPage.page}&auditPage=${auditRecordsPage.page}">Previous</a>
                                </li>
                                <c:forEach begin="${reservationRecordsPage.startPage}" end="${reservationRecordsPage.endPage}" var="pageNumber">
                                    <li class="page-item <c:if test='${pageNumber == reservationRecordsPage.page}'>active</c:if>">
                                        <a class="page-link" href="${pageContext.request.contextPath}/admin/reports?reportTab=borrowing&dateFrom=${dateFrom}&dateTo=${dateTo}&overduePage=${overdueRecordsPage.page}&reservationPage=${pageNumber}&finePage=${fineRecordsPage.page}&auditPage=${auditRecordsPage.page}">${pageNumber}</a>
                                    </li>
                                </c:forEach>
                                <li class="page-item <c:if test='${!reservationRecordsPage.hasNext}'>disabled</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/admin/reports?reportTab=borrowing&dateFrom=${dateFrom}&dateTo=${dateTo}&overduePage=${overdueRecordsPage.page}&reservationPage=${reservationRecordsPage.nextPage}&finePage=${fineRecordsPage.page}&auditPage=${auditRecordsPage.page}">Next</a>
                                </li>
                            </ul>
                        </nav>
                    </c:if>
                </div>
            </section>
        </div>

        <div class="dashboard-tab-panel" id="reports-audit-panel" role="tabpanel" aria-labelledby="reports-audit-tab" data-report-tab-panel hidden>
            <section class="panel-grid panel-grid-equal">
                <div class="panel-card" data-table-search-section data-table-search-empty="No fine activity rows matched your search on this page.">
                    <div class="table-search-header">
                        <div class="section-title">Recent fine activity</div>
                        <div class="table-search-actions">
                            <span class="table-search-meta" data-table-search-count></span>
                            <label class="table-search-shell" aria-label="Search recent fine activity">
                                <i class="bi bi-search" aria-hidden="true"></i>
                                <input class="table-search-input" type="search" placeholder="Search this table" data-table-search-input>
                            </label>
                        </div>
                    </div>
                    <div class="table-responsive">
                        <table class="table align-middle" data-table-search-table>
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
                    <c:if test="${fineRecordsPage.totalPages > 1}">
                        <nav class="mt-3" aria-label="Recent fine activity pages">
                            <ul class="pagination justify-content-center mb-0">
                                <li class="page-item <c:if test='${!fineRecordsPage.hasPrevious}'>disabled</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/admin/reports?reportTab=audit&dateFrom=${dateFrom}&dateTo=${dateTo}&overduePage=${overdueRecordsPage.page}&reservationPage=${reservationRecordsPage.page}&finePage=${fineRecordsPage.previousPage}&auditPage=${auditRecordsPage.page}">Previous</a>
                                </li>
                                <c:forEach begin="${fineRecordsPage.startPage}" end="${fineRecordsPage.endPage}" var="pageNumber">
                                    <li class="page-item <c:if test='${pageNumber == fineRecordsPage.page}'>active</c:if>">
                                        <a class="page-link" href="${pageContext.request.contextPath}/admin/reports?reportTab=audit&dateFrom=${dateFrom}&dateTo=${dateTo}&overduePage=${overdueRecordsPage.page}&reservationPage=${reservationRecordsPage.page}&finePage=${pageNumber}&auditPage=${auditRecordsPage.page}">${pageNumber}</a>
                                    </li>
                                </c:forEach>
                                <li class="page-item <c:if test='${!fineRecordsPage.hasNext}'>disabled</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/admin/reports?reportTab=audit&dateFrom=${dateFrom}&dateTo=${dateTo}&overduePage=${overdueRecordsPage.page}&reservationPage=${reservationRecordsPage.page}&finePage=${fineRecordsPage.nextPage}&auditPage=${auditRecordsPage.page}">Next</a>
                                </li>
                            </ul>
                        </nav>
                    </c:if>
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
                    <c:if test="${auditRecordsPage.totalPages > 1}">
                        <nav class="mt-3" aria-label="Recent audit event pages">
                            <ul class="pagination justify-content-center mb-0">
                                <li class="page-item <c:if test='${!auditRecordsPage.hasPrevious}'>disabled</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/admin/reports?reportTab=audit&dateFrom=${dateFrom}&dateTo=${dateTo}&overduePage=${overdueRecordsPage.page}&reservationPage=${reservationRecordsPage.page}&finePage=${fineRecordsPage.page}&auditPage=${auditRecordsPage.previousPage}">Previous</a>
                                </li>
                                <c:forEach begin="${auditRecordsPage.startPage}" end="${auditRecordsPage.endPage}" var="pageNumber">
                                    <li class="page-item <c:if test='${pageNumber == auditRecordsPage.page}'>active</c:if>">
                                        <a class="page-link" href="${pageContext.request.contextPath}/admin/reports?reportTab=audit&dateFrom=${dateFrom}&dateTo=${dateTo}&overduePage=${overdueRecordsPage.page}&reservationPage=${reservationRecordsPage.page}&finePage=${fineRecordsPage.page}&auditPage=${pageNumber}">${pageNumber}</a>
                                    </li>
                                </c:forEach>
                                <li class="page-item <c:if test='${!auditRecordsPage.hasNext}'>disabled</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/admin/reports?reportTab=audit&dateFrom=${dateFrom}&dateTo=${dateTo}&overduePage=${overdueRecordsPage.page}&reservationPage=${reservationRecordsPage.page}&finePage=${fineRecordsPage.page}&auditPage=${auditRecordsPage.nextPage}">Next</a>
                                </li>
                            </ul>
                        </nav>
                    </c:if>
                </div>
            </section>
        </div>
    </section>
</div>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
<script>
    (function () {
        var tabRoot = document.querySelector("[data-report-tabs]");
        if (!tabRoot) {
            return;
        }

        var buttons = document.querySelectorAll("[data-report-tab-button]");
        var panels = document.querySelectorAll("[data-report-tab-panel]");

        function activateTab(targetId) {
            Array.prototype.forEach.call(buttons, function (button) {
                var isActive = button.getAttribute("data-report-tab-target") === targetId;
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
                activateTab(button.getAttribute("data-report-tab-target"));
            });
        });

        var initialTab = tabRoot.getAttribute("data-report-initial-tab") || "insights";
        var initialPanelMap = {
            exports: "reports-exports-panel",
            insights: "reports-insights-panel",
            borrowing: "reports-borrowing-panel",
            audit: "reports-audit-panel"
        };

        activateTab(initialPanelMap[initialTab] || "reports-insights-panel");
    })();
</script>
<script>
    (function () {
        var chartCanvas = document.getElementById("reportsCirculationChart");
        if (!chartCanvas || typeof Chart === "undefined") {
            return;
        }

        var chartInstance;
        var rangeButtons = document.querySelectorAll("[data-reports-circulation-range-button]");
        var titleNode = document.getElementById("reportsCirculationChartTitle");
        var descriptionNode = document.getElementById("reportsCirculationChartDescription");
        var issuedTotalNode = document.getElementById("reportsCirculationIssuedTotal");
        var returnedTotalNode = document.getElementById("reportsCirculationReturnedTotal");
        var peakLabelNode = document.getElementById("reportsCirculationPeakLabel");
        var peakValueNode = document.getElementById("reportsCirculationPeakValue");
        var peakNoteNode = document.getElementById("reportsCirculationPeakNote");
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
                var isActive = button.getAttribute("data-reports-circulation-range") === nextRange;
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
                                color: "#708274",
                                font: {
                                    family: "Manrope"
                                }
                            },
                            grid: {
                                color: "rgba(16, 90, 42, 0.10)",
                                drawBorder: false
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
                activeRange = button.getAttribute("data-reports-circulation-range") || "day";
                renderChart();
            });
        });

        renderChart();
    })();
</script>
</body>
</html>

