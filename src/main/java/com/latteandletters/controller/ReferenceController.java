package com.latteandletters.controller;

import com.latteandletters.service.AuditLogService;
import com.latteandletters.service.BookService;
import com.latteandletters.util.PaginationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class ReferenceController {

    private static final int REFERENCE_PAGE_SIZE = 5;

    private final BookService bookService;
    private final AuditLogService auditLogService;

    public ReferenceController(BookService bookService,
                               AuditLogService auditLogService) {
        this.bookService = bookService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/references")
    public String references(@RequestParam(required = false) Long editCategoryId,
                             @RequestParam(required = false) Long editAuthorId,
                             @RequestParam(defaultValue = "categories") String activeTab,
                             @RequestParam(defaultValue = "1") Integer categoryPage,
                             @RequestParam(defaultValue = "1") Integer authorPage,
                             Model model) {
        var allCategories = bookService.getAllCategories();
        var allAuthors = bookService.getAllAuthors();
        var categoriesPage = PaginationUtils.paginate(allCategories, categoryPage, REFERENCE_PAGE_SIZE);
        var authorsPage = PaginationUtils.paginate(allAuthors, authorPage, REFERENCE_PAGE_SIZE);

        model.addAttribute("categories", categoriesPage.getItems());
        model.addAttribute("authors", authorsPage.getItems());
        model.addAttribute("categoriesPage", categoriesPage);
        model.addAttribute("authorsPage", authorsPage);
        model.addAttribute("categoryCount", allCategories.size());
        model.addAttribute("authorCount", allAuthors.size());
        model.addAttribute("activeReferenceTab", resolveActiveTab(activeTab, editCategoryId, editAuthorId));
        if (editCategoryId != null) {
            model.addAttribute("editCategory", bookService.getCategoryById(editCategoryId));
        }
        if (editAuthorId != null) {
            model.addAttribute("editAuthor", bookService.getAuthorById(editAuthorId));
        }
        return "admin/references";
    }

    @PostMapping("/categories")
    public String createCategory(@RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(defaultValue = "categories") String activeTab,
                                 @RequestParam(defaultValue = "1") Integer categoryPage,
                                 @RequestParam(defaultValue = "1") Integer authorPage,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            var category = bookService.createCategory(name, description);
            auditLogService.log(
                    authentication.getName(),
                    "CATEGORY_CREATED",
                    "CATEGORY",
                    category.getId().toString(),
                    "Category created",
                    "Name: " + category.getName()
            );
            redirectAttributes.addFlashAttribute("success", "Category added successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return buildReferenceRedirect(activeTab, categoryPage, authorPage, null, null);
    }

    @PostMapping("/categories/{categoryId}/update")
    public String updateCategory(@PathVariable Long categoryId,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(defaultValue = "categories") String activeTab,
                                 @RequestParam(defaultValue = "1") Integer categoryPage,
                                 @RequestParam(defaultValue = "1") Integer authorPage,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            var category = bookService.updateCategory(categoryId, name, description);
            auditLogService.log(
                    authentication.getName(),
                    "CATEGORY_UPDATED",
                    "CATEGORY",
                    categoryId.toString(),
                    "Category updated",
                    "Name: " + category.getName()
            );
            redirectAttributes.addFlashAttribute("success", "Category updated successfully.");
            return buildReferenceRedirect(activeTab, categoryPage, authorPage, null, null);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return buildReferenceRedirect(activeTab, categoryPage, authorPage, categoryId, null);
        }
    }

    @PostMapping("/categories/{categoryId}/delete")
    public String deleteCategory(@PathVariable Long categoryId,
                                 @RequestParam(defaultValue = "categories") String activeTab,
                                 @RequestParam(defaultValue = "1") Integer categoryPage,
                                 @RequestParam(defaultValue = "1") Integer authorPage,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            var category = bookService.getCategoryById(categoryId);
            bookService.deleteCategory(categoryId);
            auditLogService.log(
                    authentication.getName(),
                    "CATEGORY_DELETED",
                    "CATEGORY",
                    categoryId.toString(),
                    "Category deleted",
                    "Name: " + category.getName()
            );
            redirectAttributes.addFlashAttribute("success", "Category deleted successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return buildReferenceRedirect(activeTab, categoryPage, authorPage, null, null);
    }

    @PostMapping("/authors")
    public String createAuthor(@RequestParam String name,
                               @RequestParam(required = false) String bio,
                               @RequestParam(defaultValue = "authors") String activeTab,
                               @RequestParam(defaultValue = "1") Integer categoryPage,
                               @RequestParam(defaultValue = "1") Integer authorPage,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            var author = bookService.createAuthor(name, bio);
            auditLogService.log(
                    authentication.getName(),
                    "AUTHOR_CREATED",
                    "AUTHOR",
                    author.getId().toString(),
                    "Author created",
                    "Name: " + author.getName()
            );
            redirectAttributes.addFlashAttribute("success", "Author added successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return buildReferenceRedirect(activeTab, categoryPage, authorPage, null, null);
    }

    @PostMapping("/authors/{authorId}/update")
    public String updateAuthor(@PathVariable Long authorId,
                               @RequestParam String name,
                               @RequestParam(required = false) String bio,
                               @RequestParam(defaultValue = "authors") String activeTab,
                               @RequestParam(defaultValue = "1") Integer categoryPage,
                               @RequestParam(defaultValue = "1") Integer authorPage,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            var author = bookService.updateAuthor(authorId, name, bio);
            auditLogService.log(
                    authentication.getName(),
                    "AUTHOR_UPDATED",
                    "AUTHOR",
                    authorId.toString(),
                    "Author updated",
                    "Name: " + author.getName()
            );
            redirectAttributes.addFlashAttribute("success", "Author updated successfully.");
            return buildReferenceRedirect(activeTab, categoryPage, authorPage, null, null);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return buildReferenceRedirect(activeTab, categoryPage, authorPage, null, authorId);
        }
    }

    @PostMapping("/authors/{authorId}/delete")
    public String deleteAuthor(@PathVariable Long authorId,
                               @RequestParam(defaultValue = "authors") String activeTab,
                               @RequestParam(defaultValue = "1") Integer categoryPage,
                               @RequestParam(defaultValue = "1") Integer authorPage,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            var author = bookService.getAuthorById(authorId);
            bookService.deleteAuthor(authorId);
            auditLogService.log(
                    authentication.getName(),
                    "AUTHOR_DELETED",
                    "AUTHOR",
                    authorId.toString(),
                    "Author deleted",
                    "Name: " + author.getName()
            );
            redirectAttributes.addFlashAttribute("success", "Author deleted successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return buildReferenceRedirect(activeTab, categoryPage, authorPage, null, null);
    }

    private String resolveActiveTab(String activeTab, Long editCategoryId, Long editAuthorId) {
        if (editCategoryId != null) {
            return "categories";
        }
        if (editAuthorId != null) {
            return "authors";
        }
        return "authors".equalsIgnoreCase(activeTab) ? "authors" : "categories";
    }

    private String buildReferenceRedirect(String activeTab,
                                          Integer categoryPage,
                                          Integer authorPage,
                                          Long editCategoryId,
                                          Long editAuthorId) {
        StringBuilder redirect = new StringBuilder("redirect:/admin/references?activeTab=")
                .append("authors".equalsIgnoreCase(activeTab) ? "authors" : "categories")
                .append("&categoryPage=")
                .append(categoryPage == null ? 1 : Math.max(1, categoryPage))
                .append("&authorPage=")
                .append(authorPage == null ? 1 : Math.max(1, authorPage));

        if (editCategoryId != null) {
            redirect.append("&editCategoryId=").append(editCategoryId);
        }
        if (editAuthorId != null) {
            redirect.append("&editAuthorId=").append(editAuthorId);
        }
        return redirect.toString();
    }
}
