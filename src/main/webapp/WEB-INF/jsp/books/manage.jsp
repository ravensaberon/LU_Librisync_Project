<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="_csrf" content="${_csrf.token}">
    <meta name="_csrf_header" content="${_csrf.headerName}">
    <title>Book Management</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260504-global-side-nav-flush3">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Book Management</span>
            <div class="brand-title mt-2">Full catalog and inventory control</div>
        </div>
        <div class="nav-links">
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/dashboard">Dashboard</a>
            <a class="nav-pill active" href="${pageContext.request.contextPath}/admin/books">Books</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/issues">Issue / Return</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/reservations">Reservations</a>
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

    <c:set var="bookFormAction" value="${pageContext.request.contextPath}/admin/books"/>
    <c:if test="${not empty editBook}">
        <c:set var="bookFormAction" value="${pageContext.request.contextPath}/admin/books/${editBook.id}/update"/>
    </c:if>

    <section class="hero-card mb-4">
        <div class="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
                <span class="tag-chip">Catalog Workspace</span>
                <h1 class="fw-bold mt-3 mb-2">Manage your library collection</h1>
                <p class="muted-text mb-0">
                    Add new titles through a popup form, keep bibliographic details organized, and update inventory records without leaving the current page.
                </p>
            </div>
            <div class="d-flex flex-wrap gap-2">
                <button class="btn hero-action-button hero-action-button-brand" type="button" data-bs-toggle="modal" data-bs-target="#bookFormModal">
                    <i class="bi bi-journal-plus me-2"></i>Add book to the library
                </button>
                <button class="btn hero-action-button hero-action-button-soft" type="button" data-bs-toggle="modal" data-bs-target="#archivedBooksModal">
                    <i class="bi bi-archive me-2"></i>Archived books
                    <c:if test="${archivedBookCount > 0}">
                        <span class="badge bg-secondary ms-1">${archivedBookCount}</span>
                    </c:if>
                </button>
            </div>
        </div>
    </section>

    <section class="stat-grid books-stat-grid mb-4">
        <div class="metric-card">
            <div class="metric-value">${bookCount}</div>
            <div class="metric-label">Catalog titles</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${totalCopyCount}</div>
            <div class="metric-label">Total copies</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${availableBookCount}</div>
            <div class="metric-label">Available copies</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${digitalBookCount}</div>
            <div class="metric-label">Digital-ready titles</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${visibleCatalogBookCount}</div>
            <div class="metric-label">Visible in catalog</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${archivedBookCount}</div>
            <div class="metric-label">Archived titles</div>
        </div>
    </section>

    <section class="panel-card" data-table-search-section data-table-search-empty="No inventory rows matched your search on this page.">
        <div class="table-search-header">
            <div>
                <div class="section-title mb-1">Current inventory</div>
                <div class="table-search-meta" data-table-search-count></div>
            </div>
            <label class="table-search-shell" aria-label="Search current inventory">
                <i class="bi bi-search"></i>
                <input class="table-search-input" type="search" placeholder="Search this table" data-table-search-input>
            </label>
        </div>
        <div class="table-responsive">
            <table class="table align-middle" data-table-search-table>
                <thead>
                <tr>
                    <th>Title</th>
                    <th>ISBN / Barcode</th>
                    <th>Category</th>
                    <th>Author</th>
                    <th>Inventory</th>
                    <th>Location</th>
                    <th>Catalog</th>
                    <th>Digital</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${books}" var="book">
                    <tr>
                        <td>
                            <div class="book-title-cell">
                                <div class="book-title-cover">
                                    <c:choose>
                                        <c:when test="${readableBookCoverByBookId[book.id]}">
                                            <img src="${pageContext.request.contextPath}/books/${book.id}/cover" alt="${book.title} cover">
                                        </c:when>
                                        <c:otherwise>
                                            <i class="bi bi-book-half"></i>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <div>
                                    <strong>${book.title}</strong>
                                    <c:if test="${not empty book.publicationYear}">
                                        <div class="muted-text">${book.publicationYear}</div>
                                    </c:if>
                                    <div class="d-flex flex-wrap gap-1 mt-1">
                                        <c:if test="${book.archived}">
                                            <span class="tag-chip warn">Archived</span>
                                        </c:if>
                                        <c:if test="${!book.archived and !book.visibleInCatalog}">
                                            <span class="tag-chip">Hidden from catalog</span>
                                        </c:if>
                                        <c:if test="${!book.archived and book.visibleInCatalog}">
                                            <span class="tag-chip">Visible to students</span>
                                        </c:if>
                                    </div>
                                </div>
                            </div>
                        </td>
                        <td>
                            <div>${book.isbn}</div>
                            <c:choose>
                                <c:when test="${not empty book.barcode}">
                                    <div class="muted-text">${book.barcode}</div>
                                </c:when>
                                <c:otherwise>
                                    <button class="btn btn-warm btn-sm mt-1 generate-barcode-btn"
                                            type="button"
                                            data-book-id="${book.id}"
                                            data-book-title="${book.title}"
                                            data-book-isbn="${book.isbn}"
                                            title="Generate a barcode for this book">
                                        <i class="bi bi-upc me-1"></i>Generate barcode
                                    </button>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty book.category}">${book.category.name}</c:when>
                                <c:otherwise><span class="muted-text">Unassigned</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty book.author}">${book.author.name}</c:when>
                                <c:otherwise><span class="muted-text">Unassigned</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>${book.availableQuantity} / ${book.quantity}</td>
                        <td>${book.shelfLocation}</td>
                        <td>
                            <c:choose>
                                <c:when test="${book.archived}">
                                    <span class="tag-chip warn">Archived</span>
                                </c:when>
                                <c:when test="${book.visibleInCatalog}">
                                    <span class="tag-chip">Shown</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="tag-chip warn">Hidden</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${book.digital}">
                                    <span class="tag-chip">Yes</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="tag-chip warn">No</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="table-actions">
                            <button class="icon-action" type="button" title="View book QR code" data-bs-toggle="modal" data-bs-target="#bookQrModal" data-book-title="${book.title}" data-book-isbn="${book.isbn}" data-book-code="${book.scanCode}" data-book-code-label="${book.scanCodeLabel}">
                                <i class="bi bi-qr-code"></i>
                            </button>
                            <a class="icon-action" href="${pageContext.request.contextPath}/admin/books?editId=${book.id}&page=${booksPage.page}" title="Edit book">
                                <i class="bi bi-pencil-square"></i>
                            </a>
                            <c:choose>
                                <c:when test="${book.archived}">
                                    <form method="post" action="${pageContext.request.contextPath}/admin/books/${book.id}/restore" id="restoreForm-${book.id}">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                        <input type="hidden" name="page" value="${booksPage.page}">
                                        <button class="icon-action"
                                                type="button"
                                                title="Restore book"
                                                data-action="restore"
                                                data-book-id="${book.id}"
                                                data-book-title="${book.title}">
                                            <i class="bi bi-arrow-counterclockwise"></i>
                                        </button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/books/${book.id}/archive" id="archiveForm-${book.id}">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                        <input type="hidden" name="page" value="${booksPage.page}">
                                        <button class="icon-action danger"
                                                type="button"
                                                title="Archive book"
                                                data-action="archive"
                                                data-book-id="${book.id}"
                                                data-book-title="${book.title}">
                                            <i class="bi bi-archive"></i>
                                        </button>
                                    </form>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty books}">
                    <tr>
                        <td colspan="9" class="text-center muted-text">No books available yet.</td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
        <c:if test="${booksPage.totalPages > 1}">
            <nav class="mt-4" aria-label="Book inventory pages">
                <ul class="pagination justify-content-center mb-0">
                    <li class="page-item <c:if test='${!booksPage.hasPrevious}'>disabled</c:if>">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/books?page=${booksPage.previousPage}<c:if test='${not empty editBook}'>&editId=${editBook.id}</c:if>">Previous</a>
                    </li>
                    <c:forEach begin="${booksPage.startPage}" end="${booksPage.endPage}" var="pageNumber">
                        <li class="page-item <c:if test='${pageNumber == booksPage.page}'>active</c:if>">
                            <a class="page-link" href="${pageContext.request.contextPath}/admin/books?page=${pageNumber}<c:if test='${not empty editBook}'>&editId=${editBook.id}</c:if>">${pageNumber}</a>
                        </li>
                    </c:forEach>
                    <li class="page-item <c:if test='${!booksPage.hasNext}'>disabled</c:if>">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/books?page=${booksPage.nextPage}<c:if test='${not empty editBook}'>&editId=${editBook.id}</c:if>">Next</a>
                    </li>
                </ul>
            </nav>
        </c:if>
    </section>


    <%-- Archived books modal --%>
    <div class="modal fade" id="archivedBooksModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content border-0 shadow-lg">
                <div class="modal-header modal-header-brand">
                    <div>
                        <span class="modal-kicker">Archive</span>
                        <h2 class="h4 mb-1 mt-2">Archived books</h2>
                        <p class="modal-subtitle mb-0">These titles are hidden from the student catalog. Restore any book to make it visible again.</p>
                    </div>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <c:choose>
                        <c:when test="${empty archivedBooks}">
                            <p class="muted-text text-center mb-0">No archived books yet.</p>
                        </c:when>
                        <c:otherwise>
                            <div class="table-responsive">
                                <table class="table align-middle mb-0">
                                    <thead>
                                    <tr>
                                        <th>Title</th>
                                        <th>ISBN</th>
                                        <th>Author</th>
                                        <th>Category</th>
                                        <th></th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach items="${archivedBooks}" var="ab">
                                        <tr>
                                            <td>
                                                <strong>${ab.title}</strong>
                                                <c:if test="${not empty ab.publicationYear}">
                                                    <div class="muted-text" style="font-size:.82rem;">${ab.publicationYear}</div>
                                                </c:if>
                                            </td>
                                            <td>${ab.isbn}</td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${not empty ab.author}">${ab.author.name}</c:when>
                                                    <c:otherwise><span class="muted-text">—</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${not empty ab.category}">${ab.category.name}</c:when>
                                                    <c:otherwise><span class="muted-text">—</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="table-actions">
                                                <form method="post" action="${pageContext.request.contextPath}/admin/books/${ab.id}/restore" id="restoreArchivedForm-${ab.id}">
                                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                    <input type="hidden" name="page" value="${booksPage.page}">
                                                    <button class="icon-action"
                                                            type="button"
                                                            title="Restore book"
                                                            data-action="restore-archived"
                                                            data-book-id="${ab.id}"
                                                            data-book-title="${ab.title}">
                                                        <i class="bi bi-arrow-counterclockwise"></i>
                                                    </button>
                                                </form>
                                                <form method="post" action="${pageContext.request.contextPath}/admin/books/${ab.id}/delete" id="deleteForm-${ab.id}">
                                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                    <input type="hidden" name="page" value="${booksPage.page}">
                                                    <button class="icon-action danger"
                                                            type="button"
                                                            title="Permanently delete book"
                                                            data-action="permanent-delete"
                                                            data-book-id="${ab.id}"
                                                            data-book-title="${ab.title}">
                                                        <i class="bi bi-trash3"></i>
                                                    </button>
                                                </form>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-warm" data-bs-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="barcodeStickerModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg">
                <div class="modal-header modal-header-brand">
                    <div>
                        <span class="modal-kicker">Barcode Generated</span>
                        <h2 class="h4 mb-1 mt-2">Print &amp; stick to the physical book</h2>
                        <p class="modal-subtitle mb-0">Download the QR sticker and print it, then attach it to the book cover or spine.</p>
                    </div>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="qr-card">
                        <div class="qr-code-shell mb-3" id="stickerQrCanvas"></div>
                        <div class="qr-code-meta">
                            <div>
                                <span class="info-tile-label">Generated barcode</span>
                                <span class="qr-code-value" id="stickerBarcodeValue" style="font-family:monospace;letter-spacing:0.08em;"></span>
                            </div>
                            <div class="row g-2 mt-1">
                                <div class="col-12">
                                    <div class="info-tile">
                                        <span class="info-tile-label">Book title</span>
                                        <span class="info-tile-value" id="stickerBookTitle"></span>
                                    </div>
                                </div>
                                <div class="col-12">
                                    <div class="info-tile">
                                        <span class="info-tile-label">ISBN</span>
                                        <span class="info-tile-value" id="stickerBookIsbn"></span>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="d-flex flex-wrap gap-2 mt-3">
                            <button class="btn btn-brand" id="downloadStickerBtn" type="button" disabled>
                                <i class="bi bi-download me-2"></i>Download sticker PNG
                            </button>
                            <button class="btn btn-warm" id="printStickerBtn" type="button" disabled>
                                <i class="bi bi-printer me-2"></i>Print sticker
                            </button>
                        </div>
                        <p class="muted-text mt-3 mb-0" style="font-size:.82rem;">
                            <i class="bi bi-info-circle me-1"></i>
                            The barcode has been saved. This QR code encodes the barcode value and can be scanned at the circulation desk.
                        </p>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="bookQrModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg">
                <div class="modal-header modal-header-brand">
                    <div>
                        <span class="modal-kicker">Book QR Label</span>
                        <h2 class="h4 mb-1 mt-2">Scan-ready catalog code</h2>
                        <p class="modal-subtitle mb-0">Each title now has a QR label that resolves to its saved barcode or ISBN for fast lookup and desk-side scanning.</p>
                    </div>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="qr-card">
                        <div class="qr-code-shell mb-3" id="bookQrCanvas"></div>
                        <div class="qr-code-meta">
                            <div>
                                <span class="info-tile-label">Encoded value</span>
                                <span class="qr-code-value" id="bookQrValue">No code selected yet.</span>
                            </div>
                            <div class="row g-2 mt-1">
                                <div class="col-12">
                                    <div class="info-tile">
                                        <span class="info-tile-label">Title</span>
                                        <span class="info-tile-value" id="bookQrTitle">Not selected</span>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="info-tile">
                                        <span class="info-tile-label">ISBN</span>
                                        <span class="info-tile-value" id="bookQrIsbn">Not selected</span>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="info-tile">
                                        <span class="info-tile-label">Code type</span>
                                        <span class="info-tile-value" id="bookQrType">Not selected</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="d-flex flex-wrap gap-2 mt-3">
                            <button class="btn btn-brand" id="downloadBookQrButton" type="button" disabled>Download PNG</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<div class="modal fade" id="adminBarcodeScannerModal" tabindex="-1" aria-labelledby="adminBarcodeScannerModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header modal-header-brand">
                <div>
                    <span class="modal-kicker">Barcode Registration</span>
                    <h2 class="h4 mb-1 mt-2" id="adminBarcodeScannerModalLabel">Scan the physical book barcode</h2>
                    <p class="modal-subtitle mb-0">Point your camera at the barcode on the physical book. The code will be registered automatically.</p>
                </div>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4">
                <div class="scanner-shell">
                    <video id="adminBarcodeVideo" autoplay muted playsinline></video>
                    <div class="scanner-overlay"></div>
                    <div class="scanner-target"></div>
                </div>
                <div class="scanner-status" id="adminBarcodeStatus">
                    Camera scanner is preparing. Hold the barcode steady inside the highlighted frame.
                </div>
                <div class="mt-3 pt-3 border-top">
                    <p class="muted-text mb-2" style="font-size:.85rem;">Camera not working? Take a photo of the barcode and upload it instead.</p>
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                        <label class="btn btn-warm mb-0" for="adminBarcodeImageUpload" style="cursor:pointer;">
                            <i class="bi bi-image me-1"></i>Upload barcode photo
                        </label>
                        <input type="file" id="adminBarcodeImageUpload" accept="image/*" capture="environment" style="display:none;">
                        <span class="muted-text" id="adminUploadStatus" style="font-size:.85rem;"></span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<div class="modal fade" id="bookFormModal" tabindex="-1" aria-labelledby="bookFormModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable book-form-modal-dialog">
        <div class="modal-content">
            <div class="modal-header modal-header-brand">
                <div>
                    <div class="modal-kicker">Book Management</div>
                    <h2 class="modal-title h4 mb-1" id="bookFormModalLabel">
                        <c:choose>
                            <c:when test="${not empty editBook}">Edit book record</c:when>
                            <c:otherwise>Add book to the library</c:otherwise>
                        </c:choose>
                    </h2>
                    <p class="modal-subtitle mb-0">Create new catalog entries, update bibliographic details, adjust inventory totals, and maintain digital resource information.</p>
                </div>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <form method="post" action="${bookFormAction}" class="row g-3" enctype="multipart/form-data">
                <div class="modal-body">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <input type="hidden" name="page" value="${booksPage.page}">

                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label" for="title">Title</label>
                            <input class="form-control" id="title" name="title" value="${editBook.title}" required>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label" for="isbn">ISBN</label>
                            <input class="form-control" id="isbn" name="isbn" value="${editBook.isbn}" required>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label" for="barcode">Barcode</label>
                            <div class="input-group">
                                <input class="form-control" id="barcode" name="barcode" value="${editBook.barcode}" placeholder="Type or scan">
                                <button class="btn btn-outline-secondary" type="button" id="openBarcodeScannerBtn" title="Scan physical barcode with camera" aria-label="Scan barcode">
                                    <i class="bi bi-upc-scan"></i>
                                </button>
                            </div>
                            <div class="form-text">Scan the physical book to register its barcode.</div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="categoryId">Category</label>
                            <select class="form-select" id="categoryId" name="categoryId" data-searchable>
                                <option value="">Select category</option>
                                <c:forEach items="${categories}" var="category">
                                    <option value="${category.id}" <c:if test="${not empty editBook and not empty editBook.category and editBook.category.id == category.id}">selected</c:if>>
                                        ${category.name}
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="authorId">Author</label>
                            <select class="form-select" id="authorId" name="authorId" data-searchable>
                                <option value="">Select author</option>
                                <c:forEach items="${authors}" var="author">
                                    <option value="${author.id}" <c:if test="${not empty editBook and not empty editBook.author and editBook.author.id == author.id}">selected</c:if>>
                                        ${author.name}
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-md-2">
                            <label class="form-label" for="publicationYear">Year</label>
                            <input class="form-control" id="publicationYear" name="publicationYear" type="number" value="${editBook.publicationYear}">
                        </div>
                        <div class="col-md-2">
                            <label class="form-label" for="quantity">Quantity</label>
                            <input class="form-control" id="quantity" name="quantity" type="number" min="1" value="${not empty editBook ? editBook.quantity : 1}">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="shelfLocation">Shelf location</label>
                            <input class="form-control" id="shelfLocation" name="shelfLocation" value="${editBook.shelfLocation}">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="coverImageFile">Book cover</label>
                            <input class="form-control" id="coverImageFile" name="coverImageFile" type="file" accept="image/png,image/jpeg,image/webp,.png,.jpg,.jpeg,.webp">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="ebookPath">E-book path or external link</label>
                            <input class="form-control" id="ebookPath" name="ebookPath" value="${editBook.ebookPath}" placeholder="Optional PDF path, EbookHub URL, or Google Drive link">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="ebookFile">Upload PDF</label>
                            <input class="form-control" id="ebookFile" name="ebookFile" type="file" accept="application/pdf,.pdf">
                        </div>
                        <div class="col-12">
                            <small class="muted-text">You can upload a local PDF or paste an external `http/https` reader link such as EbookHub or a Google Drive preview/PDF URL. Cover upload is optional but recommended.</small>
                        </div>
                        <div class="col-md-3 d-flex align-items-end">
                            <div class="form-check">
                                <input class="form-check-input" id="digital" name="digital" type="checkbox" value="true" <c:if test="${not empty editBook and editBook.digital}">checked</c:if>>
                                <label class="form-check-label" for="digital">Digital copy available</label>
                            </div>
                        </div>
                        <div class="col-md-4 d-flex align-items-end">
                            <div class="form-check">
                                <input class="form-check-input" id="visibleInCatalog" name="visibleInCatalog" type="checkbox" value="true" <c:if test="${empty editBook or editBook.visibleInCatalog}">checked</c:if> <c:if test="${not empty editBook and editBook.archived}">disabled</c:if>>
                                <label class="form-check-label" for="visibleInCatalog">Show this book in the student catalog</label>
                            </div>
                        </div>
                        <c:if test="${not empty editBook and editBook.archived}">
                            <div class="col-12">
                                <small class="text-danger">Archived books stay hidden from the student catalog until restored.</small>
                            </div>
                        </c:if>
                        <div class="col-12">
                            <label class="form-label" for="description">Description</label>
                            <textarea class="form-control book-form-description" id="description" name="description" rows="5">${editBook.description}</textarea>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <c:if test="${not empty editBook}">
                        <a class="btn btn-warm me-auto" href="${pageContext.request.contextPath}/admin/books?page=${booksPage.page}">Back to add mode</a>
                    </c:if>
                    <button class="btn btn-warm" type="button" data-bs-dismiss="modal">Close</button>
                    <button class="btn btn-brand" type="submit">
                        <i class="bi bi-journal-plus me-2"></i>
                        <c:choose>
                            <c:when test="${not empty editBook}">Update book</c:when>
                            <c:otherwise>Save book</c:otherwise>
                        </c:choose>
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="https://unpkg.com/@zxing/browser@0.1.5"></script>
<script src="${pageContext.request.contextPath}/vendor/qrious.min.js"></script>
<script src="${pageContext.request.contextPath}/js/qr-tools.js"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
<script>
    (function () {
        var shouldOpenBookModal = ${not empty editBook or openBookModal ? 'true' : 'false'};
        var bookFormModal = document.getElementById("bookFormModal");
        var qrModalElement = document.getElementById("bookQrModal");
        var qrCanvasElement = document.getElementById("bookQrCanvas");
        var qrValueElement = document.getElementById("bookQrValue");
        var qrTitleElement = document.getElementById("bookQrTitle");
        var qrIsbnElement = document.getElementById("bookQrIsbn");
        var qrTypeElement = document.getElementById("bookQrType");
        var downloadButton = document.getElementById("downloadBookQrButton");
        var currentQrCanvas = null;

        if (shouldOpenBookModal && bookFormModal) {
            bootstrap.Modal.getOrCreateInstance(bookFormModal).show();
        }

        qrModalElement.addEventListener("show.bs.modal", function (event) {
            var trigger = event.relatedTarget;
            var bookCode = trigger.getAttribute("data-book-code") || "";
            var bookTitle = trigger.getAttribute("data-book-title") || "Not selected";
            var bookIsbn = trigger.getAttribute("data-book-isbn") || "Not selected";
            var bookCodeLabel = trigger.getAttribute("data-book-code-label") || "Book code";

            qrValueElement.textContent = bookCode;
            qrTitleElement.textContent = bookTitle;
            qrIsbnElement.textContent = bookIsbn;
            qrTypeElement.textContent = bookCodeLabel;
            currentQrCanvas = window.LatteAndLettersQr.renderQr(qrCanvasElement, bookCode, {
                size: 240,
                emptyText: "No QR code available for this book.",
                errorText: "Unable to render this QR code."
            });
            downloadButton.disabled = !currentQrCanvas;
            downloadButton.dataset.filename = window.LatteAndLettersQr.normalizeFilename(bookTitle, "book") + "-qr.png";
        });

        downloadButton.addEventListener("click", function () {
            if (!currentQrCanvas) {
                return;
            }

            window.LatteAndLettersQr.downloadCanvas(currentQrCanvas, downloadButton.dataset.filename);
        });
    })();
