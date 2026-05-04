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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css?v=20260504-global-side-nav-flush3">
</head>
<body>
<div class="page-shell">
    <div class="app-nav">
        <div>
            <span class="tag-chip">Reference Data</span>
            <div class="brand-title mt-2">Manage categories and authors</div>
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

    <section class="panel-grid mb-4">
        <div class="panel-card">
            <div class="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3">
                <div>
                    <div class="section-title mb-2">
                        <c:choose>
                            <c:when test="${not empty editCategory}">Edit category</c:when>
                            <c:otherwise>Add category</c:otherwise>
                        </c:choose>
                    </div>
                    <p class="helper-copy">Keep subject groupings organized so catalog search and analytics stay consistent.</p>
                </div>
                <c:if test="${not empty editCategory}">
                    <a class="action-link" href="${pageContext.request.contextPath}/admin/references">Cancel editing</a>
                </c:if>
            </div>

            <form method="post" action="${categoryFormAction}">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <div class="mb-3">
                    <label class="form-label" for="categoryName">Category name</label>
                    <input class="form-control" id="categoryName" name="name" value="${editCategory.name}" required>
                </div>
                <div class="mb-3">
                    <label class="form-label" for="categoryDescription">Description</label>
                    <textarea class="form-control" id="categoryDescription" name="description" rows="3">${editCategory.description}</textarea>
                </div>
                <div class="d-flex flex-wrap gap-2">
                    <button class="btn btn-brand" type="submit">
                        <i class="bi bi-tags me-2"></i>
                        <c:choose>
                            <c:when test="${not empty editCategory}">Update category</c:when>
                            <c:otherwise>Save category</c:otherwise>
                        </c:choose>
                    </button>
                    <c:if test="${not empty editCategory}">
                        <a class="btn btn-warm" href="${pageContext.request.contextPath}/admin/references">Back to add mode</a>
                    </c:if>
                </div>
            </form>
        </div>

        <div class="panel-card">
            <div class="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3">
                <div>
                    <div class="section-title mb-2">
                        <c:choose>
                            <c:when test="${not empty editAuthor}">Edit author</c:when>
                            <c:otherwise>Add author</c:otherwise>
                        </c:choose>
                    </div>
                    <p class="helper-copy">Maintain author records to improve catalog quality, search filters, and inventory reporting.</p>
                </div>
                <c:if test="${not empty editAuthor}">
                    <a class="action-link" href="${pageContext.request.contextPath}/admin/references">Cancel editing</a>
                </c:if>
            </div>

            <form method="post" action="${authorFormAction}">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <div class="mb-3">
                    <label class="form-label" for="authorName">Author name</label>
                    <input class="form-control" id="authorName" name="name" value="${editAuthor.name}" required>
                </div>
                <div class="mb-3">
                    <label class="form-label" for="authorBio">Bio</label>
                    <textarea class="form-control" id="authorBio" name="bio" rows="4">${editAuthor.bio}</textarea>
                </div>
                <div class="d-flex flex-wrap gap-2">
                    <button class="btn btn-warm" type="submit">
                        <i class="bi bi-person-vcard me-2"></i>
                        <c:choose>
                            <c:when test="${not empty editAuthor}">Update author</c:when>
                            <c:otherwise>Save author</c:otherwise>
                        </c:choose>
                    </button>
                    <c:if test="${not empty editAuthor}">
                        <a class="btn btn-warm" href="${pageContext.request.contextPath}/admin/references">Back to add mode</a>
                    </c:if>
                </div>
            </form>
        </div>
    </section>

    <section class="panel-grid">
        <div class="panel-card">
            <div class="section-title">Existing categories</div>
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
                                <a class="icon-action" href="${pageContext.request.contextPath}/admin/references?editCategoryId=${category.id}" title="Edit category">
                                    <i class="bi bi-pencil-square"></i>
                                </a>
                                <form method="post" action="${pageContext.request.contextPath}/admin/categories/${category.id}/delete">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
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
        </div>

        <div class="panel-card">
            <div class="section-title">Existing authors</div>
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
                                <a class="icon-action" href="${pageContext.request.contextPath}/admin/references?editAuthorId=${author.id}" title="Edit author">
                                    <i class="bi bi-pencil-square"></i>
                                </a>
                                <form method="post" action="${pageContext.request.contextPath}/admin/authors/${author.id}/delete">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
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
        </div>
    </section>
</div>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>

