<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Student Directory</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260504-global-side-nav-flush3">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Student Directory</span>
            <div class="brand-title mt-2">Manage borrower accounts</div>
        </div>
        <div class="nav-links">
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/dashboard">Dashboard</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/books">Books</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/issues">Issue / Return</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/reservations">Reservations</a>
            <a class="nav-pill active" href="${pageContext.request.contextPath}/admin/students">Students</a>
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

    <c:if test="${not empty success}">
        <div class="alert alert-success">${success}</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-danger">${error}</div>
    </c:if>

    <section class="hero-card mb-4">
        <div class="hero-card-grid">
            <div>
                <span class="tag-chip">Borrower Accounts</span>
                <h1 class="fw-bold mt-3 mb-2">Student directory</h1>
                <p class="muted-text mb-0">Search, review, and manage student borrower records from one clean workspace.</p>
            </div>
            <div class="hero-side-note">
                <c:choose>
                    <c:when test="${not empty studentIdFilter}">
                        <div class="hero-side-title">Search matches</div>
                        <strong class="hero-side-value">${studentDirectoryFilteredCount}</strong>
                        <span class="hero-side-caption">Student ID filter: ${studentIdFilter}</span>
                    </c:when>
                    <c:when test="${studentView == 'archived'}">
                        <div class="hero-side-title">Archived students</div>
                        <strong class="hero-side-value">${studentDirectoryArchivedCount}</strong>
                        <span class="hero-side-caption">Restore accounts here or remove them permanently.</span>
                    </c:when>
                    <c:otherwise>
                        <div class="hero-side-title">Active students</div>
                        <strong class="hero-side-value">${studentDirectoryTotalCount}</strong>
                        <span class="hero-side-caption">${studentDirectoryBlockedCount} blocked borrowers need follow-up.</span>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </section>

    <section class="stat-grid mb-4">
        <div class="metric-card">
            <div class="metric-value">${studentDirectoryTotalCount}</div>
            <div class="metric-label">Active student accounts</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${studentDirectoryFilteredCount}</div>
            <div class="metric-label">Shown in current view</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${studentDirectoryClearedCount}</div>
            <div class="metric-label">Borrowing cleared</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${studentDirectoryActiveCount}</div>
            <div class="metric-label">ACTIVE in this view</div>
        </div>
    </section>

    <section class="panel-card directory-workspace">
        <div class="directory-toolbar">
            <div class="directory-toolbar-copy">
                <div class="section-title mb-2">Student directory</div>
                <div class="directory-toolbar-meta">
                    <span class="directory-meta-pill">${studentDirectoryFilteredCount} results</span>
                    <span class="directory-meta-pill subtle">Active: ${studentDirectoryTotalCount}</span>
                    <span class="directory-meta-pill subtle">Archived: ${studentDirectoryArchivedCount}</span>
                    <c:if test="${not empty studentIdFilter}">
                        <span class="directory-meta-pill subtle">ID: ${studentIdFilter}</span>
                    </c:if>
                    <span class="directory-meta-text">${studentDirectoryBlockedCount} blocked borrower<c:if test="${studentDirectoryBlockedCount != 1}">s</c:if> in this view</span>
                </div>
            </div>
            <div class="directory-toolbar-actions">
                <div class="d-flex flex-wrap gap-2">
                    <a class="btn <c:choose><c:when test='${studentView == "active"}'>btn-brand</c:when><c:otherwise>btn-warm</c:otherwise></c:choose>"
                       href="${pageContext.request.contextPath}/admin/students?view=active&studentId=${studentIdFilter}">
                        Active students
                    </a>
                    <a class="btn <c:choose><c:when test='${studentView == "archived"}'>btn-brand</c:when><c:otherwise>btn-warm</c:otherwise></c:choose>"
                       href="${pageContext.request.contextPath}/admin/students?view=archived&studentId=${studentIdFilter}">
                        Archived students
                    </a>
                </div>
                <form method="get" action="${pageContext.request.contextPath}/admin/students" class="directory-search-form">
                    <label class="visually-hidden" for="studentId">Search by student ID</label>
                    <input type="hidden" name="view" value="${studentView}">
                    <div class="directory-search-input">
                        <i class="bi bi-search" aria-hidden="true"></i>
                        <input class="form-control"
                               id="studentId"
                               name="studentId"
                               value="${studentIdFilter}"
                               placeholder="Search by student ID">
                    </div>
                    <button class="btn btn-brand" type="submit">Search</button>
                    <c:if test="${not empty studentIdFilter}">
                        <a class="btn btn-warm" href="${pageContext.request.contextPath}/admin/students?view=${studentView}">Clear</a>
                    </c:if>
                </form>
            </div>
        </div>

        <div class="table-responsive directory-table-wrap">
            <table class="table align-middle directory-table">
                <thead>
                <tr>
                    <th>Student</th>
                    <th>Program</th>
                    <th>Standing</th>
                    <th>Outstanding</th>
                    <th>Phone</th>
                    <th>Status</th>
                    <th>Action</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${students}" var="student">
                    <c:set var="standing" value="${borrowerStandingByStudentId[student.studentId]}"/>
                    <tr>
                        <td>
                            <div class="directory-student-cell">
                                <strong>${student.user.name}</strong>
                                <span>${student.studentId}</span>
                                <span>${student.user.email}</span>
                            </div>
                        </td>
                        <td>
                            <div class="directory-student-cell">
                                <strong>${empty student.course ? 'Not set' : student.course}</strong>
                                <span>${empty student.yearLevel ? 'Not set' : student.yearLevel}</span>
                            </div>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${standing.eligibleToBorrow}">
                                    <span class="tag-chip">Borrowing cleared</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="tag-chip warn">Blocked</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="directory-amount">${standing.outstandingFineAmount}</td>
                        <td>${empty student.phone ? 'Not provided' : student.phone}</td>
                        <td><span class="tag-chip subtle">${student.user.status}</span></td>
                        <td class="table-actions">
                            <button class="icon-action"
                                    type="button"
                                    data-student-id="${student.studentId}"
                                    data-student-label="${student.user.name}"
                                    title="Open student details"
                                    aria-label="Open student details for ${student.user.name}">
                                <i class="bi bi-eye"></i>
                            </button>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty students}">
                    <tr>
                        <td colspan="7" class="text-center muted-text">No student matched the current student ID search.</td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
        <c:if test="${studentsPage.totalPages > 1}">
            <nav class="mt-4" aria-label="Student directory pages">
                <ul class="pagination justify-content-center mb-0">
                    <li class="page-item <c:if test='${!studentsPage.hasPrevious}'>disabled</c:if>">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/students?page=${studentsPage.previousPage}&studentId=${studentIdFilter}&view=${studentView}">Previous</a>
                    </li>
                    <c:forEach begin="${studentsPage.startPage}" end="${studentsPage.endPage}" var="pageNumber">
                        <li class="page-item <c:if test='${pageNumber == studentsPage.page}'>active</c:if>">
                            <a class="page-link" href="${pageContext.request.contextPath}/admin/students?page=${pageNumber}&studentId=${studentIdFilter}&view=${studentView}">${pageNumber}</a>
                        </li>
                    </c:forEach>
                    <li class="page-item <c:if test='${!studentsPage.hasNext}'>disabled</c:if>">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/students?page=${studentsPage.nextPage}&studentId=${studentIdFilter}&view=${studentView}">Next</a>
                    </li>
                </ul>
            </nav>
        </c:if>
    </section>
