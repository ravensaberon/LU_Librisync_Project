<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Library Catalog</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Advanced Search</span>
            <div class="brand-title mt-2">Browse the library catalog</div>
        </div>
        <div class="nav-links">
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/dashboard">Dashboard</a>
            <a class="nav-pill active" href="${pageContext.request.contextPath}/student/catalog">Catalog</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/student/reservations">Pickup requests</a>
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

    <section class="panel-card mb-4">
        <form method="get" action="${pageContext.request.contextPath}/student/catalog" class="row g-3" id="catalogSearchForm">
            <div class="col-md-3">
                <label class="form-label" for="keyword">Title or code</label>
                <input class="form-control" id="keyword" name="keyword" value="${keyword}">
            </div>
            <div class="col-md-3">
                <label class="form-label" for="isbn">ISBN</label>
                <input class="form-control" id="isbn" name="isbn" value="${isbnValue}">
            </div>
            <div class="col-md-2">
                <label class="form-label" for="categoryId">Category</label>
                <select class="form-select" id="categoryId" name="categoryId">
                    <option value="">All</option>
                    <c:forEach items="${categories}" var="category">
                        <option value="${category.id}" <c:if test="${selectedCategoryId == category.id}">selected</c:if>>${category.name}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-2">
                <label class="form-label" for="authorId">Author</label>
                <select class="form-select" id="authorId" name="authorId">
                    <option value="">All</option>
                    <c:forEach items="${authors}" var="author">
                        <option value="${author.id}" <c:if test="${selectedAuthorId == author.id}">selected</c:if>>${author.name}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-2 d-flex align-items-end">
                <div class="form-check">
                    <input class="form-check-input" id="availableOnly" name="availableOnly" type="checkbox" value="true" <c:if test="${availableOnly}">checked</c:if>>
                    <label class="form-check-label" for="availableOnly">Available only</label>
                </div>
            </div>
            <div class="col-12 d-flex flex-wrap gap-2">
                <button class="btn btn-brand" type="submit">
                    <i class="bi bi-search me-2"></i>Search catalog
                </button>
                <button class="btn btn-warm scanner-trigger" type="button" data-bs-toggle="modal" data-bs-target="#catalogScannerModal">
                    <i class="bi bi-upc-scan"></i>Scan barcode
                </button>
            </div>
        </form>
    </section>

    <section class="catalog-grid catalog-grid-uniform">
        <c:forEach items="${books}" var="book">
            <c:set var="reservationStatus" value="${studentReservationStatusByBookId[book.id]}"/>
            <c:set var="activeIssueStatus" value="${studentActiveIssueStatusByBookId[book.id]}"/>
            <c:set var="activeIssueId" value="${studentActiveIssueIdByBookId[book.id]}"/>
            <c:set var="activeIssueDueDate" value="${studentActiveIssueDueDateByBookId[book.id]}"/>
            <c:set var="returnRequested" value="${studentActiveIssueReturnRequestedByBookId[book.id]}"/>
            <c:set var="walkInBorrowableCopies" value="${walkInBorrowableCopyCountByBook[book.id]}"/>
            <c:set var="reservationPickupDate" value="${not empty nextAvailableDateByBookId[book.id] ? nextAvailableDateByBookId[book.id] : todayDate}"/>
            <button class="catalog-card catalog-card-button" type="button" data-bs-toggle="modal" data-bs-target="#catalogBookModal${book.id}">
                <span class="catalog-cover">
                    <c:choose>
                        <c:when test="${readableBookCoverByBookId[book.id]}">
                            <img class="catalog-cover-image" src="${pageContext.request.contextPath}/books/${book.id}/cover" alt="${book.title} cover">
                        </c:when>
                        <c:otherwise>
                            <i class="bi bi-book-half"></i>
                        </c:otherwise>
                    </c:choose>
                </span>
                <span class="catalog-summary">
                    <span class="catalog-title" title="${book.title}">${book.title}</span>
                    <span class="catalog-author" title="${book.author.name}">${book.author.name}</span>
                    <span class="catalog-year"><c:out value="${empty book.publicationYear ? 'Year not set' : book.publicationYear}"/></span>
                </span>
            </button>

            <div class="modal fade" id="catalogBookModal${book.id}" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered modal-lg">
                    <div class="modal-content border-0 shadow-lg">
                        <div class="modal-header modal-header-brand">
                            <div>
                                <span class="modal-kicker">Catalog Title</span>
                                <h2 class="h3 mb-1 mt-2">${book.title}</h2>
                                <p class="modal-subtitle mb-0">${book.author.name} &bull; <c:out value="${empty book.publicationYear ? 'Year not set' : book.publicationYear}"/></p>
                            </div>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body p-4">
                            <div class="modal-panel-grid">
                                <div class="modal-card">
                                    <div class="catalog-cover catalog-cover-large mb-3">
                                        <c:choose>
                                            <c:when test="${readableBookCoverByBookId[book.id]}">
                                                <img class="catalog-cover-image" src="${pageContext.request.contextPath}/books/${book.id}/cover" alt="${book.title} cover">
                                            </c:when>
                                            <c:otherwise>
                                                <i class="bi bi-journal-richtext"></i>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div class="d-flex flex-wrap gap-2 mb-3">
                                        <c:choose>
                                            <c:when test="${not empty activeIssueStatus}">
                                                <span class="tag-chip">Borrowed by you</span>
                                            </c:when>
                                            <c:when test="${reservationStatus == 'BORROW:READY'}">
                                                <span class="tag-chip">Ready for desk pickup</span>
                                            </c:when>
                                            <c:when test="${reservationStatus == 'BORROW:PENDING_APPROVAL'}">
                                                <span class="tag-chip warn">Borrow request pending approval</span>
                                            </c:when>
                                            <c:when test="${walkInBorrowableCopies > 0}">
                                                <span class="tag-chip">Available now</span>
                                            </c:when>
                                            <c:when test="${book.availableQuantity > 0}">
                                                <span class="tag-chip warn">Currently on hold</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="tag-chip warn">All copies occupied</span>
                                            </c:otherwise>
                                        </c:choose>
                                        <c:if test="${readableEbookByBookId[book.id]}">
                                            <span class="tag-chip subtle">Digital copy available</span>
                                        </c:if>
                                    </div>
                                    <p class="muted-text mb-0">
                                        <c:out value="${empty book.description ? 'Synopsis is not available yet for this title.' : book.description}"/>
                                    </p>
                                    <c:if test="${readableEbookByBookId[book.id]}">
                                        <div class="support-item mt-3">
                                            <strong>Digital access</strong>
                                            <span>You can keep reading this title online even when all physical copies are already borrowed or reserved.</span>
                                        </div>
                                    </c:if>
                                </div>

                                <div class="modal-card">
                                    <div class="modal-stat-grid">
                                        <div class="modal-stat-card">
                                            <span class="modal-stat-label">Category</span>
                                            <strong class="modal-stat-value">${book.category.name}</strong>
                                        </div>
                                        <div class="modal-stat-card">
                                            <span class="modal-stat-label">ISBN</span>
                                            <strong class="modal-stat-value">${book.isbn}</strong>
                                        </div>
                                        <div class="modal-stat-card">
                                            <span class="modal-stat-label">Shelf</span>
                                            <strong class="modal-stat-value"><c:out value="${empty book.shelfLocation ? 'Not set' : book.shelfLocation}"/></strong>
                                        </div>
                                        <div class="modal-stat-card">
                                            <span class="modal-stat-label">Copies</span>
                                            <strong class="modal-stat-value">${book.availableQuantity} / ${book.quantity}</strong>
                                        </div>
                                        <div class="modal-stat-card">
                                            <span class="modal-stat-label">Queue</span>
                                            <strong class="modal-stat-value">${empty reservationQueueSizes[book.id] ? 0 : reservationQueueSizes[book.id]}</strong>
                                        </div>
                                        <div class="modal-stat-card">
                                            <span class="modal-stat-label">Borrow due</span>
                                            <strong class="modal-stat-value">${defaultBorrowDueDate}</strong>
                                        </div>
                                        <div class="modal-stat-card">
                                            <span class="modal-stat-label">Digital reading</span>
                                            <strong class="modal-stat-value">
                                                <c:choose>
                                                    <c:when test="${readableEbookByBookId[book.id]}">Always available</c:when>
                                                    <c:otherwise>Not uploaded yet</c:otherwise>
                                                </c:choose>
                                            </strong>
                                        </div>
                                    </div>

                                    <c:if test="${walkInBorrowableCopies < 1 and empty reservationStatus and not empty nextAvailableDateByBookId[book.id]}">
                                        <div class="support-item mt-3">
                                            <strong>Estimated next copy</strong>
                                            <span>${nextAvailableDateByBookId[book.id]}</span>
                                        </div>
                                    </c:if>
                                    <c:if test="${walkInBorrowableCopies < 1 and book.availableQuantity > 0}">
                                        <div class="support-item mt-3">
                                            <strong>Hold status</strong>
                                            <span>The currently open copy is already reserved for queued borrowers.</span>
                                        </div>
                                    </c:if>

                                    <div class="d-flex flex-wrap gap-2 mt-4">
                                        <c:if test="${readableEbookByBookId[book.id]}">
                                            <a class="btn btn-warm" href="${pageContext.request.contextPath}/student/ebooks/${book.id}">Read online</a>
                                        </c:if>

                                        <c:choose>
                                            <c:when test="${not empty activeIssueStatus}">
                                                <span class="tag-chip">Active loan: ${activeIssueStatus}</span>
                                                <c:if test="${not empty activeIssueDueDate}">
                                                    <span class="tag-chip subtle">Due: ${activeIssueDueDate}</span>
                                                </c:if>
                                                <c:choose>
                                                    <c:when test="${returnRequested}">
                                                        <span class="tag-chip warn">Return request pending</span>
                                                        <form method="post" action="${pageContext.request.contextPath}/student/issues/${activeIssueId}/cancel-return-request">
                                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                            <input type="hidden" name="redirectTo" value="/student/catalog">
                                                            <button class="btn btn-outline-secondary" type="submit">Cancel return request</button>
                                                        </form>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <form method="post" action="${pageContext.request.contextPath}/student/issues/${activeIssueId}/request-return">
                                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                            <input type="hidden" name="redirectTo" value="/student/catalog">
                                                            <button class="btn btn-outline-secondary" type="submit">Request return</button>
                                                        </form>
                                                    </c:otherwise>
                                                </c:choose>
                                            </c:when>
                                            <c:when test="${reservationStatus == 'BORROW:READY'}">
                                                <span class="tag-chip">Proceed to the circulation desk for release.</span>
                                                <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/student/reservations">View reservation</a>
                                            </c:when>
                                            <c:when test="${reservationStatus == 'BORROW:PENDING_APPROVAL'}">
                                                <span class="tag-chip warn">Borrow request pending staff approval.</span>
                                                <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/student/reservations">View request</a>
                                            </c:when>
                                            <c:when test="${not empty reservationStatus}">
                                                <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/student/reservations">View reservation</a>
                                            </c:when>
                                            <c:otherwise>
                                                <c:if test="${walkInBorrowableCopies > 0}">
                                                    <form method="post" action="${pageContext.request.contextPath}/student/catalog/${book.id}/borrow">
                                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                        <button class="btn btn-brand" type="submit">Request desk pickup</button>
                                                    </form>
                                                </c:if>
                                                <form method="post" action="${pageContext.request.contextPath}/student/reservations" class="reservation-pickup-form">
                                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                    <input type="hidden" name="bookId" value="${book.id}">
                                                    <label class="form-label mb-0" for="preferredPickupDate${book.id}">Pickup date</label>
                                                    <input
                                                            class="form-control"
                                                            id="preferredPickupDate${book.id}"
                                                            name="preferredPickupDate"
                                                            type="date"
                                                            value="${reservationPickupDate}"
                                                            min="${todayDate}"
                                                            max="${reservationScheduleMaxDate}"
                                                            required>
                                                    <button class="btn btn-warm" type="submit">Join reservation queue</button>
                                                </form>
                                            </c:otherwise>
                                        </c:choose>
                                        <button class="btn btn-outline-secondary" type="button" data-bs-dismiss="modal">Close</button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </c:forEach>
    </section>

    <c:if test="${empty books}">
        <section class="panel-card mt-4">
            <p class="mb-0 muted-text">No books matched your current filters.</p>
        </section>
    </c:if>

    <c:if test="${booksPage.totalPages > 1}">
        <nav class="mt-4" aria-label="Catalog pages">
            <ul class="pagination justify-content-center mb-0">
                <li class="page-item <c:if test='${!booksPage.hasPrevious}'>disabled</c:if>">
                    <a class="page-link" href="${pageContext.request.contextPath}/student/catalog?page=${booksPage.previousPage}&keyword=${keyword}&categoryId=${selectedCategoryId}&authorId=${selectedAuthorId}&isbn=${isbnValue}&availableOnly=${availableOnly}">Previous</a>
                </li>
                <c:forEach begin="${booksPage.startPage}" end="${booksPage.endPage}" var="pageNumber">
                    <li class="page-item <c:if test='${pageNumber == booksPage.page}'>active</c:if>">
                        <a class="page-link" href="${pageContext.request.contextPath}/student/catalog?page=${pageNumber}&keyword=${keyword}&categoryId=${selectedCategoryId}&authorId=${selectedAuthorId}&isbn=${isbnValue}&availableOnly=${availableOnly}">${pageNumber}</a>
                    </li>
                </c:forEach>
                <li class="page-item <c:if test='${!booksPage.hasNext}'>disabled</c:if>">
                    <a class="page-link" href="${pageContext.request.contextPath}/student/catalog?page=${booksPage.nextPage}&keyword=${keyword}&categoryId=${selectedCategoryId}&authorId=${selectedAuthorId}&isbn=${isbnValue}&availableOnly=${availableOnly}">Next</a>
                </li>
            </ul>
        </nav>
    </c:if>

    <div class="modal fade" id="catalogScannerModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content border-0 shadow-lg">
                <div class="modal-header modal-header-brand">
                    <div>
                        <span class="modal-kicker">Barcode Scan</span>
                        <h2 class="h4 mb-1 mt-2">Search the catalog with your camera</h2>
                        <p class="modal-subtitle mb-0">Point your device at a book barcode and the search form will update automatically.</p>
                    </div>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="scanner-shell">
                        <video id="catalogScannerVideo" autoplay muted playsinline></video>
                        <div class="scanner-overlay"></div>
                        <div class="scanner-target"></div>
                    </div>
                    <div class="scanner-status" id="catalogScannerStatus">
                        Camera scanner is preparing. Hold the barcode steady inside the highlighted frame.
                    </div>
                    <div class="mt-3 pt-3 border-top">
                        <p class="muted-text mb-2" style="font-size:.85rem;">Camera not working? Take a photo of the barcode and upload it instead.</p>
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <label class="btn btn-warm mb-0" for="catalogBarcodeImageUpload" style="cursor:pointer;">
                                <i class="bi bi-image me-1"></i>Upload barcode photo
                            </label>
                            <input type="file" id="catalogBarcodeImageUpload" accept="image/*" capture="environment" style="display:none;">
                            <span class="muted-text" id="catalogUploadStatus" style="font-size:.85rem;"></span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="https://unpkg.com/@zxing/browser@0.1.5"></script>
