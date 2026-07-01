<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Student Details</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260504-global-side-nav-flush3">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Student Details</span>
            <div class="brand-title mt-2">Complete student circulation view</div>
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
        <div class="row g-4 align-items-center">
            <div class="col-md-8">
                <h1 class="fw-bold mb-2">${student.user.name}</h1>
                <p class="muted-text mb-0">Student ID: <strong>${student.studentId}</strong> | Email: <strong>${student.user.email}</strong></p>
            </div>
            <div class="col-md-4 text-md-end">
                <a class="action-link" href="${pageContext.request.contextPath}/admin/students">Back to student directory</a>
            </div>
        </div>
    </section>

    <section class="info-grid mb-4">
        <div class="info-tile">
            <span class="info-tile-label">Course</span>
            <span class="info-tile-value">${student.course}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Year Level</span>
            <span class="info-tile-value">${student.yearLevel}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Phone</span>
            <span class="info-tile-value">${student.phone}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Address</span>
            <span class="info-tile-value">${student.address}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Date of Birth</span>
            <span class="info-tile-value">${student.dateOfBirthDisplay}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Account Status</span>
            <span class="info-tile-value">${student.user.status}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Active Loans</span>
            <span class="info-tile-value">${activeCount}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Borrowing History</span>
            <span class="info-tile-value">${historyCount}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Overdue Items</span>
            <span class="info-tile-value">${overdueItems}</span>
        </div>
        <div class="info-tile">
            <span class="info-tile-label">Accumulated Fines</span>
            <span class="info-tile-value">${totalFineAmount}</span>
        </div>
    </section>

    <section class="detail-grid mb-4">
        <div class="panel-card">
            <div class="section-title">Update student account</div>
            <p class="helper-copy mb-4">Manage borrower information, account status, and contact details from one place.</p>
            <form method="post" action="${pageContext.request.contextPath}/admin/students/${student.studentId}/update" class="row g-3">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">

                <div class="col-md-6">
                    <label class="form-label" for="name">Full name</label>
                    <input class="form-control" id="name" name="name" value="${student.user.name}" required>
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="email">Email address</label>
                    <input class="form-control" id="email" name="email" type="email" value="${student.user.email}" required>
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="course">Program</label>
                    <select class="form-select" id="course" name="course">
                        <option value="">Select program</option>
                        <c:if test="${not empty student.course and student.course != 'Not set' and !programOptionLookup[student.course]}">
                            <option value="${student.course}" selected>${student.course}</option>
                        </c:if>
                        <c:forEach items="${programOptionsByCollege}" var="collegeEntry">
                            <optgroup label="${collegeEntry.key}">
                                <c:forEach items="${collegeEntry.value}" var="programOption">
                                    <option value="${programOption}" <c:if test="${student.course == programOption}">selected</c:if>>${programOption}</option>
                                </c:forEach>
                            </optgroup>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="yearLevel">Year level</label>
                    <select class="form-select" id="yearLevel" name="yearLevel">
                        <option value="">Select year level</option>
                        <c:if test="${not empty student.yearLevel and student.yearLevel != 'Not set' and !yearLevelOptionLookup[student.yearLevel]}">
                            <option value="${student.yearLevel}" selected>${student.yearLevel}</option>
                        </c:if>
                        <c:forEach items="${yearLevelOptions}" var="yearLevelOption">
                            <option value="${yearLevelOption}" <c:if test="${student.yearLevel == yearLevelOption}">selected</c:if>>${yearLevelOption}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="phone">Phone</label>
                    <input class="form-control" id="phone" name="phone" value="${student.phone}">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="dateOfBirth">Birth date</label>
                    <input class="form-control" id="dateOfBirth" name="dateOfBirth" type="date" value="${student.dateOfBirth}">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="status">Account status</label>
                    <select class="form-select" id="status" name="status">
                        <c:forEach items="${userStatuses}" var="status">
                            <option value="${status}" <c:if test="${status == student.user.status}">selected</c:if>>${status}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="province">Province</label>
                    <select class="form-select" id="province" name="province">
                        <option value="Laguna" <c:if test="${empty studentAddressProvinceValue or studentAddressProvinceValue == 'Laguna'}">selected</c:if>>Laguna</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="cityMunicipality">City / Municipality</label>
                    <select class="form-select" id="cityMunicipality" name="cityMunicipality">
                        <option value="">Select city / municipality</option>
                        <c:forEach items="${registrationCityZipCodes}" var="entry">
                            <option value="${entry.key}" <c:if test="${studentAddressCityMunicipalityValue == entry.key}">selected</c:if>>${entry.key}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="barangay">Barangay</label>
                    <select class="form-select"
                            id="barangay"
                            name="barangay"
                            data-selected-barangay="<c:out value='${studentAddressBarangayValue}'/>"
                            <c:if test="${empty studentAddressCityMunicipalityValue}">disabled</c:if>>
                        <option value="">
                            <c:choose>
                                <c:when test="${not empty studentAddressCityMunicipalityValue and not empty studentAddressBarangayValue}">${studentAddressBarangayValue}</c:when>
                                <c:when test="${not empty studentAddressCityMunicipalityValue}">Loading barangays...</c:when>
                                <c:otherwise>Select city / municipality first</c:otherwise>
                            </c:choose>
                        </option>
                        <c:if test="${not empty studentAddressBarangayValue}">
                            <option value="${studentAddressBarangayValue}" selected>${studentAddressBarangayValue}</option>
                        </c:if>
                    </select>
                </div>
                <div class="col-md-8">
                    <label class="form-label" for="street">Street / House No.</label>
                    <input class="form-control" id="street" name="street" value="${studentAddressStreetValue}">
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="zipcode">Zip code</label>
                    <input class="form-control" id="zipcode" name="zipcode" value="${studentAddressZipcodeValue}" readonly>
                </div>
                <div class="col-12 d-flex flex-wrap gap-2">
                    <button class="btn btn-brand" type="submit">Save student details</button>
                    <a class="btn btn-warm" href="${pageContext.request.contextPath}/admin/students">Back to directory</a>
                </div>
            </form>
        </div>

        <div class="panel-stack">
            <div class="panel-card">
                <div class="section-title">Reset portal password</div>
                <form method="post" action="${pageContext.request.contextPath}/admin/students/${student.studentId}/password" class="row g-3">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">

                    <div class="col-12">
                        <label class="form-label" for="newPassword">New password</label>
                        <input class="form-control" id="newPassword" name="newPassword" type="password" minlength="12" required>
                    </div>
                    <div class="col-12">
                        <label class="form-label" for="confirmPassword">Confirm password</label>
                        <input class="form-control" id="confirmPassword" name="confirmPassword" type="password" minlength="12" required>
                    </div>
                    <div class="col-12">
                        <button class="btn btn-warm" type="submit">Reset password</button>
                    </div>
                </form>
            </div>

            <div class="panel-card danger-card">
                <c:choose>
                    <c:when test="${student.user.status.name() == 'ARCHIVED'}">
                        <div class="section-title">Archived account actions</div>
                        <p class="helper-copy mb-3">
                            Restore the borrower account back to the active directory, or permanently delete it if the archive should be final.
                        </p>
                        <div class="d-flex flex-wrap gap-2">
                            <form method="post" action="${pageContext.request.contextPath}/admin/students/${student.studentId}/restore">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                <button class="btn btn-warm" type="submit">Restore student account</button>
                            </form>
                            <form method="post" action="${pageContext.request.contextPath}/admin/students/${student.studentId}/delete">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                <input type="hidden" name="view" value="${studentView}">
                                <button class="btn btn-danger" type="submit">Delete account permanently</button>
                            </form>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="section-title">Archive account</div>
                        <p class="helper-copy mb-3">
                            Archive this borrower instead of deleting right away. This only works when the student has no active issued or overdue books linked to the borrower.
                        </p>
                        <form method="post" action="${pageContext.request.contextPath}/admin/students/${student.studentId}/archive">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button class="btn btn-danger" type="submit">Archive student account</button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </section>

    <section class="detail-grid mb-4">
        <div class="panel-card">
            <div class="section-title">Active borrowed books</div>
            <div class="table-responsive">
                <table class="table align-middle">
                    <thead>
                    <tr>
                        <th>Book</th>
                        <th>Issue code</th>
                        <th>Issue date</th>
                        <th>Due date</th>
                        <th>Status</th>
                        <th>Fine</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${activeIssues}" var="issue">
                        <tr>
                            <td>${issue.book.title}</td>
                            <td>${issue.qrIssueCode}</td>
                            <td>${issue.issueDateDisplay}</td>
                            <td>${issue.dueDateDisplay}</td>
                            <td><span class="tag-chip">${issue.status}</span></td>
                            <td>${issue.fineAmount}</td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty activeIssues}">
                        <tr>
                            <td colspan="6" class="text-center muted-text">This student has no active borrowed books right now.</td>
                        </tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="panel-card">
            <div class="section-title">Admin notes</div>
            <div class="support-list">
                <div class="support-item">
                    <strong>Use this page before issuing more books</strong>
                    <span>Check active loans and overdue items first so circulation decisions stay consistent and fair.</span>
                </div>
                <div class="support-item">
                    <strong>Review fine exposure quickly</strong>
                    <span>The accumulated fine amount helps the desk identify students who may need account follow-up before new borrowing.</span>
                </div>
                <div class="support-item">
                    <strong>Track reading and borrowing behavior</strong>
                    <span>Borrowing history gives admins a clearer view of student activity and recurring catalog demand.</span>
                </div>
                <div class="support-item">
                    <strong>Use inactive status for temporary restrictions</strong>
                    <span>When a borrower should not log in temporarily, set the account to inactive instead of deleting the record immediately.</span>
                </div>
            </div>
        </div>
    </section>

    <section class="panel-card">
        <div class="section-title">Complete borrowing history</div>
        <div class="table-responsive">
            <table class="table align-middle">
                <thead>
                <tr>
                    <th>Book</th>
                    <th>Issue code</th>
                    <th>Issued by</th>
                    <th>Issue date</th>
                    <th>Due date</th>
                    <th>Return date</th>
                    <th>Status</th>
                    <th>Fine</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${issueRecords}" var="issue">
                    <tr>
                        <td>${issue.book.title}</td>
                        <td>${issue.qrIssueCode}</td>
                        <td>${issue.issuedBy.name}</td>
                        <td>${issue.issueDateDisplay}</td>
                        <td>${issue.dueDateDisplay}</td>
                        <td>${issue.returnDateDisplay}</td>
                        <td><span class="tag-chip">${issue.status}</span></td>
                        <td>${issue.fineAmount}</td>
                    </tr>
                </c:forEach>
                <c:if test="${empty issueRecords}">
                    <tr>
                        <td colspan="8" class="text-center muted-text">No borrowing history available for this student yet.</td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </section>
</div>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
<script>
    (function () {
        if (!window.LatteAndLettersAddress) {
            return;
        }

        window.LatteAndLettersAddress.initForm({
            cityMunicipality: document.getElementById("cityMunicipality"),
            barangay: document.getElementById("barangay"),
            zipcode: document.getElementById("zipcode"),
            endpoint: "${pageContext.request.contextPath}/register/barangays",
            cityZipCodes: {
                <c:forEach items="${registrationCityZipCodes}" var="entry" varStatus="status">
                "${entry.key}": "${entry.value}"<c:if test="${!status.last}">,</c:if>
                </c:forEach>
            }
        });
    })();
</script>
</body>
</html>


