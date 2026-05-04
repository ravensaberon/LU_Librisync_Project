<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Pickup Requests</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Pickup Requests</span>
            <div class="brand-title mt-2">Borrow requests and queue</div>
        </div>
        <div class="nav-links">
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/dashboard">Dashboard</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/catalog">Catalog</a>
            <a class="nav-pill active" href="${pageContext.request.contextPath}/student/reservations">Pickup requests</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/profile">Profile</a>
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
    <c:if test="${not empty error}">
        <div class="alert alert-danger">${error}</div>
    </c:if>

    <section class="hero-card mb-4">
        <h1 class="fw-bold mb-2">Borrow requests and queue status</h1>
        <p class="muted-text mb-0">Available physical books that you request for desk pickup and queued reservations both appear here until staff releases or cancels them.</p>
    </section>

    <section class="panel-card">
        <div class="section-tabs mb-4" role="tablist" aria-label="Pickup request sections">
            <a class="section-tab <c:if test='${activeTab == "borrow"}'>active</c:if>"
               href="${pageContext.request.contextPath}/student/reservations?tab=borrow&borrowPage=${borrowRequestsPage.page}&queuePage=${queueReservationsPage.page}"
               role="tab"
               aria-selected="${activeTab == 'borrow'}">
                Borrow requests
            </a>
            <a class="section-tab <c:if test='${activeTab == "queue"}'>active</c:if>"
               href="${pageContext.request.contextPath}/student/reservations?tab=queue&borrowPage=${borrowRequestsPage.page}&queuePage=${queueReservationsPage.page}"
               role="tab"
               aria-selected="${activeTab == 'queue'}">
                Reservation queue
            </a>
        </div>

        <c:if test="${activeTab == 'borrow'}">
            <div class="tab-panel active" role="tabpanel" aria-label="Borrow requests">
                <div class="section-title">Borrow requests</div>
                <div class="table-responsive">
                    <table class="table align-middle">
                        <thead>
                        <tr>
                            <th>Book</th>
                            <th>Status</th>
                            <th>Requested at</th>
                            <th>Claim by</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${borrowRequests}" var="reservation">
                            <tr>
                                <td>
                                    ${reservation.book.title}
                                    <div class="muted-text small">Desk pickup request</div>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${reservation.status.name() == 'PENDING_APPROVAL'}">
                                            <span class="tag-chip warn">Pending approval</span>
                                        </c:when>
                                        <c:when test="${reservation.status.name() == 'READY'}">
                                            <span class="tag-chip">Approved</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="tag-chip">${reservation.status}</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${reservation.reservedAtDisplay}</td>
                                <td>${reservation.expiresAtDisplay}</td>
                                <td style="min-width:180px">
                                    <div class="d-flex flex-column gap-2 align-items-start">
                                        <c:choose>
                                            <c:when test="${reservation.status.name() == 'PENDING_APPROVAL'}">
                                                <span class="muted-text small">Waiting for staff approval.</span>
                                            </c:when>
                                            <c:when test="${reservation.status.name() == 'READY'}">
                                                <span class="muted-text small">Bring your ID to the desk.</span>
                                            </c:when>
                                        </c:choose>
                                        <div class="d-flex flex-wrap gap-2">
                                            <c:if test="${reservation.status.name() == 'PENDING_APPROVAL' or reservation.status.name() == 'READY'}">
                                                <button class="btn btn-warm btn-sm" type="button"
                                                        data-bs-toggle="modal"
                                                        data-bs-target="#reservationQrModal"
                                                        data-reservation-qr="${reservation.deskQrCode}"
                                                        data-book-title="${reservation.book.title}"
                                                        data-reservation-type="Borrow request"
                                                        data-reservation-status="${reservation.status}">
                                                    <i class="bi bi-qr-code me-1"></i>Show QR
                                                </button>
                                            </c:if>
                                            <c:if test="${reservation.active}">
                                                <form method="post" action="${pageContext.request.contextPath}/student/reservations/${reservation.id}/cancel">
                                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                    <input type="hidden" name="tab" value="borrow">
                                                    <input type="hidden" name="borrowPage" value="${borrowRequestsPage.page}">
                                                    <input type="hidden" name="queuePage" value="${queueReservationsPage.page}">
                                                    <button class="btn btn-outline-secondary btn-sm" type="submit">Cancel</button>
                                                </form>
                                            </c:if>
                                        </div>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty borrowRequests}">
                            <tr>
                                <td colspan="5" class="text-center muted-text">You do not have any active borrow requests yet.</td>
                            </tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
                <c:if test="${borrowRequestsPage.totalPages > 1}">
                    <nav class="mt-4" aria-label="Borrow request pages">
                        <ul class="pagination justify-content-center mb-0">
                            <li class="page-item <c:if test='${!borrowRequestsPage.hasPrevious}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/student/reservations?tab=borrow&borrowPage=${borrowRequestsPage.previousPage}&queuePage=${queueReservationsPage.page}">Previous</a>
                            </li>
                            <c:forEach begin="${borrowRequestsPage.startPage}" end="${borrowRequestsPage.endPage}" var="pageNumber">
                                <li class="page-item <c:if test='${pageNumber == borrowRequestsPage.page}'>active</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/student/reservations?tab=borrow&borrowPage=${pageNumber}&queuePage=${queueReservationsPage.page}">${pageNumber}</a>
                                </li>
                            </c:forEach>
                            <li class="page-item <c:if test='${!borrowRequestsPage.hasNext}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/student/reservations?tab=borrow&borrowPage=${borrowRequestsPage.nextPage}&queuePage=${queueReservationsPage.page}">Next</a>
                            </li>
                        </ul>
                    </nav>
                </c:if>
            </div>
        </c:if>

        <c:if test="${activeTab == 'queue'}">
            <div class="tab-panel active" role="tabpanel" aria-label="Reservation queue">
                <div class="section-title">Reservation queue</div>
                <div class="table-responsive">
                    <table class="table align-middle">
                        <thead>
                        <tr>
                            <th>Book</th>
                            <th>Preferred pickup</th>
                            <th>Queue position</th>
                            <th>Status</th>
                            <th>Reserved at</th>
                            <th>Claim by</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${queueReservations}" var="reservation">
                            <tr>
                                <td>
                                    ${reservation.book.title}
                                    <div class="muted-text small">
                                        <c:choose>
                                            <c:when test="${reservation.status.name() == 'READY'}">
                                                Ready for desk pickup
                                            </c:when>
                                            <c:otherwise>
                                                Waiting in reservation queue
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </td>
                                <td>${reservation.preferredPickupDateDisplay}</td>
                                <td>${reservation.queuePosition}</td>
                                <td>
                                    <span class="tag-chip">${reservation.status}</span>
                                </td>
                                <td>${reservation.reservedAtDisplay}</td>
                                <td>${reservation.expiresAtDisplay}</td>
                                <td>
                                    <div class="d-flex flex-wrap gap-2">
                                        <c:if test="${reservation.status.name() == 'READY'}">
                                            <span class="tag-chip">Bring your ID and visit the circulation desk for release.</span>
                                        </c:if>
                                        <button class="btn btn-outline-secondary" type="button"
                                                data-bs-toggle="modal"
                                                data-bs-target="#reservationQrModal"
                                                data-reservation-qr="${reservation.deskQrCode}"
                                                data-book-title="${reservation.book.title}"
                                                data-reservation-type="Reservation queue"
                                                data-reservation-status="${reservation.status}">
                                            <i class="bi bi-qr-code me-2"></i>Show pickup QR
                                        </button>
                                        <c:if test="${reservation.active}">
                                            <form method="post" action="${pageContext.request.contextPath}/student/reservations/${reservation.id}/cancel">
                                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                <input type="hidden" name="tab" value="queue">
                                                <input type="hidden" name="borrowPage" value="${borrowRequestsPage.page}">
                                                <input type="hidden" name="queuePage" value="${queueReservationsPage.page}">
                                                <button class="btn btn-warm" type="submit">Cancel</button>
                                            </form>
                                        </c:if>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty queueReservations}">
                            <tr>
                                <td colspan="7" class="text-center muted-text">You do not have any reservation queue entries yet.</td>
                            </tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
                <c:if test="${queueReservationsPage.totalPages > 1}">
                    <nav class="mt-4" aria-label="Reservation queue pages">
                        <ul class="pagination justify-content-center mb-0">
                            <li class="page-item <c:if test='${!queueReservationsPage.hasPrevious}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/student/reservations?tab=queue&borrowPage=${borrowRequestsPage.page}&queuePage=${queueReservationsPage.previousPage}">Previous</a>
                            </li>
                            <c:forEach begin="${queueReservationsPage.startPage}" end="${queueReservationsPage.endPage}" var="pageNumber">
                                <li class="page-item <c:if test='${pageNumber == queueReservationsPage.page}'>active</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/student/reservations?tab=queue&borrowPage=${borrowRequestsPage.page}&queuePage=${pageNumber}">${pageNumber}</a>
                                </li>
                            </c:forEach>
                            <li class="page-item <c:if test='${!queueReservationsPage.hasNext}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/student/reservations?tab=queue&borrowPage=${borrowRequestsPage.page}&queuePage=${queueReservationsPage.nextPage}">Next</a>
                            </li>
                        </ul>
                    </nav>
                </c:if>
            </div>
        </c:if>
    </section>

    <div class="modal fade" id="reservationQrModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg">
                <div class="modal-header modal-header-brand">
                    <div>
                        <span class="modal-kicker">Pickup QR</span>
                        <h2 class="h4 mb-1 mt-2">Show this code at the circulation desk</h2>
                        <p class="modal-subtitle mb-0">Admin can scan this QR from your phone to match your request and record the desk release faster.</p>
                    </div>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="qr-card">
                        <div class="qr-code-shell mb-3" id="reservationQrCanvas"></div>
                        <div class="qr-code-meta">
                            <div>
                                <span class="info-tile-label">Reservation code</span>
                                <span class="qr-code-value" id="reservationQrValue">No reservation selected yet.</span>
                            </div>
                            <div class="row g-2 mt-1">
                                <div class="col-12">
                                    <div class="info-tile">
                                        <span class="info-tile-label">Book</span>
                                        <span class="info-tile-value" id="reservationQrBookTitle">Not selected</span>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="info-tile">
                                        <span class="info-tile-label">Request type</span>
                                        <span class="info-tile-value" id="reservationQrType">Not selected</span>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="info-tile">
                                        <span class="info-tile-label">Status</span>
                                        <span class="info-tile-value" id="reservationQrStatus">Not selected</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="d-flex flex-wrap gap-2 mt-3">
                            <button class="btn btn-brand" id="downloadReservationQrButton" type="button" disabled>Download PNG</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/vendor/qrious.min.js"></script>