<script src="${pageContext.request.contextPath}/js/qr-tools.js"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
<script>
    document.addEventListener("DOMContentLoaded", function () {
        const modalElement = document.getElementById("catalogScannerModal");
        const keywordInput = document.getElementById("keyword");
        const isbnInput = document.getElementById("isbn");
        const searchForm = document.getElementById("catalogSearchForm");
        const scanner = window.LuLibrisyncQr.createScanner({
            videoElement: document.getElementById("catalogScannerVideo"),
            statusElement: document.getElementById("catalogScannerStatus"),
            formats: ["code_128", "ean_13", "ean_8", "upc_a", "upc_e", "code_39", "codabar", "itf"],
            liveMessage: "Scanner is live. Align the barcode inside the frame and hold still for a moment.",
            unsupportedMessage: "This browser cannot decode live barcodes. You can still type the ISBN or barcode manually.",
            permissionMessage: "Camera access was blocked or is unavailable on this device. Please allow camera access, then try again.",
            onDetected: applyDetectedCode,
            onScanError: function () {
                scanner.setStatus("Camera access is active, but the current barcode could not be decoded yet.", true);
            }
        });

        function applyDetectedCode(rawValue) {
            var detectedCode = (rawValue || "").trim();
            if (!detectedCode) {
                return;
            }

            scanner.setStatus("Code detected: " + detectedCode + ". Looking up book...", false);

            // Try exact barcode/ISBN lookup first for instant navigation
            fetch("${pageContext.request.contextPath}/student/catalog/barcode-lookup?code=" + encodeURIComponent(detectedCode), {
                headers: { "Accept": "application/json" }
            })
            .then(function (response) {
                if (response.ok) {
                    return response.json();
                }
                return null;
            })
            .then(function (data) {
                var modal = bootstrap.Modal.getInstance(modalElement);
                if (modal) {
                    modal.hide();
                }

                if (data && data.bookId) {
                    // Exact match found — open the book detail modal directly
                    window.setTimeout(function () {
                        var bookModal = document.getElementById("catalogBookModal" + data.bookId);
                        if (bookModal) {
                            bootstrap.Modal.getOrCreateInstance(bookModal).show();
                        } else {
                            // Book is on a different page — fall back to search
                            keywordInput.value = detectedCode;
                            searchForm.requestSubmit();
                        }
                    }, 220);
                } else {
                    // No exact match — fall back to keyword search
                    keywordInput.value = detectedCode;
                    if (/^\d{10}(\d{3})?$/.test(detectedCode)) {
                        isbnInput.value = detectedCode;
                    }
                    window.setTimeout(function () {
                        searchForm.requestSubmit();
                    }, 220);
                }
            })
            .catch(function () {
                // Network error — fall back to keyword search
                var modal = bootstrap.Modal.getInstance(modalElement);
                if (modal) {
                    modal.hide();
                }
                keywordInput.value = detectedCode;
                if (/^\d{10}(\d{3})?$/.test(detectedCode)) {
                    isbnInput.value = detectedCode;
                }
                window.setTimeout(function () {
                    searchForm.requestSubmit();
                }, 220);
            });
        }

        modalElement.addEventListener("shown.bs.modal", function () {
            scanner.start();
        });
        modalElement.addEventListener("hidden.bs.modal", function () {
            scanner.stop();
            scanner.setStatus("Camera scanner is preparing. Hold the barcode steady inside the highlighted frame.", false);
        });

        // Image upload fallback
        var uploadInput = document.getElementById("catalogBarcodeImageUpload");
        var uploadStatus = document.getElementById("catalogUploadStatus");
        uploadInput.addEventListener("change", function () {
            var file = uploadInput.files && uploadInput.files[0];
            if (!file) {
                return;
            }
            uploadStatus.textContent = "Reading barcode from image...";
            window.LuLibrisyncQr.decodeBarcodeFromImageFile(file)
                .then(function (code) {
                    uploadStatus.textContent = "";
                    uploadInput.value = "";
                    applyDetectedCode(code);
                })
                .catch(function (err) {
                    uploadStatus.textContent = err && err.message ? err.message : "Could not read barcode from image.";
                    uploadInput.value = "";
                });
        });
    });
</script>
</body>
</html>


