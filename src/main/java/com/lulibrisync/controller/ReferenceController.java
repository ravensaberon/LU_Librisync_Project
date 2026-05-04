package com.lulibrisync.controller;

import com.lulibrisync.service.AuditLogService;
import com.lulibrisync.service.BookService;
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
                             Model model) {
        model.addAttribute("categories", bookService.getAllCategories());
        model.addAttribute("authors", bookService.getAllAuthors());
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
        return "redirect:/admin/references";
    }

    @PostMapping("/categories/{categoryId}/update")
    public String updateCategory(@PathVariable Long categoryId,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String description,
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
            return "redirect:/admin/references";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admin/references?editCategoryId=" + categoryId;
        }
    }

    @PostMapping("/categories/{categoryId}/delete")
    public String deleteCategory(@PathVariable Long categoryId,
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
        return "redirect:/admin/references";
    }

    @PostMapping("/authors")
    public String createAuthor(@RequestParam String name,
                               @RequestParam(required = false) String bio,
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
        return "redirect:/admin/references";
    }

    @PostMapping("/authors/{authorId}/update")
    public String updateAuthor(@PathVariable Long authorId,
                               @RequestParam String name,
                               @RequestParam(required = false) String bio,
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
            return "redirect:/admin/references";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admin/references?editAuthorId=" + authorId;
        }
    }

    @PostMapping("/authors/{authorId}/delete")
    public String deleteAuthor(@PathVariable Long authorId,
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
        return "redirect:/admin/references";
    }
}