</script>
<script>
    (function () {
        var scannerModalElement = document.getElementById("adminBarcodeScannerModal");
        var barcodeInput = document.getElementById("barcode");
        var openScannerBtn = document.getElementById("openBarcodeScannerBtn");

        var scanner = window.LatteAndLettersQr.createScanner({
            videoElement: document.getElementById("adminBarcodeVideo"),
            statusElement: document.getElementById("adminBarcodeStatus"),
            formats: ["code_128", "ean_13", "ean_8", "upc_a", "upc_e", "code_39", "codabar", "itf"],
            liveMessage: "Scanner is live. Align the barcode inside the frame and hold still.",
            unsupportedMessage: "This browser cannot decode live barcodes. You can still type the barcode manually.",
            permissionMessage: "Camera access was blocked. Please allow camera access and try again.",
            onDetected: function (rawValue) {
                var detectedCode = (rawValue || "").trim();
                if (!detectedCode) {
                    return;
                }

                // Fill the barcode field
                barcodeInput.value = detectedCode;
                barcodeInput.dispatchEvent(new Event("input", { bubbles: true }));

                scanner.setStatus("Barcode registered: " + detectedCode, false);

                // Close scanner modal and reopen the book form modal
                var scannerModal = bootstrap.Modal.getInstance(scannerModalElement);
                if (scannerModal) {
                    scannerModal.hide();
                }
            },
            onScanError: function () {
                scanner.setStatus("Scanning... Hold the barcode steady inside the frame.", false);
            }
        });

        // Open scanner: hide book form modal first, then show scanner modal
        openScannerBtn.addEventListener("click", function () {
            var bookFormModal = bootstrap.Modal.getInstance(document.getElementById("bookFormModal"));
            if (bookFormModal) {
                bookFormModal.hide();
            }
            window.setTimeout(function () {
                bootstrap.Modal.getOrCreateInstance(scannerModalElement).show();
            }, 300);
        });

        scannerModalElement.addEventListener("shown.bs.modal", function () {
            scanner.start();
        });

        // When scanner modal closes, stop camera and reopen the book form
        scannerModalElement.addEventListener("hidden.bs.modal", function () {
            scanner.stop();
            scanner.setStatus("Camera scanner is preparing. Hold the barcode steady inside the highlighted frame.", false);
            var bookFormModal = bootstrap.Modal.getOrCreateInstance(document.getElementById("bookFormModal"));
            bookFormModal.show();
        });

        // Image upload fallback
        var adminUploadInput = document.getElementById("adminBarcodeImageUpload");
        var adminUploadStatus = document.getElementById("adminUploadStatus");
        adminUploadInput.addEventListener("change", function () {
            var file = adminUploadInput.files && adminUploadInput.files[0];
            if (!file) {
                return;
            }
            adminUploadStatus.textContent = "Reading barcode from image...";
            window.LatteAndLettersQr.decodeBarcodeFromImageFile(file)
                .then(function (code) {
                    adminUploadStatus.textContent = "";
                    adminUploadInput.value = "";
                    barcodeInput.value = code;
                    barcodeInput.dispatchEvent(new Event("input", { bubbles: true }));
                    var scannerModal = bootstrap.Modal.getInstance(scannerModalElement);
                    if (scannerModal) {
                        scannerModal.hide();
                    }
                })
                .catch(function (err) {
                    adminUploadStatus.textContent = err && err.message ? err.message : "Could not read barcode from image.";
                    adminUploadInput.value = "";
                });
        });
    })();