</div>

<div class="modal fade" id="studentDetailModal" tabindex="-1" aria-labelledby="studentDetailModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-fullscreen-lg-down modal-xl">
        <div class="modal-content" id="studentDetailModalContent">
            <div class="modal-body modal-loading-state">
                <div class="text-center py-5">
                    <div class="spinner-border text-success mb-3" role="status" aria-hidden="true"></div>
                    <p class="mb-0 muted-text">Loading student details...</p>
                </div>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    (function () {
        var modalElement = document.getElementById("studentDetailModal");
        var modalContent = document.getElementById("studentDetailModalContent");
        var studentButtons = document.querySelectorAll("[data-student-id]");
        var bootstrapModal = modalElement ? new bootstrap.Modal(modalElement) : null;
        var autoOpenStudentId = "${modalStudentId}";
        var currentStudentView = "${studentView}";
        var cityZipCodes = {
            <c:forEach items="${registrationCityZipCodes}" var="entry" varStatus="status">
            "${entry.key}": "${entry.value}"<c:if test="${!status.last}">,</c:if>
            </c:forEach>
        };

        function initAddressForms(root) {
            if (!window.LatteAndLettersAddress) {
                return;
            }

            var scope = root || document;
            window.LatteAndLettersAddress.initForm({
                cityMunicipality: scope.querySelector("#modalCityMunicipality"),
                barangay: scope.querySelector("#modalBarangay"),
                zipcode: scope.querySelector("#modalZipcode"),
                endpoint: "${pageContext.request.contextPath}/register/barangays",
                cityZipCodes: cityZipCodes
            });
        }

        function showLoadingState() {
            modalContent.innerHTML = '' +
                '<div class="modal-body modal-loading-state">' +
                '    <div class="text-center py-5">' +
                '        <div class="spinner-border text-success mb-3" role="status" aria-hidden="true"></div>' +
                '        <p class="mb-0 muted-text">Loading student details...</p>' +
                '    </div>' +
                '</div>';
        }

        function showErrorState() {
            modalContent.innerHTML = '' +
                '<div class="modal-header modal-header-brand">' +
                '    <div>' +
                '        <div class="modal-kicker">Student Account</div>' +
                '        <h2 class="modal-title h4 mb-1">Unavailable</h2>' +
                '        <p class="modal-subtitle mb-0">The student detail popup could not be loaded.</p>' +
                '    </div>' +
                '    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>' +
                '</div>' +
                '<div class="modal-body modal-loading-state">' +
                '    <div class="text-center py-5">' +
                '        <i class="bi bi-exclamation-circle modal-empty-icon"></i>' +
                '        <p class="mb-0 muted-text">Unable to load the student record right now.</p>' +
                '    </div>' +
                '</div>';
        }

        function openStudentModal(studentId) {
            if (!studentId || !bootstrapModal) {
                return;
            }

            showLoadingState();
            bootstrapModal.show();

            fetch("${pageContext.request.contextPath}/admin/students/" + encodeURIComponent(studentId) + "/modal?view=" + encodeURIComponent(currentStudentView), {
                headers: {
                    "X-Requested-With": "XMLHttpRequest"
                }
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error("Request failed");
                    }
                    return response.text();
                })
                .then(function (html) {
                    modalContent.innerHTML = html;
                    initAddressForms(modalContent);
                })
                .catch(function () {
                    showErrorState();
                });
        }

        studentButtons.forEach(function (button) {
            button.addEventListener("click", function () {
                openStudentModal(button.getAttribute("data-student-id"));
            });
        });

        if (autoOpenStudentId) {
            openStudentModal(autoOpenStudentId);
        }

        initAddressForms(document);
    })();
</script>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>


