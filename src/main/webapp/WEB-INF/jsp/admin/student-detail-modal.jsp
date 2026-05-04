<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<div class="modal-header modal-header-brand">
    <div>
        <div class="modal-kicker">Student Account</div>
        <h2 class="modal-title h4 mb-1" id="studentDetailModalLabel">${student.user.name}</h2>
        <p class="modal-subtitle mb-0">Student ID: ${student.studentId} | ${student.user.email}</p>
    </div>
    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
</div>

<div class="modal-body">
    <section class="modal-section">
        <div class="modal-stat-grid">
            <div class="modal-stat-card">
                <span class="modal-stat-label">Program</span>
                <strong class="modal-stat-value">${student.course}</strong>
            </div>
            <div class="modal-stat-card">
                <span class="modal-stat-label">Year Level</span>
                <strong class="modal-stat-value">${student.yearLevel}</strong>
            </div>
            <div class="modal-stat-card">
                <span class="modal-stat-label">Phone</span>
                <strong class="modal-stat-value">${student.phone}</strong>
            </div>
            <div class="modal-stat-card">
                <span class="modal-stat-label">Status</span>
                <strong class="modal-stat-value">${student.user.status}</strong>
            </div>
            <div class="modal-stat-card">
                <span class="modal-stat-label">Active Loans</span>
                <strong class="modal-stat-value">${activeCount}</strong>
            </div>
            <div class="modal-stat-card">
                <span class="modal-stat-label">Overdue Items</span>
                <strong class="modal-stat-value">${overdueItems}</strong>
            </div>
            <div class="modal-stat-card">
                <span class="modal-stat-label">Borrow History</span>
                <strong class="modal-stat-value">${historyCount}</strong>
            </div>
            <div class="modal-stat-card">
                <span class="modal-stat-label">Total Fines</span>
                <strong class="modal-stat-value">${totalFineAmount}</strong>
            </div>
        </div>
    </section>

    <section class="modal-section modal-panel-grid">
        <div class="modal-card">
            <div class="section-title">Borrower standing</div>
            <div class="support-item">
                <strong>${borrowerStanding.statusLabel}</strong>
                <span>
                    Active loans: ${borrowerStanding.activeLoansCount}/${borrowerStanding.maxActiveLoans}
                    | Remaining slots: ${borrowerStanding.remainingLoanSlots}
                    | Unpaid fines: ${borrowerStanding.outstandingFineAmount}
                </span>
            </div>
            <c:if test="${borrowerStanding.blocked}">
                <div class="support-list mt-3">
                    <c:forEach items="${borrowerStanding.blockers}" var="blocker">
                        <div class="support-item">
                            <strong>Borrowing blocker</strong>
                            <span>${blocker}</span>
                        </div>
                    </c:forEach>
                </div>
            </c:if>
        </div>

        <div class="modal-card">
            <div class="section-title">Fine ledger summary</div>
            <div class="modal-stat-grid">
                <div class="modal-stat-card">
                    <span class="modal-stat-label">Outstanding Amount</span>
                    <strong class="modal-stat-value">${borrowerStanding.outstandingFineAmount}</strong>
                </div>
                <div class="modal-stat-card">
                    <span class="modal-stat-label">Unpaid Fine Records</span>
                    <strong class="modal-stat-value">${borrowerStanding.unpaidFineCount}</strong>
                </div>
                <div class="modal-stat-card">
                    <span class="modal-stat-label">Overdue Items</span>
                    <strong class="modal-stat-value">${borrowerStanding.overdueCount}</strong>
                </div>
                <div class="modal-stat-card">
                    <span class="modal-stat-label">Account Access</span>
                    <strong class="modal-stat-value">${student.user.status}</strong>
                </div>
            </div>
        </div>
    </section>

    <section class="modal-section">
        <div class="section-title">Update Student Account</div>
        <form method="post" action="${pageContext.request.contextPath}/admin/students/${student.studentId}/update" class="row g-3">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">

            <div class="col-md-6">
                <label class="form-label" for="modalName">Full name</label>
                <input class="form-control" id="modalName" name="name" value="${student.user.name}" required>
            </div>
            <div class="col-md-6">
                <label class="form-label" for="modalEmail">Email address</label>
                <input class="form-control" id="modalEmail" name="email" type="email" value="${student.user.email}" required>
            </div>
            <div class="col-md-4">
                <label class="form-label" for="modalCourse">Program</label>
                <select class="form-select" id="modalCourse" name="course">
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
                <label class="form-label" for="modalYearLevel">Year level</label>
                <select class="form-select" id="modalYearLevel" name="yearLevel">
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
                <label class="form-label" for="modalPhone">Phone</label>
                <input class="form-control" id="modalPhone" name="phone" value="${student.phone}">
            </div>
            <div class="col-md-6">
                <label class="form-label" for="modalBirthDate">Birth date</label>
                <input class="form-control" id="modalBirthDate" name="dateOfBirth" type="date" value="${student.dateOfBirth}">
            </div>
            <div class="col-md-6">
                <label class="form-label" for="modalStatus">Account status</label>
                <select class="form-select" id="modalStatus" name="status">
                    <c:forEach items="${userStatuses}" var="status">
                        <option value="${status}" <c:if test="${status == student.user.status}">selected</c:if>>${status}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-4">
                <label class="form-label" for="modalProvince">Province</label>
                <select class="form-select" id="modalProvince" name="province">
                    <option value="Laguna" <c:if test="${empty studentAddressProvinceValue or studentAddressProvinceValue == 'Laguna'}">selected</c:if>>Laguna</option>
                </select>
            </div>
            <div class="col-md-4">
                <label class="form-label" for="modalCityMunicipality">City / Municipality</label>
                <select class="form-select" id="modalCityMunicipality" name="cityMunicipality">
                    <option value="">Select city / municipality</option>
                    <c:forEach items="${registrationCityZipCodes}" var="entry">
                        <option value="${entry.key}" <c:if test="${studentAddressCityMunicipalityValue == entry.key}">selected</c:if>>${entry.key}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-4">
                <label class="form-label" for="modalBarangay">Barangay</label>
                <select class="form-select"
                        id="modalBarangay"
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
                <label class="form-label" for="modalStreet">Street / House No.</label>
                <input class="form-control" id="modalStreet" name="street" value="${studentAddressStreetValue}">
            </div>
            <div class="col-md-4">
                <label class="form-label" for="modalZipcode">Zip code</label>
                <input class="form-control" id="modalZipcode" name="zipcode" value="${studentAddressZipcodeValue}" readonly>
            </div>
            <div class="col-12 d-flex flex-wrap gap-2">
                <button class="btn btn-brand" type="submit">Save changes</button>
            </div>
        </form>
    </section>

    <section class="modal-section modal-panel-grid">
        <div class="modal-card">
            <div class="section-title">Reset Password</div>
            <form method="post" action="${pageContext.request.contextPath}/admin/students/${student.studentId}/password" class="row g-3">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">

                <div class="col-12">
                    <label class="form-label" for="modalNewPassword">New password</label>
                    <input class="form-control" id="modalNewPassword" name="newPassword" type="password" minlength="12" required>
                </div>
                <div class="col-12">
                    <label class="form-label" for="modalConfirmPassword">Confirm password</label>
                    <input class="form-control" id="modalConfirmPassword" name="confirmPassword" type="password" minlength="12" required>
                </div>
                <div class="col-12">
                    <button class="btn btn-warm" type="submit">Reset password</button>
                </div>
            </form>
        </div>

        <div class="modal-card danger-card">
            <div class="section-title">Delete Account</div>
            <p class="helper-copy mb-3">
                This only works when the student has no active issued or overdue books.
            </p>
            <form method="post" action="${pageContext.request.contextPath}/admin/students/${student.studentId}/delete">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <button class="btn btn-danger" type="submit">Delete student account</button>
            </form>
        </div>
    </section>

    <section class="modal-section">
        <div class="section-title">Active Borrowed Books</div>
        <div class="table-responsive">
            <table class="table align-middle modal-table">
                <thead>
                <tr>
                    <th>Book</th>
                    <th>Issue Code</th>
                    <th>Issue Date</th>
                    <th>Due Date</th>
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
                        <td colspan="6" class="text-center muted-text">No active borrowed books for this student.</td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </section>

    <section class="modal-section">
        <div class="section-title">Fine ledger</div>
        <div class="table-responsive">
            <table class="table align-middle modal-table">
                <thead>
                <tr>
                    <th>Issue Code</th>
                    <th>Book</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Calculated</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${studentFines}" var="fine">
                    <tr>
                        <td>${fine.issueRecord.qrIssueCode}</td>
                        <td>${fine.issueRecord.book.title}</td>
                        <td>${fine.amount}</td>
                        <td><span class="tag-chip">${fine.status}</span></td>
                        <td>${fine.calculatedAtDisplay}</td>
                    </tr>
                </c:forEach>
                <c:if test="${empty studentFines}">
                    <tr>
                        <td colspan="5" class="text-center muted-text">No fine ledger entries for this borrower yet.</td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </section>

    <section class="modal-section">
        <div class="section-title">Borrowing History</div>
        <div class="table-responsive">
            <table class="table align-middle modal-table">
                <thead>
                <tr>
                    <th>Book</th>
                    <th>Issue Code</th>
                    <th>Issued By</th>
                    <th>Issue Date</th>
                    <th>Due Date</th>
                    <th>Return Date</th>
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
                        <td colspan="8" class="text-center muted-text">No borrowing history available yet.</td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </section>
</div>