</script>
<script>
    // ── Generate Barcode buttons ──────────────────────────────────────────
    (function () {
        var csrfToken   = document.querySelector("meta[name='_csrf']")
                          ? document.querySelector("meta[name='_csrf']").getAttribute("content")
                          : null;
        var csrfHeader  = document.querySelector("meta[name='_csrf_header']")
                          ? document.querySelector("meta[name='_csrf_header']").getAttribute("content")
                          : "X-CSRF-TOKEN";

        var stickerModal      = document.getElementById("barcodeStickerModal");
        var stickerQrCanvas   = document.getElementById("stickerQrCanvas");
        var stickerBarcodeVal = document.getElementById("stickerBarcodeValue");
        var stickerTitle      = document.getElementById("stickerBookTitle");
        var stickerIsbn       = document.getElementById("stickerBookIsbn");
        var downloadStickerBtn = document.getElementById("downloadStickerBtn");
        var printStickerBtn    = document.getElementById("printStickerBtn");
        var currentStickerCanvas = null;

        function openStickerModal(data) {
            stickerBarcodeVal.textContent = data.barcode;
            stickerTitle.textContent      = data.title;
            stickerIsbn.textContent       = data.isbn;

            currentStickerCanvas = window.LatteAndLettersQr.renderQr(stickerQrCanvas, data.barcode, {
                size: 240,
                emptyText: "Unable to render QR.",
                errorText: "Unable to render QR."
            });

            downloadStickerBtn.disabled = !currentStickerCanvas;
            printStickerBtn.disabled    = !currentStickerCanvas;
            downloadStickerBtn.dataset.filename =
                window.LatteAndLettersQr.normalizeFilename(data.title, "barcode-sticker") + ".png";

            bootstrap.Modal.getOrCreateInstance(stickerModal).show();
        }

        downloadStickerBtn.addEventListener("click", function () {
            if (currentStickerCanvas) {
                window.LatteAndLettersQr.downloadCanvas(currentStickerCanvas, downloadStickerBtn.dataset.filename);
            }
        });

        printStickerBtn.addEventListener("click", function () {
            if (!currentStickerCanvas) return;
            var dataUrl = currentStickerCanvas.toDataURL("image/png");
            var win = window.open("", "_blank", "width=400,height=500");
            win.document.write(
                "<html><head><title>Barcode Sticker</title>"
                + "<style>body{margin:0;display:flex;flex-direction:column;align-items:center;"
                + "justify-content:center;min-height:100vh;font-family:sans-serif;}"
                + "img{max-width:240px;}"
                + ".label{margin-top:10px;font-size:13px;font-weight:700;letter-spacing:.06em;text-align:center;}"
                + ".sub{font-size:11px;color:#555;text-align:center;margin-top:4px;}"
                + "@media print{button{display:none;}}"
                + "</style></head><body>"
                + "<img src='" + dataUrl + "' alt='Barcode QR'>"
                + "<div class='label'>" + stickerBarcodeVal.textContent + "</div>"
                + "<div class='sub'>" + stickerTitle.textContent + "</div>"
                + "<div class='sub'>ISBN: " + stickerIsbn.textContent + "</div>"
                + "<br><button onclick='window.print()' style='padding:8px 20px;cursor:pointer;'>Print</button>"
                + "</body></html>"
            );
            win.document.close();
        });

        // Delegate click on all "Generate barcode" buttons
        document.addEventListener("click", function (e) {
            var btn = e.target.closest(".generate-barcode-btn");
            if (!btn) return;

            var bookId    = btn.dataset.bookId;
            var bookTitle = btn.dataset.bookTitle;

            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Generating...';

            var headers = { "Content-Type": "application/json" };
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            fetch("${pageContext.request.contextPath}/admin/books/" + bookId + "/generate-barcode", {
                method: "POST",
                headers: headers
            })
            .then(function (res) { return res.json(); })
            .then(function (data) {
                if (!data.success) {
                    alert(data.message || "Failed to generate barcode.");
                    btn.disabled = false;
                    btn.innerHTML = '<i class="bi bi-upc me-1"></i>Generate barcode';
                    return;
                }
                // Replace the button cell with the barcode text (no reload needed)
                var cell = btn.closest("td");
                if (cell) {
                    var barcodeDiv = document.createElement("div");
                    barcodeDiv.className = "muted-text";
                    barcodeDiv.textContent = data.barcode;
                    btn.replaceWith(barcodeDiv);
                }
                // Open the sticker modal so admin can print immediately
                openStickerModal(data);
            })
            .catch(function () {
                alert("Network error. Please try again.");
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-upc me-1"></i>Generate barcode';
            });
        });

        // Wire up CSRF meta tags if Spring Security injects them
        // (add <meta name="_csrf" content="${_csrf.token}"> to <head> if not already present)
    })();
