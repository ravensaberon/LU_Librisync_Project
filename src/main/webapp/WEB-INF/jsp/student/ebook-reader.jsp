<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>E-Book Reader</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Digital Library</span>
            <div class="brand-title mt-2">Read e-book</div>
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

    <section class="hero-card mb-4">
        <div class="hero-card-grid">
            <div>
                <h1 class="fw-bold mb-2">${book.title}</h1>
                <p class="muted-text mb-0">ISBN: ${book.isbn} | Author: ${book.author.name} | Category: ${book.category.name}</p>
            </div>
            <div class="hero-side-note">
                <span class="hero-side-title">Reader memory</span>
                <strong class="hero-side-value" id="readerResumePage">Page 1</strong>
                <span class="hero-side-caption">Your last page is saved automatically so you can continue reading after going back.</span>
            </div>
        </div>
    </section>

    <section class="panel-card">
        <div class="ebook-reader-shell" data-book-id="${book.id}" data-pdf-url="${pageContext.request.contextPath}/student/ebooks/${book.id}/content">
            <div class="ebook-reader-status" id="readerStatus">Preparing your reader...</div>

            <div class="ebook-reader-stage">
                <canvas id="readerCanvas"></canvas>
            </div>

            <div class="ebook-reader-toolbar">
                <div class="ebook-reader-toolbar-group">
                    <button class="btn btn-warm" id="readerPrevPage" type="button"><i class="bi bi-chevron-left"></i> Prev</button>
                    <button class="btn btn-warm" id="readerNextPage" type="button">Next <i class="bi bi-chevron-right"></i></button>
                </div>
                <div class="ebook-reader-toolbar-group">
                    <label class="ebook-reader-label" for="readerPageInput">Page</label>
                    <input class="form-control ebook-reader-page-input" id="readerPageInput" type="number" min="1" value="1">
                    <span class="ebook-reader-label" id="readerPageCount">of 1</span>
                </div>
                <div class="ebook-reader-toolbar-group">
                    <button class="btn btn-warm" id="readerZoomOut" type="button"><i class="bi bi-dash-lg"></i></button>
                    <span class="ebook-reader-label" id="readerZoomLabel">100%</span>
                    <button class="btn btn-warm" id="readerZoomIn" type="button"><i class="bi bi-plus-lg"></i></button>
                </div>
            </div>
        </div>
    </section>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
<script type="module">
    import * as pdfjsLib from "https://cdn.jsdelivr.net/npm/pdfjs-dist@5.6.205/build/pdf.min.mjs";

    pdfjsLib.GlobalWorkerOptions.workerSrc = "https://cdn.jsdelivr.net/npm/pdfjs-dist@5.6.205/build/pdf.worker.min.mjs";

    document.addEventListener("DOMContentLoaded", async function () {
        const readerShell = document.querySelector(".ebook-reader-shell");
        if (!readerShell) {
            return;
        }

        const storageKey = "latte-and-letters-reader-" + readerShell.dataset.bookId;
        const pdfUrl = readerShell.dataset.pdfUrl;
        const canvas = document.getElementById("readerCanvas");
        const context = canvas.getContext("2d");
        const status = document.getElementById("readerStatus");
        const resumePageLabel = document.getElementById("readerResumePage");
        const pageInput = document.getElementById("readerPageInput");
        const pageCount = document.getElementById("readerPageCount");
        const zoomLabel = document.getElementById("readerZoomLabel");
        const prevPageButton = document.getElementById("readerPrevPage");
        const nextPageButton = document.getElementById("readerNextPage");
        const zoomInButton = document.getElementById("readerZoomIn");
        const zoomOutButton = document.getElementById("readerZoomOut");

        let pdfDocument = null;
        let currentPage = 1;
        let totalPages = 1;
        let currentScale = 1.2;
        let isRendering = false;
        let pendingPage = null;

        function readSavedProgress() {
            try {
                return JSON.parse(window.localStorage.getItem(storageKey) || "{}");
            } catch (error) {
                return {};
            }
        }

        function saveProgress() {
            window.localStorage.setItem(storageKey, JSON.stringify({
                page: currentPage,
                scale: currentScale
            }));
        }

        function syncToolbar() {
            pageInput.value = currentPage;
            pageInput.max = totalPages;
            pageCount.textContent = "of " + totalPages;
            zoomLabel.textContent = Math.round(currentScale * 100) + "%";
            resumePageLabel.textContent = "Page " + currentPage;
            prevPageButton.disabled = currentPage <= 1;
            nextPageButton.disabled = currentPage >= totalPages;
        }

        async function renderPage(pageNumber) {
            if (!pdfDocument) {
                return;
            }

            isRendering = true;
            status.textContent = "Rendering page " + pageNumber + "...";

            const page = await pdfDocument.getPage(pageNumber);
            const viewport = page.getViewport({ scale: currentScale });
            const outputScale = window.devicePixelRatio || 1;

            canvas.width = Math.floor(viewport.width * outputScale);
            canvas.height = Math.floor(viewport.height * outputScale);
            canvas.style.width = Math.floor(viewport.width) + "px";
            canvas.style.height = Math.floor(viewport.height) + "px";

            const renderContext = {
                canvasContext: context,
                viewport: viewport,
                transform: outputScale !== 1 ? [outputScale, 0, 0, outputScale, 0, 0] : null
            };

            await page.render(renderContext).promise;
            isRendering = false;
            currentPage = pageNumber;
            syncToolbar();
            saveProgress();
            status.textContent = "Page " + currentPage + " ready.";

            if (pendingPage !== null && pendingPage !== currentPage) {
                const nextPendingPage = pendingPage;
                pendingPage = null;
                await queueRender(nextPendingPage);
            }
        }

        async function queueRender(pageNumber) {
            const safePage = Math.max(1, Math.min(totalPages, pageNumber));
            if (isRendering) {
                pendingPage = safePage;
                return;
            }
            await renderPage(safePage);
        }

        function updateScale(nextScale) {
            currentScale = Math.max(0.8, Math.min(2.4, nextScale));
            queueRender(currentPage);
        }

        prevPageButton.addEventListener("click", function () {
            queueRender(currentPage - 1);
        });

        nextPageButton.addEventListener("click", function () {
            queueRender(currentPage + 1);
        });

        pageInput.addEventListener("change", function () {
            const requestedPage = Number(pageInput.value || currentPage);
            queueRender(requestedPage);
        });

        zoomInButton.addEventListener("click", function () {
            updateScale(currentScale + 0.15);
        });

        zoomOutButton.addEventListener("click", function () {
            updateScale(currentScale - 0.15);
        });

        try {
            status.textContent = "Loading digital copy...";
            const savedProgress = readSavedProgress();
            if (savedProgress.scale) {
                currentScale = Math.max(0.8, Math.min(2.4, Number(savedProgress.scale)));
            }

            pdfDocument = await pdfjsLib.getDocument(pdfUrl).promise;
            totalPages = pdfDocument.numPages;
            currentPage = Math.max(1, Math.min(totalPages, Number(savedProgress.page || 1)));
            syncToolbar();
            await renderPage(currentPage);
        } catch (error) {
            status.textContent = "Unable to load this PDF right now.";
        }
    });
</script>
</body>
</html>

