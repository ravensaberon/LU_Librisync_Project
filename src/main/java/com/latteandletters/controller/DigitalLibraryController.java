package com.latteandletters.controller;

import com.latteandletters.model.Book;
import com.latteandletters.service.AuditLogService;
import com.latteandletters.service.BookService;
import com.latteandletters.service.DigitalLibraryService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@SuppressWarnings("null")
public class DigitalLibraryController {

    private final BookService bookService;
    private final DigitalLibraryService digitalLibraryService;
    private final AuditLogService auditLogService;

    public DigitalLibraryController(BookService bookService,
                                    DigitalLibraryService digitalLibraryService,
                                    AuditLogService auditLogService) {
        this.bookService = bookService;
        this.digitalLibraryService = digitalLibraryService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/student/ebooks/{bookId}")
    public String ebookReader(@PathVariable Long bookId, Authentication authentication, Model model) {
        Book book = bookService.getCatalogBookById(bookId);
        if (!book.isDigital() || !digitalLibraryService.hasReadableEbook(book)) {
            throw new IllegalArgumentException("This title does not have a readable digital copy yet.");
        }
        if (digitalLibraryService.isExternalEbookUrl(book)) {
            return "redirect:" + digitalLibraryService.getExternalEbookUrl(book);
        }

        if (authentication != null) {
            auditLogService.log(
                    authentication.getName(),
                    "DIGITAL_BOOK_OPENED",
                    "BOOK",
                    book.getId().toString(),
                    "Digital book opened",
                    "Opened e-book reader for " + book.getTitle()
            );
        }
        model.addAttribute("book", book);
        return "student/ebook-reader";
    }

    @GetMapping("/student/ebooks/{bookId}/content")
    public ResponseEntity<Resource> ebookContent(@PathVariable Long bookId) {
        Book book = bookService.getCatalogBookById(bookId);
        if (digitalLibraryService.isExternalEbookUrl(book)) {
            throw new IllegalArgumentException("This title uses an external digital copy link.");
        }
        Resource ebookResource = digitalLibraryService.getEbookResource(book);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(book.getTitle() + ".pdf").build().toString())
                .body(ebookResource);
    }
}