</script>
<script>
    // ── Archive / Restore / Delete confirmation via SweetAlert2 ──────────
    (function () {
        var archivedBooksModalEl = document.getElementById("archivedBooksModal");
        var archivedBooksModal = bootstrap.Modal.getOrCreateInstance(archivedBooksModalEl);

        var swalBase = Swal.mixin({
            customClass: {
                confirmButton: 'btn btn-brand ms-2',
                cancelButton: 'btn btn-warm'
            },
            buttonsStyling: false,
            reverseButtons: false
        });

        document.addEventListener("click", function (e) {
            var btn = e.target.closest("[data-action]");
            if (!btn) return;
            e.preventDefault();

            var action    = btn.dataset.action;
            var bookId    = btn.dataset.bookId;
            var bookTitle = btn.dataset.bookTitle;

            if (action === "archive") {
                var form = document.getElementById("archiveForm-" + bookId);
                swalBase.fire({
                    title: "Archive this book?",
                    html: "<strong>" + bookTitle + "</strong><br><span style='font-size:.9rem;color:#617567;'>It will be hidden from the student catalog. You can restore it anytime.</span>",
                    icon: "warning",
                    showCancelButton: true,
                    confirmButtonText: "Archive",
                    cancelButtonText: "Cancel"
                }).then(function (result) {
                    if (result.isConfirmed) form.submit();
                });

            } else if (action === "restore-archived") {
                var form = document.getElementById("restoreArchivedForm-" + bookId);
                archivedBooksModal.hide();
                swalBase.fire({
                    title: "Restore this book?",
                    html: "<strong>" + bookTitle + "</strong><br><span style='font-size:.9rem;color:#617567;'>It will be visible again in the student catalog.</span>",
                    icon: "question",
                    showCancelButton: true,
                    confirmButtonText: "Restore",
                    cancelButtonText: "Cancel"
                }).then(function (result) {
                    if (result.isConfirmed) {
                        form.submit();
                    } else {
                        archivedBooksModal.show();
                    }
                });

            } else if (action === "permanent-delete") {
                var form = document.getElementById("deleteForm-" + bookId);
                archivedBooksModal.hide();
                swalBase.fire({
                    title: "Delete permanently?",
                    html: "<strong>" + bookTitle + "</strong><br><span style='font-size:.9rem;color:#617567;'>This cannot be undone. Books with borrow history cannot be deleted.</span>",
                    icon: "error",
                    showCancelButton: true,
                    confirmButtonText: "Delete permanently",
                    cancelButtonText: "Cancel",
                    customClass: {
                        confirmButton: 'btn btn-danger ms-2',
                        cancelButton: 'btn btn-warm'
                    }
                }).then(function (result) {
                    if (result.isConfirmed) {
                        form.submit();
                    } else {
                        archivedBooksModal.show();
                    }
                });
            }
        });
    })();
