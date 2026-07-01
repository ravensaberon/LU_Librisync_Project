<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Reservation Management</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260504-global-side-nav-flush3">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Reservation Queue</span>
            <div class="brand-title mt-2">Manage reservations</div>
        </div>
        <div class="nav-links">
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/dashboard">Dashboard</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/books">Books</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/issues">Issue / Return</a>
            <a class="nav-pill active" href="${pageContext.request.contextPath}/admin/reservations">Reservations</a>
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

    <c:if test="${not empty success}">
        <div class="alert alert-success">${success}</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-danger">${error}</div>
    </c:if>

    <section class="hero-card mb-4">
        <div class="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
                <span class="tag-chip">Reservation Queue</span>
                <h1 class="fw-bold mt-3 mb-2">Queue Reservations</h1>
                <p class="muted-text mb-0">Students who reserved a book in advance. Release when their copy is ready for pickup at the desk.</p>
            </div>
            <button class="btn btn-warm scanner-trigger" type="button" data-bs-toggle="modal" data-bs-target="#reservationQrScannerModal">
                <i class="bi bi-qr-code-scan me-2"></i>Scan student pickup QR
            </button>
        </div>
    </section>

    <section class="stat-grid mb-4">
        <div class="metric-card">
            <div class="metric-value">${reservationCount}</div>
            <div class="metric-label">Reservation records</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${pendingReservationCount}</div>
            <div class="metric-label">Pending queue requests</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${readyReservationCount}</div>
            <div class="metric-label">Ready to claim</div>
        </div>
    </section>

    <section class="panel-card" data-table-search-section data-table-search-empty="No reservation rows matched your search on this page.">
        <div class="table-search-header">
            <div>
                <div class="section-title mb-1">Queue reservations</div>
                <div class="table-search-meta" data-table-search-count></div>
            </div>
            <label class="table-search-shell" aria-label="Search queue reservations">
                <i class="bi bi-search"></i>
                <input class="table-search-input" type="search" placeholder="Search this table" data-table-search-input>
            </label>
        </div>
        <div class="table-responsive">
            <table class="table align-middle" data-table-search-table>
                <thead>
                <tr>
                    <th>Book</th>
                    <th>Student</th>
                    <th>Queue</th>
                    <th>Status</th>
                    <th>Reserved at</th>
                    <th>Claim by</th>
                    <th>Desk release</th>
                    <th>Cancel</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${queueReservations}" var="reservation">
                    <tr>
                        <td>${reservation.book.title}</td>
                        <td>${reservation.student.studentId} - ${reservation.student.user.name}</td>
                        <td>${reservation.queuePosition}</td>
                        <td><span class="tag-chip">${reservation.status}</span></td>
                        <td>${reservation.reservedAtDisplay}</td>
                        <td>${reservation.expiresAtDisplay}</td>
                        <td>
                            <c:choose>
                                <c:when test="${reservation.status.name() == 'READY'}">
                                    <form method="post" action="${pageContext.request.contextPath}/admin/reservations/${reservation.id}/claim" class="d-grid gap-2">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                        <input type="hidden" name="queuePage" value="${queueReservationsPage.page}">
                                        <input class="form-control form-control-sm" name="remarks" placeholder="Optional remarks">
                                        <button class="btn btn-brand" type="submit"><i class="bi bi-box-arrow-right me-2"></i>Confirm Pickup</button>
                                        <span class="muted-text small">Due date is set automatically using the circulation policy.</span>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <span class="muted-text">Desk release unlocks when the reservation becomes READY.</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:if test="${reservation.active}">
                                <form method="post" action="${pageContext.request.contextPath}/admin/reservations/${reservation.id}/cancel">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                    <input type="hidden" name="queuePage" value="${queueReservationsPage.page}">
                                    <button class="btn btn-warm" type="submit">Cancel</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty queueReservations}">
                    <tr>
                        <td colspan="8" class="text-center muted-text">No queue reservation records available yet.</td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
        <c:if test="${queueReservationsPage.totalPages > 1}">
            <nav class="mt-4" aria-label="Admin reservation queue pages">
                <ul class="pagination justify-content-center mb-0">
                    <li class="page-item <c:if test='${!queueReservationsPage.hasPrevious}'>disabled</c:if>">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/reservations?queuePage=${queueReservationsPage.previousPage}">Previous</a>
                    </li>
                    <c:forEach begin="${queueReservationsPage.startPage}" end="${queueReservationsPage.endPage}" var="pageNumber">
                        <li class="page-item <c:if test='${pageNumber == queueReservationsPage.page}'>active</c:if>">
                            <a class="page-link" href="${pageContext.request.contextPath}/admin/reservations?queuePage=${pageNumber}">${pageNumber}</a>
                        </li>
                    </c:forEach>
                    <li class="page-item <c:if test='${!queueReservationsPage.hasNext}'>disabled</c:if>">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/reservations?queuePage=${queueReservationsPage.nextPage}">Next</a>
                    </li>
                </ul>
            </nav>
        </c:if>
    </section>

    <div class="modal fade" id="reservationQrScannerModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content border-0 shadow-lg">
                <div class="modal-header modal-header-brand">
                    <div>
                        <span class="modal-kicker">Pickup Scan</span>
                        <h2 class="h4 mb-1 mt-2">Scan a student reservation QR</h2>
                        <p class="modal-subtitle mb-0">Scan the student's QR so the desk release can be recorded automatically. The due date is set by the circulation policy.</p>
                    </div>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <form id="reservationQrClaimForm" method="post" action="${pageContext.request.contextPath}/admin/reservations/claim-by-qr" class="row g-3 mb-3">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <input type="hidden" name="queuePage" value="${queueReservationsPage.page}">
                        <input type="hidden" name="qrCode" id="reservationQrCodeField">
                        <div class="col-12">
                            <label class="form-label" for="reservationScannerRemarks">Remarks</label>
                            <input class="form-control" id="reservationScannerRemarks" name="remarks" placeholder="Optional remarks for the issued copy">
                            <div class="form-note mt-2">Due date is assigned automatically when the pickup is confirmed.</div>
                        </div>
                    </form>
                    <div id="reservationScannerView">
                        <div class="scanner-shell">
                            <video id="reservationScannerVideo" autoplay muted playsinline></video>
                            <div class="scanner-overlay"></div>
                            <div class="scanner-target scanner-target-qr"></div>
                        </div>
                        <div class="scanner-status" id="reservationScannerStatus">
                            Camera scanner is preparing. Hold the student pickup QR inside the highlighted frame.
                        </div>
                        <div class="scanner-upload">
                            <div class="scanner-upload-actions">
                                <label class="btn btn-warm mb-0" for="reservationScannerUpload">
                                    <i class="bi bi-image me-2"></i>Upload QR from gallery
                                </label>
                                <input class="d-none" id="reservationScannerUpload" type="file" accept="image/*">
                                <span class="form-note">You can also choose a saved screenshot of the student's QR code.</span>
                            </div>
                        </div>
                    </div>
                    <div id="reservationScannerPreview" hidden>
                        <div class="support-item mb-3">
                            <strong>QR code detected</strong>
                            <span id="reservationPreviewCode" class="d-block mt-1" style="font-family:monospace;font-size:.9rem;word-break:break-all;color:var(--primary-900)"></span>
                        </div>
                        <p class="muted-text mb-3">Review the code above. Click <strong>Confirm pickup and issue</strong> to complete the desk release, or <strong>Re-scan</strong> to try again.</p>
                        <div class="d-flex gap-2">
                            <button class="btn btn-brand" type="submit" form="reservationQrClaimForm" id="reservationPreviewConfirmBtn">
                                <i class="bi bi-box-arrow-right me-2"></i>Confirm pickup and issue
                            </button>
                            <button class="btn btn-warm" type="button" id="reservationPreviewRescanBtn">
                                <i class="bi bi-arrow-repeat me-2"></i>Re-scan
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/vendor/jsQR.js"></script>
<script src="${pageContext.request.contextPath}/js/qr-tools.js"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
<script>
    document.addEventListener("DOMContentLoaded", function () {
        const scannerModalElement = document.getElementById("reservationQrScannerModal");
        const scannerUploadInput = document.getElementById("reservationScannerUpload");
        const qrCodeField = document.getElementById("reservationQrCodeField");
        const claimForm = document.getElementById("reservationQrClaimForm");
        const scannerView = document.getElementById("reservationScannerView");
        const scannerPreview = document.getElementById("reservationScannerPreview");
        const previewCode = document.getElementById("reservationPreviewCode");
        const previewConfirmBtn = document.getElementById("reservationPreviewConfirmBtn");
        const previewRescanBtn = document.getElementById("reservationPreviewRescanBtn");
        const scanner = window.LatteAndLettersQr.createScanner({
            videoElement: document.getElementById("reservationScannerVideo"),
            statusElement: document.getElementById("reservationScannerStatus"),
            formats: ["qr_code"],
            liveMessage: "Scanner is live. Aim the camera at the student's pickup QR and hold it steady.",
            qrFallbackMessage: "QR-only scanning is active on this browser. Aim the camera at the student's reservation QR.",
            unsupportedMessage: "This browser cannot decode live QR codes. You can still upload a saved QR image.",
            permissionMessage: "Camera access was blocked or unavailable. Please allow camera use, then try again.",
            fileSuccessMessage: "QR image decoded. Review the details below.",
            onDetected: showReservationScanPreview,
            onScanError: function () {
                window.LatteAndLettersQr.setStatus(
                    document.getElementById("reservationScannerStatus"),
                    "Camera access is active, but the current frame could not be decoded yet.",
                    true
                );
            }
        });

        function showReservationScanPreview(rawValue) {
            const detectedCode = (rawValue || "").trim();
            if (!detectedCode) {
                return;
            }

            scanner.stop();
            qrCodeField.value = detectedCode;
            previewCode.textContent = detectedCode;
            scannerView.hidden = true;
            scannerPreview.hidden = false;
        }

        function resetReservationScanner() {
            qrCodeField.value = "";
            previewCode.textContent = "";
            scannerPreview.hidden = true;
            scannerView.hidden = false;
            scanner.start();
        }

        previewRescanBtn.addEventListener("click", function () {
            resetReservationScanner();
        });

        scannerModalElement.addEventListener("shown.bs.modal", function () {
            scannerPreview.hidden = true;
            scannerView.hidden = false;
            scanner.start();
        });

        scannerModalElement.addEventListener("hidden.bs.modal", function () {
            scanner.stop();
            scanner.setStatus("Camera scanner is preparing. Hold the student pickup QR inside the highlighted frame.", false);
            scannerUploadInput.value = "";
            qrCodeField.value = "";
            previewCode.textContent = "";
            scannerPreview.hidden = true;
            scannerView.hidden = false;
        });

        scannerUploadInput.addEventListener("change", function (event) {
            const selectedFile = event.target.files && event.target.files[0];
            if (!selectedFile) {
                return;
            }

            scanner.decodeFile(selectedFile);
            scannerUploadInput.value = "";
        });
    });
</script>
</body>
</html>

