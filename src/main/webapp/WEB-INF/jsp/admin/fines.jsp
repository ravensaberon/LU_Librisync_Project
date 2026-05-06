<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Fine Management</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Fine Ledger</span>
            <div class="brand-title mt-2">Payment and waiver control</div>
        </div>
        <div class="nav-links">
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/dashboard">Dashboard</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/books">Books</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/issues">Issue / Return</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/students">Students</a>
            <a class="nav-pill active" href="${pageContext.request.contextPath}/admin/fines">Fines</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/reports">Reports</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/references">Categories / Authors</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/profile">Profile</a>
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
        <div class="hero-card-grid">
            <div>
                <span class="tag-chip">Financial Accountability</span>
                <h1 class="fw-bold mt-3 mb-2">Manage overdue charges with a real ledger</h1>
                <p class="muted-text mb-0">Track unpaid balances, mark penalties as settled, and record waived charges without losing the circulation history behind each fine.</p>
            </div>
            <div class="hero-side-note">
                <div class="hero-side-title">Filtered fine records</div>
                <strong class="hero-side-value">${filteredFineCount}</strong>
                <span class="hero-side-caption">Outstanding in current list: ${filteredOutstandingTotal}</span>
            </div>
        </div>
    </section>

    <section class="stat-grid fines-stat-grid mb-4">
        <div class="metric-card">
            <div class="metric-value">${outstandingFineCount}</div>
            <div class="metric-label">Unpaid fine records</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${outstandingFineTotal}</div>
            <div class="metric-label">Outstanding balance</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${paidFineCount}</div>
            <div class="metric-label">Paid fine records</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${paidFineTotal}</div>
            <div class="metric-label">Collected amount</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${waivedFineCount}</div>
            <div class="metric-label">Waived fine records</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${waivedFineTotal}</div>
            <div class="metric-label">Waived amount</div>
        </div>
    </section>

    <section class="panel-grid mb-4">
        <div class="panel-card">
            <div class="section-title">Filter fine ledger</div>
            <form method="get" action="${pageContext.request.contextPath}/admin/fines" class="row g-3 align-items-end">
                <div class="col-md-5">
                    <label class="form-label" for="studentKeyword">Student search</label>
                    <input class="form-control" id="studentKeyword" name="studentKeyword" value="${studentKeyword}" placeholder="Student ID, name, or email">
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="status">Fine status</label>
                    <select class="form-select" id="status" name="status">
                        <option value="">All statuses</option>
                        <c:forEach items="${fineStatuses}" var="fineStatus">
                            <option value="${fineStatus}" <c:if test="${selectedStatus == fineStatus}">selected</c:if>>${fineStatus}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-3 d-grid">
                    <button class="btn btn-brand" type="submit">
                        <i class="bi bi-funnel me-2"></i>Apply filters
                    </button>
                </div>
            </form>
        </div>

        <div class="panel-card">
            <div class="section-title">Recommended desk workflow</div>
            <div class="support-list">
                <div class="support-item">
                    <strong>Review the borrower context</strong>
                    <span>Use student search and the issue code in this ledger before accepting payment or waiving a penalty.</span>
                </div>
                <div class="support-item">
                    <strong>Record staff-side decisions clearly</strong>
                    <span>Paid and waived actions write into the audit trail so accountability is visible in the admin reports module.</span>
                </div>
            </div>
        </div>
    </section>

    <section class="panel-card">
        <div class="section-title">Fine records</div>
        <div class="table-responsive">
            <table class="table align-middle">
                <thead>
                <tr>
                    <th>Student</th>
                    <th>Book / Issue</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Calculated</th>
                    <th>Paid or Waived</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${fines}" var="fine">
                    <tr>
                        <td>
                            <strong>${fine.student.user.name}</strong>
                            <div class="muted-text">${fine.student.studentId}</div>
                            <div class="muted-text">${fine.student.user.email}</div>
                        </td>
                        <td>
                            <strong>${fine.issueRecord.book.title}</strong>
                            <div class="muted-text">${fine.issueRecord.qrIssueCode}</div>
                        </td>
                        <td>${fine.amount}</td>
                        <td>
                            <c:choose>
                                <c:when test="${fine.status.name() == 'UNPAID'}">
                                    <span class="tag-chip warn">UNPAID</span>
                                </c:when>
                                <c:when test="${fine.status.name() == 'PAID'}">
                                    <span class="tag-chip">PAID</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="tag-chip subtle">WAIVED</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>${fine.calculatedAtDisplay}</td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty fine.paidAt}">
                                    ${fine.paidAtDisplay}
                                </c:when>
                                <c:otherwise>
                                    <span class="muted-text">Pending</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="table-actions">
                            <c:if test="${fine.status.name() == 'UNPAID'}">
                                <form method="post"
                                      action="${pageContext.request.contextPath}/admin/fines/${fine.id}/pay"
                                      data-confirm-title="Mark this fine as paid?"
                                      data-confirm-text="This will record the selected overdue penalty as settled."
                                      data-confirm-button-text="Yes, mark as paid"
                                      data-confirm-cancel-text="Review first">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                    <input type="hidden" name="page" value="${finesPage.page}">
                                    <input type="hidden" name="studentKeyword" value="${studentKeyword}">
                                    <input type="hidden" name="status" value="${selectedStatus}">
                                    <button class="icon-action" type="submit" title="Mark as paid">
                                        <i class="bi bi-cash-coin"></i>
                                    </button>
                                </form>
                                <form method="post"
                                      action="${pageContext.request.contextPath}/admin/fines/${fine.id}/waive"
                                      data-confirm-title="Waive this fine?"
                                      data-confirm-text="Only waive a penalty when there is a valid admin decision for the borrower."
                                      data-confirm-button-text="Yes, waive fine"
                                      data-confirm-cancel-text="Keep unpaid">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                    <input type="hidden" name="page" value="${finesPage.page}">
                                    <input type="hidden" name="studentKeyword" value="${studentKeyword}">
                                    <input type="hidden" name="status" value="${selectedStatus}">
                                    <button class="icon-action" type="submit" title="Waive fine">
                                        <i class="bi bi-receipt-cutoff"></i>
                                    </button>
                                </form>
                            </c:if>
                            <c:if test="${fine.status.name() != 'UNPAID'}">
                                <span class="muted-text">Recorded</span>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty fines}">
                    <tr>
                        <td colspan="7" class="text-center muted-text">No fine records matched the current filters.</td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
        <c:if test="${finesPage.totalPages > 1}">
            <nav class="mt-4" aria-label="Fine ledger pages">
                <ul class="pagination justify-content-center mb-0">
                    <li class="page-item <c:if test='${!finesPage.hasPrevious}'>disabled</c:if>">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/fines?page=${finesPage.previousPage}&studentKeyword=${studentKeyword}&status=${selectedStatus}">Previous</a>
                    </li>
                    <c:forEach begin="${finesPage.startPage}" end="${finesPage.endPage}" var="pageNumber">
                        <li class="page-item <c:if test='${pageNumber == finesPage.page}'>active</c:if>">
                            <a class="page-link" href="${pageContext.request.contextPath}/admin/fines?page=${pageNumber}&studentKeyword=${studentKeyword}&status=${selectedStatus}">${pageNumber}</a>
                        </li>
                    </c:forEach>
                    <li class="page-item <c:if test='${!finesPage.hasNext}'>disabled</c:if>">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/fines?page=${finesPage.nextPage}&studentKeyword=${studentKeyword}&status=${selectedStatus}">Next</a>
                    </li>
                </ul>
            </nav>
        </c:if>
    </section>
</div>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>