</script>
<script>
    // Searchable select — enhances any <select data-searchable> into a type-to-filter combobox
    (function () {
        var style = document.createElement("style");
        style.textContent = [
            ".searchable-wrap{position:relative;}",
            ".searchable-input{width:100%;padding:.375rem 2rem .375rem .75rem;border:1px solid #dee2e6;border-radius:.375rem;font-size:1rem;line-height:1.5;background:#fff url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 16 16'%3E%3Cpath fill='none' stroke='%23343a40' stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='m2 5 6 6 6-6'/%3E%3C/svg%3E\") no-repeat right .75rem center/16px 12px;appearance:none;cursor:pointer;}",
            ".searchable-input:focus{outline:0;border-color:#86b7fe;box-shadow:0 0 0 .25rem rgba(13,110,253,.25);}",
            ".searchable-dropdown{position:absolute;top:100%;left:0;right:0;z-index:1055;background:#fff;border:1px solid #dee2e6;border-top:none;border-radius:0 0 .375rem .375rem;max-height:220px;overflow-y:auto;box-shadow:0 4px 12px rgba(0,0,0,.1);display:none;}",
            ".searchable-dropdown.open{display:block;}",
            ".searchable-option{padding:.45rem .75rem;cursor:pointer;font-size:.95rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}",
            ".searchable-option:hover,.searchable-option.focused{background:#f0f7f2;color:#0f7f34;}",
            ".searchable-option.selected{font-weight:700;color:#0f7f34;}",
            ".searchable-option.hidden{display:none;}",
            ".searchable-no-results{padding:.45rem .75rem;color:#6c757d;font-size:.9rem;font-style:italic;}"
        ].join("");
        document.head.appendChild(style);

        function initSearchableSelect(select) {
            var wrap = document.createElement("div");
            wrap.className = "searchable-wrap";
            select.parentNode.insertBefore(wrap, select);
            wrap.appendChild(select);
            select.style.display = "none";

            var input = document.createElement("input");
            input.type = "text";
            input.className = "searchable-input";
            input.setAttribute("autocomplete", "off");
            input.setAttribute("spellcheck", "false");
            input.setAttribute("aria-haspopup", "listbox");
            input.setAttribute("aria-expanded", "false");
            input.setAttribute("role", "combobox");
            wrap.insertBefore(input, select);

            var dropdown = document.createElement("div");
            dropdown.className = "searchable-dropdown";
            dropdown.setAttribute("role", "listbox");
            wrap.appendChild(dropdown);

            var options = [];
            var focusedIndex = -1;

            // Build option list from the original select
            function buildOptions() {
                options = [];
                dropdown.innerHTML = "";
                var selectOptions = select.options;
                for (var i = 0; i < selectOptions.length; i++) {
                    var opt = selectOptions[i];
                    if (!opt.value) continue; // skip placeholder
                    var div = document.createElement("div");
                    div.className = "searchable-option";
                    div.textContent = opt.text.trim();
                    div.dataset.value = opt.value;
                    div.setAttribute("role", "option");
                    options.push({ el: div, text: opt.text.trim().toLowerCase(), value: opt.value });
                    dropdown.appendChild(div);
                }
            }

            function getSelectedText() {
                var sel = select.options[select.selectedIndex];
                return sel && sel.value ? sel.text.trim() : "";
            }

            function syncInputToSelect() {
                input.value = getSelectedText();
                markSelected();
            }

            function markSelected() {
                options.forEach(function (o) {
                    o.el.classList.toggle("selected", o.value === select.value);
                });
            }

            function openDropdown() {
                buildOptions();
                filterOptions(input.value);
                dropdown.classList.add("open");
                input.setAttribute("aria-expanded", "true");
                focusedIndex = -1;
                scrollToSelected();
            }

            function closeDropdown() {
                dropdown.classList.remove("open");
                input.setAttribute("aria-expanded", "false");
                focusedIndex = -1;
                syncInputToSelect();
            }

            function scrollToSelected() {
                var sel = dropdown.querySelector(".selected:not(.hidden)");
                if (sel) sel.scrollIntoView({ block: "nearest" });
            }

            function filterOptions(query) {
                var q = (query || "").toLowerCase().trim();
                var visibleCount = 0;
                focusedIndex = -1;
                options.forEach(function (o) {
                    var match = !q || o.text.indexOf(q) !== -1;
                    o.el.classList.toggle("hidden", !match);
                    if (match) visibleCount++;
                });
                var noResults = dropdown.querySelector(".searchable-no-results");
                if (visibleCount === 0) {
                    if (!noResults) {
                        noResults = document.createElement("div");
                        noResults.className = "searchable-no-results";
                        noResults.textContent = "No results found.";
                        dropdown.appendChild(noResults);
                    }
                    noResults.style.display = "";
                } else if (noResults) {
                    noResults.style.display = "none";
                }
            }

            function selectOption(value, text) {
                select.value = value;
                input.value = text;
                closeDropdown();
                select.dispatchEvent(new Event("change", { bubbles: true }));
            }

            function moveFocus(dir) {
                var visible = options.filter(function (o) { return !o.el.classList.contains("hidden"); });
                if (!visible.length) return;
                options.forEach(function (o) { o.el.classList.remove("focused"); });
                focusedIndex = Math.max(0, Math.min(visible.length - 1, focusedIndex + dir));
                visible[focusedIndex].el.classList.add("focused");
                visible[focusedIndex].el.scrollIntoView({ block: "nearest" });
            }

            // Events
            input.addEventListener("click", function () {
                if (dropdown.classList.contains("open")) {
                    closeDropdown();
                } else {
                    input.select();
                    openDropdown();
                }
            });

            input.addEventListener("input", function () {
                if (!dropdown.classList.contains("open")) {
                    dropdown.classList.add("open");
                    input.setAttribute("aria-expanded", "true");
                }
                filterOptions(input.value);
            });

            input.addEventListener("keydown", function (e) {
                if (e.key === "ArrowDown") { e.preventDefault(); if (!dropdown.classList.contains("open")) openDropdown(); moveFocus(1); }
                else if (e.key === "ArrowUp") { e.preventDefault(); moveFocus(-1); }
                else if (e.key === "Enter") {
                    e.preventDefault();
                    var focused = dropdown.querySelector(".focused");
                    if (focused) selectOption(focused.dataset.value, focused.textContent);
                    else closeDropdown();
                }
                else if (e.key === "Escape") { closeDropdown(); }
                else if (e.key === "Tab") { closeDropdown(); }
            });

            dropdown.addEventListener("mousedown", function (e) {
                var opt = e.target.closest(".searchable-option");
                if (opt) {
                    e.preventDefault();
                    selectOption(opt.dataset.value, opt.textContent);
                }
            });

            document.addEventListener("mousedown", function (e) {
                if (!wrap.contains(e.target)) closeDropdown();
            });

            buildOptions();
            syncInputToSelect();
        }

        function initAll() {
            document.querySelectorAll("select[data-searchable]").forEach(function (sel) {
                if (!sel.dataset.searchableInit) {
                    sel.dataset.searchableInit = "true";
                    initSearchableSelect(sel);
                }
            });
        }

        // Run on DOM ready and also after modals open (selects may be inside modals)
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", initAll);
        } else {
            initAll();
        }

        // Re-init when Bootstrap modals open (in case selects are inside modals)
        document.addEventListener("shown.bs.modal", function () {
            initAll();
        });
    })();
</script>
</body>
</html>