<script src="${pageContext.request.contextPath}/js/qr-tools.js"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
<script>
    document.addEventListener("DOMContentLoaded", function () {
        const qrModalElement = document.getElementById("reservationQrModal");
        const qrCanvasElement = document.getElementById("reservationQrCanvas");
        const qrValueElement = document.getElementById("reservationQrValue");
        const qrBookTitleElement = document.getElementById("reservationQrBookTitle");
        const qrTypeElement = document.getElementById("reservationQrType");
        const qrStatusElement = document.getElementById("reservationQrStatus");
        const downloadButton = document.getElementById("downloadReservationQrButton");
        let currentReservationQrCanvas = null;

        qrModalElement.addEventListener("show.bs.modal", function (event) {
            const trigger = event.relatedTarget;
            const reservationQr = trigger.getAttribute("data-reservation-qr") || "";
            const bookTitle = trigger.getAttribute("data-book-title") || "Not selected";
            const reservationType = trigger.getAttribute("data-reservation-type") || "Not selected";
            const reservationStatus = trigger.getAttribute("data-reservation-status") || "Not selected";

            qrValueElement.textContent = reservationQr;
            qrBookTitleElement.textContent = bookTitle;
            qrTypeElement.textContent = reservationType;
            qrStatusElement.textContent = reservationStatus;
            currentReservationQrCanvas = window.LuLibrisyncQr.renderQr(qrCanvasElement, reservationQr, {
                size: 240,
                emptyText: "No reservation QR code is available.",
                errorText: "Unable to render this reservation QR code."
            });
            downloadButton.disabled = !currentReservationQrCanvas;
            downloadButton.dataset.filename = window.LuLibrisyncQr.normalizeFilename(bookTitle, "reservation") + "-pickup-qr.png";
        });

        downloadButton.addEventListener("click", function () {
            if (!currentReservationQrCanvas) {
                return;
            }

            window.LuLibrisyncQr.downloadCanvas(currentReservationQrCanvas, downloadButton.dataset.filename);
        });
    });
</script>
</body>
</html>


