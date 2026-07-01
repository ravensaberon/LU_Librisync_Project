<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Student Profile</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Student Portal</span>
            <div class="brand-title mt-2">My profile</div>
            <p class="muted-text mt-2 mb-0">Review and update your student details.</p>
        </div>
        <div class="nav-links">
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/dashboard">Dashboard</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/catalog">Catalog</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/reservations">Pickup requests</a>
            <a class="nav-pill active" href="${pageContext.request.contextPath}/student/profile">Profile</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/history">Borrowed books</a>
            <form method="post" action="${pageContext.request.contextPath}/logout">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <button class="nav-pill warm border-0" type="submit" aria-label="Logout" title="Logout"><span class="nav-pill-icon"><i class="bi bi-power" aria-hidden="true"></i></span><span class="nav-pill-label">Logout</span></button>
            </form>
        </div>
    </div>

    <c:if test="${not empty success}">
        <div class="alert alert-success">${success}</div>
    </c:if>
    <c:if test="${not empty info}">
        <div class="alert alert-info">${info}</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-danger">${error}</div>
    </c:if>

    <section class="hero-card profile-hero-card mb-4">
        <div class="profile-hero-grid">
            <div class="profile-identity">
                <div class="profile-avatar-shell">
                    <c:choose>
                        <c:when test="${hasProfileImage}">
                            <img class="profile-avatar-image"
                                 src="${pageContext.request.contextPath}/student/profile/avatar?v=${profileImageVersion}"
                                 alt="Profile picture of ${student.user.name}">
                        </c:when>
                        <c:otherwise>
                            <div class="profile-avatar-badge">${studentInitials}</div>
                        </c:otherwise>
                    </c:choose>
                </div>
                <div>
                    <span class="tag-chip">Student Information</span>
                    <h1 class="profile-hero-title">${student.user.name}</h1>
                    <p class="muted-text mb-2">
                        ${empty student.course ? 'Not set' : student.course}
                        <span class="profile-dot-separator"></span>
                        ${empty student.yearLevel ? 'Not set' : student.yearLevel}
                    </p>
                    <div class="profile-meta-strip">
                        <span><i class="bi bi-person-vcard"></i> ${student.studentId}</span>
                        <span><i class="bi bi-envelope"></i> ${student.user.email}</span>
                        <span><i class="bi bi-shield-check"></i> ${student.user.status}</span>
                    </div>
                </div>
            </div>

            <div class="profile-hero-actions">
                <button class="btn btn-brand" type="button" data-bs-toggle="modal" data-bs-target="#editProfileModal">
                    <i class="bi bi-pencil-square"></i>
                    Edit profile
                </button>
                <c:if test="${hasPendingProfileOtp}">
                    <button class="btn btn-warm" type="button" data-bs-toggle="modal" data-bs-target="#verifyProfileOtpModal">
                        <i class="bi bi-shield-lock"></i>
                        Enter OTP
                    </button>
                </c:if>
            </div>
        </div>
    </section>

    <section class="panel-card profile-summary-card mb-4">
        <div class="profile-summary-header">
            <div>
                <div class="section-title mb-1">Profile overview</div>
                <p class="muted-text mb-0">A cleaner view of your current library account details.</p>
            </div>
            <span class="tag-chip">${student.user.status}</span>
        </div>

        <div class="profile-highlight-grid">
            <div class="profile-highlight-tile">
                <span class="profile-highlight-label">Program</span>
                <strong>${empty student.course ? 'Not set' : student.course}</strong>
            </div>
            <div class="profile-highlight-tile">
                <span class="profile-highlight-label">Year level</span>
                <strong>${empty student.yearLevel ? 'Not set' : student.yearLevel}</strong>
            </div>
            <div class="profile-highlight-tile">
                <span class="profile-highlight-label">Phone</span>
                <strong>${empty student.phone ? 'Not provided' : student.phone}</strong>
            </div>
            <div class="profile-highlight-tile">
                <span class="profile-highlight-label">Birthday</span>
                <strong>${empty student.dateOfBirth ? 'Not provided' : student.dateOfBirthDisplay}</strong>
            </div>
        </div>
    </section>

    <section class="panel-grid panel-grid-equal mb-4">
        <article class="panel-card">
            <div class="section-title">Borrowing standing</div>
            <div class="support-item">
                <strong>${borrowerStanding.statusLabel}</strong>
                <span>
                    Active loans: ${borrowerStanding.activeLoansCount}/${borrowerStanding.maxActiveLoans}
                    | Remaining slots: ${borrowerStanding.remainingLoanSlots}
                    | Outstanding fines: ${borrowerStanding.outstandingFineAmount}
                </span>
            </div>
            <c:if test="${borrowerStanding.blocked}">
                <div class="support-list mt-3">
                    <c:forEach items="${borrowerStanding.blockers}" var="blocker">
                        <div class="support-item">
                            <strong>Account hold</strong>
                            <span>${blocker}</span>
                        </div>
                    </c:forEach>
                </div>
            </c:if>
        </article>

        <article class="panel-card">
            <div class="section-title">Fine ledger summary</div>
            <div class="profile-highlight-grid">
                <div class="profile-highlight-tile">
                    <span class="profile-highlight-label">Outstanding amount</span>
                    <strong>${outstandingFineTotal}</strong>
                </div>
                <div class="profile-highlight-tile">
                    <span class="profile-highlight-label">Unpaid fine records</span>
                    <strong>${borrowerStanding.unpaidFineCount}</strong>
                </div>
            </div>
            <ul class="list-clean mt-3">
                <c:forEach items="${studentFines}" var="fine" end="4">
                    <li class="d-flex justify-content-between align-items-center">
                        <span>${fine.issueRecord.book.title}</span>
                        <span class="tag-chip">${fine.amount} | ${fine.status}</span>
                    </li>
                </c:forEach>
                <c:if test="${empty studentFines}">
                    <li class="muted-text">No fine ledger entries have been recorded for this account yet.</li>
                </c:if>
            </ul>
        </article>
    </section>

    <section class="panel-card profile-directory-card">
        <div class="profile-section-block">
            <div class="profile-section-heading">
                <span class="profile-section-icon"><i class="bi bi-person-badge"></i></span>
                <div>
                    <div class="section-title mb-1">Personal details</div>
                    <p class="muted-text mb-0">Your core student identity inside Latte and Letters.</p>
                </div>
            </div>
            <div class="profile-detail-grid">
                <div class="profile-detail-item">
                    <span class="profile-detail-label">Full name</span>
                    <strong class="profile-detail-value">${student.user.name}</strong>
                </div>
                <div class="profile-detail-item">
                    <span class="profile-detail-label">Student ID</span>
                    <strong class="profile-detail-value">${student.studentId}</strong>
                </div>
                <div class="profile-detail-item">
                    <span class="profile-detail-label">Date of birth</span>
                    <strong class="profile-detail-value">${empty student.dateOfBirth ? 'Not provided' : student.dateOfBirthDisplay}</strong>
                </div>
                <div class="profile-detail-item">
                    <span class="profile-detail-label">Account status</span>
                    <strong class="profile-detail-value">${student.user.status}</strong>
                </div>
            </div>
        </div>

        <div class="profile-section-block">
            <div class="profile-section-heading">
                <span class="profile-section-icon"><i class="bi bi-telephone"></i></span>
                <div>
                    <div class="section-title mb-1">Contact details</div>
                    <p class="muted-text mb-0">Your registered communication and home address information.</p>
                </div>
            </div>
            <div class="profile-detail-grid">
                <div class="profile-detail-item">
                    <span class="profile-detail-label">Email address</span>
                    <strong class="profile-detail-value">${student.user.email}</strong>
                </div>
                <div class="profile-detail-item">
                    <span class="profile-detail-label">Phone number</span>
                    <strong class="profile-detail-value">${empty student.phone ? 'Not provided' : student.phone}</strong>
                </div>
                <div class="profile-detail-item profile-detail-item-wide">
                    <span class="profile-detail-label">Address</span>
                    <strong class="profile-detail-value">${empty student.address ? 'Not provided' : student.address}</strong>
                </div>
            </div>
        </div>

        <div class="profile-section-block">
            <div class="profile-section-heading">
                <span class="profile-section-icon"><i class="bi bi-mortarboard"></i></span>
                <div>
                    <div class="section-title mb-1">Academic details</div>
                    <p class="muted-text mb-0">Your current program and student record details.</p>
                </div>
            </div>
            <div class="profile-detail-grid">
                <div class="profile-detail-item">
                    <span class="profile-detail-label">Program</span>
                    <strong class="profile-detail-value">${empty student.course ? 'Not set' : student.course}</strong>
                </div>
                <div class="profile-detail-item">
                    <span class="profile-detail-label">Year level</span>
                    <strong class="profile-detail-value">${empty student.yearLevel ? 'Not set' : student.yearLevel}</strong>
                </div>
                <div class="profile-detail-item">
                    <span class="profile-detail-label">Created at</span>
                    <strong class="profile-detail-value">${student.createdAtDisplay}</strong>
                </div>
                <div class="profile-detail-item">
                    <span class="profile-detail-label">Last updated</span>
                    <strong class="profile-detail-value">${student.updatedAt}</strong>
                </div>
            </div>
        </div>
    </section>
