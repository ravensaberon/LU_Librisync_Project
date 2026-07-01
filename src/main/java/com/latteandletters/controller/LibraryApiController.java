package com.latteandletters.controller;

import com.latteandletters.dto.LibraryApiDtos.BookSummaryResponse;
import com.latteandletters.dto.LibraryApiDtos.BorrowerEligibilityResponse;
import com.latteandletters.dto.LibraryApiDtos.LibrarySummaryResponse;
import com.latteandletters.dto.LibraryApiDtos.ModuleInfoResponse;
import com.latteandletters.service.LibraryIntegrationService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/library")
public class LibraryApiController {

    private final LibraryIntegrationService libraryIntegrationService;

    public LibraryApiController(LibraryIntegrationService libraryIntegrationService) {
        this.libraryIntegrationService = libraryIntegrationService;
    }

    @GetMapping({"/health", "/module-info"})
    public ModuleInfoResponse moduleInfo() {
        return libraryIntegrationService.moduleInfo();
    }

    @GetMapping("/books")
    public List<BookSummaryResponse> books(@RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "false") boolean availableOnly) {
        return libraryIntegrationService.books(keyword, availableOnly);
    }

    @GetMapping("/books/{bookId}")
    public BookSummaryResponse bookById(@PathVariable Long bookId) {
        return libraryIntegrationService.bookById(bookId);
    }

    @GetMapping("/students/{studentId}/borrower-eligibility")
    public BorrowerEligibilityResponse borrowerEligibility(@PathVariable String studentId) {
        return libraryIntegrationService.borrowerEligibility(studentId);
    }

    @GetMapping("/summary")
    public LibrarySummaryResponse summary() {
        return libraryIntegrationService.summary();
    }
}
