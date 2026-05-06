<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Borrowed Books</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Borrowed Books</span>
            <div class="brand-title mt-2">Active loans and return timeline</div>
        </div>
        <div class="nav-links">
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/dashboard">Dashboard</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/catalog">Catalog</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/reservations">Pickup requests</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/profile">Profile</a>
            <a class="nav-pill active" href="${pageContext.request.contextPath}/student/history">Borrowed books</a>
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

    <section class="panel-card">
        <div class="section-title">Borrowing overview</div>
        <div class="info-grid mb-4">
            <div class="info-tile">
                <span class="info-tile-label">Borrowed items</span>
                <span class="info-tile-value">${activeCount}</span>
            </div>
            <div class="info-tile">
                <span class="info-tile-label">Overdue items</span>
                <span class="info-tile-value">${overdueCount}</span>
            </div>
            <div class="info-tile">
                <span class="info-tile-label">Active reservations</span>
                <span class="info-tile-value">${reservationCount}</span>
            </div>
            <div class="info-tile">
                <span class="info-tile-label">Return requests</span>
                <span class="info-tile-value">${returnRequestCount}</span>
            </div>
            <div class="info-tile">
                <span class="info-tile-label">Outstanding fines</span>
                <span class="info-tile-value">${outstandingFineTotal}</span>
            </div>
            <div class="info-tile">
                <span class="info-tile-label">Returned books</span>
                <span class="info-tile-value">${returnedCount}</span>
            </div>
        </div>
        <div class="support-item mb-4">
            <strong>${borrowerStanding.statusLabel}</strong>
            <span>Active loans: ${borrowerStanding.activeLoansCount}/${borrowerStanding.maxActiveLoans} | Remaining slots: ${borrowerStanding.remainingLoanSlots}</span>
        </div>

        <div class="section-tabs mb-4" role="tablist" aria-label="Borrowed books sections">
            <a class="section-tab <c:if test='${activeTab == "all"}'>active</c:if>"
               href="${pageContext.request.contextPath}/student/history?tab=all&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}"
               role="tab"
               aria-selected="${activeTab == 'all'}">
                All
            </a>
            <a class="section-tab <c:if test='${activeTab == "current"}'>active</c:if>"
               href="${pageContext.request.contextPath}/student/history?tab=current&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}"
               role="tab"
               aria-selected="${activeTab == 'current'}">
                Currently borrowed
            </a>
            <a class="section-tab <c:if test='${activeTab == "requests"}'>active</c:if>"
               href="${pageContext.request.contextPath}/student/history?tab=requests&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}"
               role="tab"
               aria-selected="${activeTab == 'requests'}">
                Return requests
            </a>
            <a class="section-tab <c:if test='${activeTab == "returned"}'>active</c:if>"
               href="${pageContext.request.contextPath}/student/history?tab=returned&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}"
               role="tab"
               aria-selected="${activeTab == 'returned'}">
                Returned
            </a>
        </div>

        <c:if test="${activeTab == 'all'}">
            <div class="tab-panel active" role="tabpanel" aria-label="All borrowed records" data-table-search-section data-table-search-empty="No borrowed records matched your search on this page.">
                <div class="table-search-header">
                    <div class="section-title">All borrowed records</div>
                    <div class="table-search-actions">
                        <span class="table-search-meta" data-table-search-count></span>
                        <label class="table-search-shell" aria-label="Search all borrowed records">
                            <i class="bi bi-search" aria-hidden="true"></i>
                            <input class="table-search-input" type="search" placeholder="Search this table" data-table-search-input>
                        </label>
                    </div>
                </div>
                <div class="table-responsive">
                    <table class="table align-middle" data-table-search-table>
                        <thead>
                        <tr>
                            <th>Book</th>
                            <th>Issue date</th>
                            <th>Due date</th>
                            <th>Return date</th>
                            <th>Status</th>
                            <th>Fine</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${historyIssues}" var="issue">
                            <tr>
                                <td>${issue.book.title}</td>
                                <td>${issue.issueDateDisplay}</td>
                                <td>${issue.dueDateDisplay}</td>
                                <td>${issue.returnDateDisplay}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${issue.returnRequested}">
                                            <span class="tag-chip warn">RETURN REQUESTED</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="tag-chip">${issue.status}</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${issue.fineAmount}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${issue.returned}">
                                            <span class="muted-text">Completed</span>
                                        </c:when>
                                        <c:when test="${issue.returnRequested}">
                                            <div class="d-flex flex-wrap gap-2">
                                                <span class="tag-chip warn">Pending desk return</span>
                                                <form method="post" action="${pageContext.request.contextPath}/student/issues/${issue.id}/cancel-return-request">
                                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                    <input type="hidden" name="redirectTo" value="/student/history?tab=all&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}">
                                                    <button class="btn btn-warm" type="submit">Cancel request</button>
                                                </form>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <form method="post" action="${pageContext.request.contextPath}/student/issues/${issue.id}/request-return">
                                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                <input type="hidden" name="redirectTo" value="/student/history?tab=all&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}">
                                                <button class="btn btn-warm" type="submit">Request return</button>
                                            </form>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty historyIssues}">
                            <tr>
                                <td colspan="7" class="text-center muted-text">No borrowed records available yet.</td>
                            </tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
                <c:if test="${historyIssuesPage.totalPages > 1}">
                    <nav class="mt-4" aria-label="All borrowed record pages">
                        <ul class="pagination justify-content-center mb-0">
                            <li class="page-item <c:if test='${!historyIssuesPage.hasPrevious}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/student/history?tab=all&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.previousPage}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}">Previous</a>
                            </li>
                            <c:forEach begin="${historyIssuesPage.startPage}" end="${historyIssuesPage.endPage}" var="pageNumber">
                                <li class="page-item <c:if test='${pageNumber == historyIssuesPage.page}'>active</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/student/history?tab=all&activePage=${activeIssuesPage.page}&historyPage=${pageNumber}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}">${pageNumber}</a>
                                </li>
                            </c:forEach>
                            <li class="page-item <c:if test='${!historyIssuesPage.hasNext}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/student/history?tab=all&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.nextPage}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}">Next</a>
                            </li>
                        </ul>
                    </nav>
                </c:if>
            </div>
        </c:if>

        <c:if test="${activeTab == 'current'}">
            <div class="tab-panel active" role="tabpanel" aria-label="Currently borrowed" data-table-search-section data-table-search-empty="No current borrowed books matched your search on this page.">
                <div class="table-search-header">
                    <div class="section-title">Currently borrowed</div>
                    <div class="table-search-actions">
                        <span class="table-search-meta" data-table-search-count></span>
                        <label class="table-search-shell" aria-label="Search current borrowed books">
                            <i class="bi bi-search" aria-hidden="true"></i>
                            <input class="table-search-input" type="search" placeholder="Search this table" data-table-search-input>
                        </label>
                    </div>
                </div>
                <div class="table-responsive mb-4">
                    <table class="table align-middle" data-table-search-table>
                        <thead>
                        <tr>
                            <th>Book</th>
                            <th>Issue date</th>
                            <th>Due date</th>
                            <th>Status</th>
                            <th>Fine</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${activeIssues}" var="issue">
                            <tr>
                                <td>${issue.book.title}</td>
                                <td>${issue.issueDateDisplay}</td>
                                <td>${issue.dueDateDisplay}</td>
                                <td><span class="tag-chip">${issue.status}</span></td>
                                <td>${issue.fineAmount}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${issue.returnRequested}">
                                            <div class="d-flex flex-wrap gap-2">
                                                <span class="tag-chip warn">Pending desk return</span>
                                                <form method="post" action="${pageContext.request.contextPath}/student/issues/${issue.id}/cancel-return-request">
                                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                    <input type="hidden" name="redirectTo" value="/student/history?tab=current&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}">
                                                    <button class="btn btn-warm" type="submit">Cancel request</button>
                                                </form>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <form method="post" action="${pageContext.request.contextPath}/student/issues/${issue.id}/request-return">
                                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                <input type="hidden" name="redirectTo" value="/student/history?tab=current&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}">
                                                <button class="btn btn-warm" type="submit">Request return</button>
                                            </form>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty activeIssues}">
                            <tr>
                                <td colspan="6" class="text-center muted-text">You do not have any active borrowed books right now.</td>
                            </tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
                <c:if test="${activeIssuesPage.totalPages > 1}">
                    <nav class="mb-4" aria-label="Active borrowed books pages">
                        <ul class="pagination justify-content-center mb-0">
                            <li class="page-item <c:if test='${!activeIssuesPage.hasPrevious}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/student/history?tab=current&activePage=${activeIssuesPage.previousPage}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}">Previous</a>
                            </li>
                            <c:forEach begin="${activeIssuesPage.startPage}" end="${activeIssuesPage.endPage}" var="pageNumber">
                                <li class="page-item <c:if test='${pageNumber == activeIssuesPage.page}'>active</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/student/history?tab=current&activePage=${pageNumber}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}">${pageNumber}</a>
                                </li>
                            </c:forEach>
                            <li class="page-item <c:if test='${!activeIssuesPage.hasNext}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/student/history?tab=current&activePage=${activeIssuesPage.nextPage}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}">Next</a>
                            </li>
                        </ul>
                    </nav>
                </c:if>
            </div>
        </c:if>

        <c:if test="${activeTab == 'requests'}">
            <div class="tab-panel active" role="tabpanel" aria-label="Return requests" data-table-search-section data-table-search-empty="No return requests matched your search on this page.">
                <div class="table-search-header">
                    <div class="section-title">Return requests</div>
                    <div class="table-search-actions">
                        <span class="table-search-meta" data-table-search-count></span>
                        <label class="table-search-shell" aria-label="Search return requests">
                            <i class="bi bi-search" aria-hidden="true"></i>
                            <input class="table-search-input" type="search" placeholder="Search this table" data-table-search-input>
                        </label>
                    </div>
                </div>
                <div class="table-responsive">
                    <table class="table align-middle" data-table-search-table>
                        <thead>
                        <tr>
                            <th>Book</th>
                            <th>Issue date</th>
                            <th>Due date</th>
                            <th>Status</th>
                            <th>Fine</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${returnRequestIssues}" var="issue">
                            <tr>
                                <td>${issue.book.title}</td>
                                <td>${issue.issueDateDisplay}</td>
                                <td>${issue.dueDateDisplay}</td>
                                <td><span class="tag-chip warn">RETURN REQUESTED</span></td>
                                <td>${issue.fineAmount}</td>
                                <td>
                                    <div class="d-flex flex-wrap gap-2">
                                        <span class="tag-chip warn">Pending desk return</span>
                                        <form method="post" action="${pageContext.request.contextPath}/student/issues/${issue.id}/cancel-return-request">
                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                            <input type="hidden" name="redirectTo" value="/student/history?tab=requests&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.page}">
                                            <button class="btn btn-warm" type="submit">Cancel request</button>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty returnRequestIssues}">
                            <tr>
                                <td colspan="6" class="text-center muted-text">You do not have any pending return requests right now.</td>
                            </tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
                <c:if test="${returnRequestIssuesPage.totalPages > 1}">
                    <nav class="mt-4" aria-label="Return request pages">
                        <ul class="pagination justify-content-center mb-0">
                            <li class="page-item <c:if test='${!returnRequestIssuesPage.hasPrevious}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/student/history?tab=requests&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.previousPage}&returnedPage=${returnedIssuesPage.page}">Previous</a>
                            </li>
                            <c:forEach begin="${returnRequestIssuesPage.startPage}" end="${returnRequestIssuesPage.endPage}" var="pageNumber">
                                <li class="page-item <c:if test='${pageNumber == returnRequestIssuesPage.page}'>active</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/student/history?tab=requests&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${pageNumber}&returnedPage=${returnedIssuesPage.page}">${pageNumber}</a>
                                </li>
                            </c:forEach>
                            <li class="page-item <c:if test='${!returnRequestIssuesPage.hasNext}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/student/history?tab=requests&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.nextPage}&returnedPage=${returnedIssuesPage.page}">Next</a>
                            </li>
                        </ul>
                    </nav>
                </c:if>
            </div>
        </c:if>

        <c:if test="${activeTab == 'returned'}">
            <div class="tab-panel active" role="tabpanel" aria-label="Returned books" data-table-search-section data-table-search-empty="No returned books matched your search on this page.">
                <div class="table-search-header">
                    <div class="section-title">Returned books</div>
                    <div class="table-search-actions">
                        <span class="table-search-meta" data-table-search-count></span>
                        <label class="table-search-shell" aria-label="Search returned books">
                            <i class="bi bi-search" aria-hidden="true"></i>
                            <input class="table-search-input" type="search" placeholder="Search this table" data-table-search-input>
                        </label>
                    </div>
                </div>
                <div class="table-responsive">
                    <table class="table align-middle" data-table-search-table>
                        <thead>
                        <tr>
                            <th>Book</th>
                            <th>Issue date</th>
                            <th>Due date</th>
                            <th>Return date</th>
                            <th>Status</th>
                            <th>Fine</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${returnedIssues}" var="issue">
                            <tr>
                                <td>${issue.book.title}</td>
                                <td>${issue.issueDateDisplay}</td>
                                <td>${issue.dueDateDisplay}</td>
                                <td>${issue.returnDateDisplay}</td>
                                <td><span class="tag-chip">${issue.status}</span></td>
                                <td>${issue.fineAmount}</td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty returnedIssues}">
                            <tr>
                                <td colspan="6" class="text-center muted-text">You do not have any returned books yet.</td>
                            </tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
                <c:if test="${returnedIssuesPage.totalPages > 1}">
                    <nav class="mt-4" aria-label="Returned book pages">
                        <ul class="pagination justify-content-center mb-0">
                            <li class="page-item <c:if test='${!returnedIssuesPage.hasPrevious}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/student/history?tab=returned&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.previousPage}">Previous</a>
                            </li>
                            <c:forEach begin="${returnedIssuesPage.startPage}" end="${returnedIssuesPage.endPage}" var="pageNumber">
                                <li class="page-item <c:if test='${pageNumber == returnedIssuesPage.page}'>active</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/student/history?tab=returned&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${pageNumber}">${pageNumber}</a>
                                </li>
                            </c:forEach>
                            <li class="page-item <c:if test='${!returnedIssuesPage.hasNext}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/student/history?tab=returned&activePage=${activeIssuesPage.page}&historyPage=${historyIssuesPage.page}&requestPage=${returnRequestIssuesPage.page}&returnedPage=${returnedIssuesPage.nextPage}">Next</a>
                            </li>
                        </ul>
                    </nav>
                </c:if>
            </div>
        </c:if>
    </section>
</div>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>