</div>

<div class="modal fade" id="editProfileModal" tabindex="-1" aria-labelledby="editProfileModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header modal-header-brand">
                <div>
                    <div class="modal-kicker">Profile Update</div>
                    <h2 class="modal-title h4 mb-1" id="editProfileModalLabel">Edit your profile</h2>
                </div>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body pb-0">
                <div class="profile-avatar-editor">
                    <div class="profile-avatar-editor-preview">
                        <div class="profile-avatar-shell profile-avatar-shell-modal">
                            <c:choose>
                                <c:when test="${hasProfileImage}">
                                    <img class="profile-avatar-image"
                                         src="${pageContext.request.contextPath}/student/profile/avatar?v=${profileImageVersion}"
                                         alt="Profile picture of ${student.user.name}">
                                </c:when>
                                <c:otherwise>
                                    <div class="profile-avatar-badge">${studentInitials}</div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div>
                            <strong class="d-block">Profile picture</strong>
                            <p class="muted-text mb-0">Upload a JPG, PNG, or WEBP image up to 5 MB.</p>
                        </div>
                    </div>
                    <div class="profile-avatar-editor-actions">
                        <form id="profileImageUploadForm"
                              method="post"
                              action="${pageContext.request.contextPath}/student/profile/avatar"
                              enctype="multipart/form-data">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <input class="d-none" id="profileImageInput" name="profileImage" type="file" accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp">
                            <button class="btn btn-warm" type="button" id="profileImageTrigger">
                                <i class="bi bi-camera"></i>
                                Change picture
                            </button>
                        </form>
                        <c:if test="${hasProfileImage}">
                            <form method="post" action="${pageContext.request.contextPath}/student/profile/avatar/remove">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                <button class="btn btn-warm" type="submit">
                                    <i class="bi bi-trash3"></i>
                                    Remove picture
                                </button>
                            </form>
                        </c:if>
                    </div>
                </div>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/student/profile/request-otp">
                <div class="modal-body">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label" for="firstName">First name</label>
                            <input class="form-control" id="firstName" name="firstName" value="${profileForm.firstName}" autocapitalize="words" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="middleName">Middle name</label>
                            <input class="form-control" id="middleName" name="middleName" value="${profileForm.middleName}" autocapitalize="words">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="lastName">Last name</label>
                            <input class="form-control" id="lastName" name="lastName" value="${profileForm.lastName}" autocapitalize="words" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="suffix">Suffix</label>
                            <input class="form-control" id="suffix" name="suffix" value="${profileForm.suffix}" autocapitalize="characters">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="studentIdDisplay">Student ID</label>
                            <input class="form-control" id="studentIdDisplay" value="${student.studentId}" disabled>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="course">Program</label>
                            <select class="form-select" id="course" name="course">
                                <option value="">Select program</option>
                                <c:if test="${not empty profileForm.course and profileForm.course != 'Not set' and !programOptionLookup[profileForm.course]}">
                                    <option value="${profileForm.course}" selected>${profileForm.course}</option>
                                </c:if>
                                <c:forEach items="${programOptionsByCollege}" var="collegeEntry">
                                    <optgroup label="${collegeEntry.key}">
                                        <c:forEach items="${collegeEntry.value}" var="programOption">
                                            <option value="${programOption}" <c:if test="${profileForm.course == programOption}">selected</c:if>>${programOption}</option>
                                        </c:forEach>
                                    </optgroup>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="yearLevel">Year level</label>
                            <select class="form-select" id="yearLevel" name="yearLevel">
                                <option value="">Select year level</option>
                                <c:if test="${not empty profileForm.yearLevel and profileForm.yearLevel != 'Not set' and !yearLevelOptionLookup[profileForm.yearLevel]}">
                                    <option value="${profileForm.yearLevel}" selected>${profileForm.yearLevel}</option>
                                </c:if>
                                <c:forEach items="${yearLevelOptions}" var="yearLevelOption">
                                    <option value="${yearLevelOption}" <c:if test="${profileForm.yearLevel == yearLevelOption}">selected</c:if>>${yearLevelOption}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="phone">Phone</label>
                            <input class="form-control" id="phone" name="phone" value="${profileForm.phone}">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="dateOfBirth">Date of birth</label>
                            <input class="form-control" id="dateOfBirth" name="dateOfBirth" type="date" value="${profileForm.dateOfBirth}">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="profileProvince">Province</label>
                            <select class="form-select" id="profileProvince" name="province">
                                <option value="Laguna" <c:if test="${empty profileProvinceValue or profileProvinceValue == 'Laguna'}">selected</c:if>>Laguna</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="profileCityMunicipality">City / Municipality</label>
                            <select class="form-select" id="profileCityMunicipality" name="cityMunicipality">
                                <option value="">Select city / municipality</option>
                                <c:forEach items="${registrationCityZipCodes}" var="entry">
                                    <option value="${entry.key}" <c:if test="${profileCityMunicipalityValue == entry.key}">selected</c:if>>${entry.key}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="profileBarangay">Barangay</label>
                            <select class="form-select"
                                    id="profileBarangay"
                                    name="barangay"
                                    data-selected-barangay="<c:out value='${profileBarangayValue}'/>"
                                    <c:if test="${empty profileCityMunicipalityValue}">disabled</c:if>>
                                <option value="">
                                    <c:choose>
                                        <c:when test="${not empty profileCityMunicipalityValue and not empty profileBarangayValue}">${profileBarangayValue}</c:when>
                                        <c:when test="${not empty profileCityMunicipalityValue}">Loading barangays...</c:when>
                                        <c:otherwise>Select city / municipality first</c:otherwise>
                                    </c:choose>
                                </option>
                                <c:if test="${not empty profileBarangayValue}">
                                    <option value="${profileBarangayValue}" selected>${profileBarangayValue}</option>
                                </c:if>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="profileStreet">Street / House No.</label>
                            <input class="form-control" id="profileStreet" name="street" value="${profileStreetValue}">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="profileZipcode">Zip code</label>
                            <input class="form-control" id="profileZipcode" name="zipcode" value="${profileZipcodeValue}" readonly>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-warm" type="button" data-bs-dismiss="modal">Cancel</button>
                    <button class="btn btn-brand" type="submit">
                        <i class="bi bi-shield-lock"></i>
                        Request OTP
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<div class="modal fade" id="verifyProfileOtpModal" tabindex="-1" aria-labelledby="verifyProfileOtpModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header modal-header-brand">
                <div>
                    <div class="modal-kicker">OTP Verification</div>
                    <h2 class="modal-title h4 mb-1" id="verifyProfileOtpModalLabel">Confirm profile update</h2>
                </div>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/student/profile/verify-otp">
                <div class="modal-body">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <div class="otp-panel">
                        <div class="otp-panel-icon"><i class="bi bi-envelope-paper"></i></div>
                        <div>
                            <strong>${empty otpMaskedEmail ? 'Registered email' : otpMaskedEmail}</strong>
                            <div class="small muted-text">
                                Resend OTP in <strong id="modalOtpResendCountdown">calculating...</strong>
                            </div>
                        </div>
                    </div>

                    <div class="mt-3">
                        <label class="form-label" for="otpCode">One-time passcode</label>
                        <input class="form-control form-control-lg otp-input" id="otpCode" name="otpCode" maxlength="6" inputmode="numeric" pattern="[0-9]{6}" placeholder="Enter 6-digit OTP" required>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-warm me-auto" type="submit" form="resendProfileOtpForm" id="resendOtpButton">
                        <i class="bi bi-arrow-repeat"></i>
                        Resend OTP
                    </button>
                    <button class="btn btn-warm" type="button" data-bs-dismiss="modal">Close</button>
                    <button class="btn btn-brand" type="submit">
                        <i class="bi bi-check2-circle"></i>
                        Verify and save
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>
<form id="resendProfileOtpForm" method="post" action="${pageContext.request.contextPath}/student/profile/resend-otp" class="d-none">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
</form>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
<script>
    (function () {
        var shouldOpenEditModal = ${openEditModal ? 'true' : 'false'};
        var shouldOpenOtpModal = ${openOtpModal ? 'true' : 'false'};
        var otpExpiresAtEpochMs = ${empty otpExpiresAtEpochMs ? 'null' : otpExpiresAtEpochMs};
        var otpResendAvailableAtEpochMs = ${empty otpResendAvailableAtEpochMs ? 'null' : otpResendAvailableAtEpochMs};
        var profileImageInput = document.getElementById("profileImageInput");
        var profileImageTrigger = document.getElementById("profileImageTrigger");

        var editProfileModal = document.getElementById("editProfileModal");
        var verifyProfileOtpModal = document.getElementById("verifyProfileOtpModal");
        var resendOtpButton = document.getElementById("resendOtpButton");
        var profileCityMunicipality = document.getElementById("profileCityMunicipality");
        var profileBarangay = document.getElementById("profileBarangay");
        var profileZipcode = document.getElementById("profileZipcode");
        var profileStreet = document.getElementById("profileStreet");
        var profileNameFields = [
            document.getElementById("firstName"),
            document.getElementById("middleName"),
            document.getElementById("lastName"),
            document.getElementById("suffix")
        ];

        function toWordCase(value) {
            return (value || "")
                .replace(/\s+/g, " ")
                .replace(/(^|\s|[-'])(([a-z]))/g, function (match, prefix, letterGroup, letter) {
                    return prefix + letter.toUpperCase();
                });
        }

        function toAddressCase(value) {
            return (value || "")
                .replace(/\s+/g, " ")
                .replace(/(^|\s|[-/'])(([a-z]))/g, function (match, prefix, letterGroup, letter) {
                    return prefix + letter.toUpperCase();
                });
        }

        function applyNameCase(input) {
            if (!input) {
                return;
            }

            var selectionStart = input.selectionStart;
            var selectionEnd = input.selectionEnd;
            var formattedValue = toWordCase(input.value);
            if (formattedValue === input.value) {
                return;
            }

            input.value = formattedValue;
            if (typeof selectionStart === "number" && typeof selectionEnd === "number") {
                input.setSelectionRange(selectionStart, selectionEnd);
            }
        }

        function applyFormattedCase(input, formatter) {
            if (!input || typeof formatter !== "function") {
                return;
            }

            var selectionStart = input.selectionStart;
            var selectionEnd = input.selectionEnd;
            var formattedValue = formatter(input.value);
            if (formattedValue === input.value) {
                return;
            }

            input.value = formattedValue;
            if (typeof selectionStart === "number" && typeof selectionEnd === "number") {
                input.setSelectionRange(selectionStart, selectionEnd);
            }
        }

        function formatCountdown(targetEpochMs) {
            if (!targetEpochMs) {
                return "not active";
            }

            var remainingMs = targetEpochMs - Date.now();
            if (remainingMs <= 0) {
                return "00:00";
            }

            var totalSeconds = Math.floor(remainingMs / 1000);
            var minutes = Math.floor(totalSeconds / 60);
            var seconds = totalSeconds % 60;
            return String(minutes).padStart(2, "0") + ":" + String(seconds).padStart(2, "0");
        }

        function updateOtpCountdowns() {
            var resendTargets = document.querySelectorAll("#profileOtpResendCountdown, #modalOtpResendCountdown");

            var resendCountdown = formatCountdown(otpResendAvailableAtEpochMs);
            resendTargets.forEach(function (element) {
                if (element) {
                    element.textContent = resendCountdown;
                }
            });

            if (resendOtpButton) {
                var resendBlocked = otpResendAvailableAtEpochMs && otpResendAvailableAtEpochMs > Date.now();
                resendOtpButton.disabled = resendBlocked;
            }
        }

        if (shouldOpenOtpModal && verifyProfileOtpModal) {
            bootstrap.Modal.getOrCreateInstance(verifyProfileOtpModal).show();
        }

        if (shouldOpenEditModal && editProfileModal) {
            bootstrap.Modal.getOrCreateInstance(editProfileModal).show();
        }

        if (profileImageTrigger && profileImageInput) {
            profileImageTrigger.addEventListener("click", function () {
                profileImageInput.click();
            });

            profileImageInput.addEventListener("change", function () {
                if (profileImageInput.files && profileImageInput.files.length > 0) {
                    profileImageInput.form.submit();
                }
            });
        }

        if (window.LatteAndLettersAddress) {
            window.LatteAndLettersAddress.initForm({
                cityMunicipality: profileCityMunicipality,
                barangay: profileBarangay,
                zipcode: profileZipcode,
                endpoint: "${pageContext.request.contextPath}/register/barangays",
                cityZipCodes: {
                    <c:forEach items="${registrationCityZipCodes}" var="entry" varStatus="status">
                    "${entry.key}": "${entry.value}"<c:if test="${!status.last}">,</c:if>
                    </c:forEach>
                }
            });
        }

        profileNameFields.forEach(function (input) {
            if (!input) {
                return;
            }

            applyFormattedCase(input, toWordCase);
            input.addEventListener("input", function () {
                applyFormattedCase(input, toWordCase);
            });
            input.addEventListener("blur", function () {
                applyFormattedCase(input, toWordCase);
            });
        });

        if (profileStreet) {
            applyFormattedCase(profileStreet, toAddressCase);
            profileStreet.addEventListener("input", function () {
                applyFormattedCase(profileStreet, toAddressCase);
            });
            profileStreet.addEventListener("blur", function () {
                applyFormattedCase(profileStreet, toAddressCase);
            });
        }

        updateOtpCountdowns();
        window.setInterval(updateOtpCountdowns, 1000);
    })();
</script>
</body>
</html>


