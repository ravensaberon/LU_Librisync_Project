<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Categories and Authors</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260505-reference-refresh">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Reference Data</span>
            <div class="brand-title mt-2">Manage Categories and Authors</div>
        </div>
        <div class="nav-links">
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/dashboard">Dashboard</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/books">Books</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/issues">Issue / Return</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/reservations">Reservations</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/students">Students</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/fines">Fines</a>
            <a class="nav-pill" href="${pageContext.request.contextPath}/admin/reports">Reports</a>
            <a class="nav-pill active" href="${pageContext.request.contextPath}/admin/references">Categories / Authors</a>
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

    <c:set var="categoryFormAction" value="${pageContext.request.contextPath}/admin/categories"/>
    <c:if test="${not empty editCategory}">
        <c:set var="categoryFormAction" value="${pageContext.request.contextPath}/admin/categories/${editCategory.id}/update"/>
    </c:if>

    <c:set var="authorFormAction" value="${pageContext.request.contextPath}/admin/authors"/>
    <c:if test="${not empty editAuthor}">
        <c:set var="authorFormAction" value="${pageContext.request.contextPath}/admin/authors/${editAuthor.id}/update"/>
    </c:if>

    <section class="hero-card mb-4">
        <div class="hero-card-grid">
            <div>
                <span class="tag-chip">Reference Workspace</span>
                <h1 class="fw-bold mt-3 mb-2">Shape the Catalog Structure Behind Every Book Record</h1>
                <p class="muted-text mb-4">Add clean subject categories, maintain author profiles, and keep browsing data organized without letting the page turn into a long scroll.</p>
                <div class="reference-hero-actions">
                    <button class="btn btn-light btn-lg" type="button" data-bs-toggle="modal" data-bs-target="#categoryModal">
                        <i class="bi bi-tags me-2"></i>Add Category
                    </button>
                    <button class="btn btn-outline-light btn-lg" type="button" data-bs-toggle="modal" data-bs-target="#authorModal">
                        <i class="bi bi-person-vcard me-2"></i>Add Author
                    </button>
                </div>
            </div>
            <div class="hero-side-note">
                <div class="hero-side-title">Reference Snapshot</div>
                <strong class="hero-side-value">${categoryCount + authorCount}</strong>
                <span class="hero-side-caption">${categoryCount} Categories and ${authorCount} Authors Ready for Catalog Use.</span>
            </div>
        </div>
    </section>

    <section class="stat-grid mb-4">
        <div class="metric-card">
            <div class="metric-value">${categoryCount}</div>
            <div class="metric-label">Total Categories</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${authorCount}</div>
            <div class="metric-label">Total Authors</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${categoriesPage.totalPages}</div>
            <div class="metric-label">Category Pages</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${authorsPage.totalPages}</div>
            <div class="metric-label">Author Pages</div>
        </div>
    </section>

    <section class="panel-card">
        <div class="dashboard-tab-shell" data-reference-tab-shell data-reference-initial-tab="${activeReferenceTab}">
            <div class="dashboard-tab-nav" role="tablist" aria-label="Reference data views">
                <button class="dashboard-tab-button" type="button" role="tab" id="reference-categories-tab" aria-controls="reference-categories-panel" aria-selected="false" data-reference-tab-button data-reference-tab-target="reference-categories-panel" data-reference-tab-key="categories">
                    <i class="bi bi-tags"></i>
                    <span>Categories</span>
                </button>
                <button class="dashboard-tab-button" type="button" role="tab" id="reference-authors-tab" aria-controls="reference-authors-panel" aria-selected="false" data-reference-tab-button data-reference-tab-target="reference-authors-panel" data-reference-tab-key="authors">
                    <i class="bi bi-person-vcard"></i>
                    <span>Authors</span>
                </button>
            </div>

            <div class="dashboard-tab-panel" id="reference-categories-panel" role="tabpanel" aria-labelledby="reference-categories-tab" data-reference-tab-panel hidden>
                <div class="table-search-header">
                    <div>
                        <div class="section-title mb-1">Categories</div>
                        <p class="helper-copy mb-0">Subject groupings used across search, catalog organization, and reporting.</p>
                    </div>
                    <div class="table-search-meta">${categoriesPage.totalItems} Total Records</div>
                </div>
                <div class="table-responsive">
                    <table class="table align-middle">
                        <thead>
                        <tr>
                            <th>Name</th>
                            <th>Description</th>
                            <th>Created</th>
                            <th>Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${categories}" var="category">
                            <tr>
                                <td><strong>${category.name}</strong></td>
                                <td class="muted-text">${category.description}</td>
                                <td>${category.createdAtDisplay}</td>
                                <td class="table-actions">
                                    <a class="icon-action"
                                       href="${pageContext.request.contextPath}/admin/references?activeTab=categories&categoryPage=${categoriesPage.page}&authorPage=${authorsPage.page}&editCategoryId=${category.id}"
                                       title="Edit category">
                                        <i class="bi bi-pencil-square"></i>
                                    </a>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/categories/${category.id}/delete">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                        <input type="hidden" name="activeTab" value="categories">
                                        <input type="hidden" name="categoryPage" value="${categoriesPage.page}">
                                        <input type="hidden" name="authorPage" value="${authorsPage.page}">
                                        <button class="icon-action danger" type="submit" title="Delete category">
                                            <i class="bi bi-trash3"></i>
                                        </button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty categories}">
                            <tr>
                                <td colspan="4" class="text-center muted-text">No categories available yet.</td>
                            </tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
                <c:if test="${categoriesPage.totalPages > 1}">
                    <nav class="mt-4" aria-label="Category pages">
                        <ul class="pagination justify-content-center mb-0">
                            <li class="page-item <c:if test='${!categoriesPage.hasPrevious}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/admin/references?activeTab=categories&categoryPage=${categoriesPage.previousPage}&authorPage=${authorsPage.page}">Previous</a>
                            </li>
                            <c:forEach begin="${categoriesPage.startPage}" end="${categoriesPage.endPage}" var="pageNumber">
                                <li class="page-item <c:if test='${pageNumber == categoriesPage.page}'>active</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/admin/references?activeTab=categories&categoryPage=${pageNumber}&authorPage=${authorsPage.page}">${pageNumber}</a>
                                </li>
                            </c:forEach>
                            <li class="page-item <c:if test='${!categoriesPage.hasNext}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/admin/references?activeTab=categories&categoryPage=${categoriesPage.nextPage}&authorPage=${authorsPage.page}">Next</a>
                            </li>
                        </ul>
                    </nav>
                </c:if>
            </div>

            <div class="dashboard-tab-panel" id="reference-authors-panel" role="tabpanel" aria-labelledby="reference-authors-tab" data-reference-tab-panel hidden>
                <div class="table-search-header">
                    <div>
                        <div class="section-title mb-1">Authors</div>
                        <p class="helper-copy mb-0">Author records that support filtering, book metadata, and collection quality.</p>
                    </div>
                    <div class="table-search-meta">${authorsPage.totalItems} Total Records</div>
                </div>
                <div class="table-responsive">
                    <table class="table align-middle">
                        <thead>
                        <tr>
                            <th>Name</th>
                            <th>Bio</th>
                            <th>Created</th>
                            <th>Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${authors}" var="author">
                            <tr>
                                <td><strong>${author.name}</strong></td>
                                <td class="muted-text">${author.bio}</td>
                                <td>${author.createdAtDisplay}</td>
                                <td class="table-actions">
                                    <a class="icon-action"
                                       href="${pageContext.request.contextPath}/admin/references?activeTab=authors&categoryPage=${categoriesPage.page}&authorPage=${authorsPage.page}&editAuthorId=${author.id}"
                                       title="Edit author">
                                        <i class="bi bi-pencil-square"></i>
                                    </a>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/authors/${author.id}/delete">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                        <input type="hidden" name="activeTab" value="authors">
                                        <input type="hidden" name="categoryPage" value="${categoriesPage.page}">
                                        <input type="hidden" name="authorPage" value="${authorsPage.page}">
                                        <button class="icon-action danger" type="submit" title="Delete author">
                                            <i class="bi bi-trash3"></i>
                                        </button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty authors}">
                            <tr>
                                <td colspan="4" class="text-center muted-text">No authors available yet.</td>
                            </tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
                <c:if test="${authorsPage.totalPages > 1}">
                    <nav class="mt-4" aria-label="Author pages">
                        <ul class="pagination justify-content-center mb-0">
                            <li class="page-item <c:if test='${!authorsPage.hasPrevious}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/admin/references?activeTab=authors&categoryPage=${categoriesPage.page}&authorPage=${authorsPage.previousPage}">Previous</a>
                            </li>
                            <c:forEach begin="${authorsPage.startPage}" end="${authorsPage.endPage}" var="pageNumber">
                                <li class="page-item <c:if test='${pageNumber == authorsPage.page}'>active</c:if>">
                                    <a class="page-link" href="${pageContext.request.contextPath}/admin/references?activeTab=authors&categoryPage=${categoriesPage.page}&authorPage=${pageNumber}">${pageNumber}</a>
                                </li>
                            </c:forEach>
                            <li class="page-item <c:if test='${!authorsPage.hasNext}'>disabled</c:if>">
                                <a class="page-link" href="${pageContext.request.contextPath}/admin/references?activeTab=authors&categoryPage=${categoriesPage.page}&authorPage=${authorsPage.nextPage}">Next</a>
                            </li>
                        </ul>
                    </nav>
                </c:if>
            </div>
        </div>
    </section>
</div>

<div class="modal fade" id="categoryModal" tabindex="-1" aria-labelledby="categoryModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <div>
                    <span class="modal-kicker">Category Form</span>
                    <h2 class="h4 mb-1 mt-2" id="categoryModalLabel">
                        <c:choose>
                            <c:when test="${not empty editCategory}">Edit Category</c:when>
                            <c:otherwise>Add Category</c:otherwise>
                        </c:choose>
                    </h2>
                    <p class="modal-subtitle mb-0">Keep subject groupings organized so catalog search and reporting stay consistent.</p>
                </div>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4">
                <form method="post" action="${categoryFormAction}" class="d-grid gap-3">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <input type="hidden" name="activeTab" value="categories">
                    <input type="hidden" name="categoryPage" value="${categoriesPage.page}">
                    <input type="hidden" name="authorPage" value="${authorsPage.page}">
                    <div>
                        <label class="form-label" for="categoryName">Category Name</label>
                        <input class="form-control" id="categoryName" name="name" value="${editCategory.name}" required>
                    </div>
                    <div>
                        <label class="form-label" for="categoryDescription">Description</label>
                        <textarea class="form-control" id="categoryDescription" name="description" rows="4">${editCategory.description}</textarea>
                    </div>
                    <div class="d-flex flex-wrap gap-2 pt-2">
                        <button class="btn btn-brand" type="submit">
                            <i class="bi bi-tags me-2"></i>
                            <c:choose>
                                <c:when test="${not empty editCategory}">Update Category</c:when>
                                <c:otherwise>Save Category</c:otherwise>
                            </c:choose>
                        </button>
                        <c:if test="${not empty editCategory}">
                            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/admin/references?activeTab=categories&categoryPage=${categoriesPage.page}&authorPage=${authorsPage.page}">Cancel Editing</a>
                        </c:if>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="authorModal" tabindex="-1" aria-labelledby="authorModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <div>
                    <span class="modal-kicker">Author Form</span>
                    <h2 class="h4 mb-1 mt-2" id="authorModalLabel">
                        <c:choose>
                            <c:when test="${not empty editAuthor}">Edit Author</c:when>
                            <c:otherwise>Add Author</c:otherwise>
                        </c:choose>
                    </h2>
                    <p class="modal-subtitle mb-0">Maintain author records to improve catalog quality, search filters, and inventory reporting.</p>
                </div>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4">
                <form method="post" action="${authorFormAction}" class="d-grid gap-3">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <input type="hidden" name="activeTab" value="authors">
                    <input type="hidden" name="categoryPage" value="${categoriesPage.page}">
                    <input type="hidden" name="authorPage" value="${authorsPage.page}">
                    <div>
                        <label class="form-label" for="authorName">Author Name</label>
                        <input class="form-control" id="authorName" name="name" value="${editAuthor.name}" required>
                    </div>
                    <div>
                        <label class="form-label" for="authorBio">Bio</label>
                        <textarea class="form-control" id="authorBio" name="bio" rows="5">${editAuthor.bio}</textarea>
                    </div>
                    <div class="d-flex flex-wrap gap-2 pt-2">
                        <button class="btn btn-warm" type="submit">
                            <i class="bi bi-person-vcard me-2"></i>
                            <c:choose>
                                <c:when test="${not empty editAuthor}">Update Author</c:when>
                                <c:otherwise>Save Author</c:otherwise>
                            </c:choose>
                        </button>
                        <c:if test="${not empty editAuthor}">
                            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/admin/references?activeTab=authors&categoryPage=${categoriesPage.page}&authorPage=${authorsPage.page}">Cancel Editing</a>
                        </c:if>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    (function () {
        var shell = document.querySelector("[data-reference-tab-shell]");
        if (!shell) {
            return;
        }

        var buttons = shell.querySelectorAll("[data-reference-tab-button]");
        var panels = shell.querySelectorAll("[data-reference-tab-panel]");
        var initialTab = shell.getAttribute("data-reference-initial-tab") || "categories";

        function activateTab(tabKey) {
            Array.prototype.forEach.call(buttons, function (button) {
                var isActive = button.getAttribute("data-reference-tab-key") === tabKey;
                button.classList.toggle("is-active", isActive);
                button.setAttribute("aria-selected", isActive ? "true" : "false");
            });

            Array.prototype.forEach.call(panels, function (panel) {
                var panelId = panel.getAttribute("id");
                panel.hidden = !panelId || panelId !== "reference-" + tabKey + "-panel";
            });
        }

        Array.prototype.forEach.call(buttons, function (button) {
            button.addEventListener("click", function () {
                activateTab(button.getAttribute("data-reference-tab-key") || "categories");
            });
        });

        activateTab(initialTab === "authors" ? "authors" : "categories");

        <c:if test="${not empty editCategory}">
        bootstrap.Modal.getOrCreateInstance(document.getElementById("categoryModal")).show();
        </c:if>

        <c:if test="${not empty editAuthor}">
        bootstrap.Modal.getOrCreateInstance(document.getElementById("authorModal")).show();
        </c:if>
    })();
</script>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
