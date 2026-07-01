package com.latteandletters.service;

import com.latteandletters.dto.LibraryApiDtos.BookSummaryResponse;
import com.latteandletters.dto.LibraryApiDtos.BorrowerEligibilityResponse;
import com.latteandletters.dto.LibraryApiDtos.LibrarySummaryResponse;
import com.latteandletters.dto.LibraryApiDtos.ModuleInfoResponse;
import com.latteandletters.model.Book;
import com.latteandletters.model.FineStatus;
import com.latteandletters.model.IssueStatus;
import com.latteandletters.model.Student;
import com.latteandletters.model.UserStatus;
import com.latteandletters.repository.BookRepository;
import com.latteandletters.repository.FineRepository;
import com.latteandletters.repository.IssueRecordRepository;
import com.latteandletters.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LibraryIntegrationService {

    private final BookRepository bookRepository;
    private final StudentRepository studentRepository;
    private final IssueRecordRepository issueRecordRepository;
    private final FineRepository fineRepository;

    public LibraryIntegrationService(BookRepository bookRepository,
                                     StudentRepository studentRepository,
                                     IssueRecordRepository issueRecordRepository,
                                     FineRepository fineRepository) {
        this.bookRepository = bookRepository;
        this.studentRepository = studentRepository;
        this.issueRecordRepository = issueRecordRepository;
        this.fineRepository = fineRepository;
    }

    public ModuleInfoResponse moduleInfo() {
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("moduleInfo", "GET /api/library/module-info");
        endpoints.put("books", "GET /api/library/books?availableOnly=true&keyword=java");
        endpoints.put("bookById", "GET /api/library/books/{bookId}");
        endpoints.put("borrowerEligibility", "GET /api/library/students/{studentId}/borrower-eligibility");
        endpoints.put("summary", "GET /api/library/summary");
        endpoints.put("attendanceStatusConsumer", "GET /api/integrations/attendance/students/{studentId}/status");
        endpoints.put("cafeProfileConsumer", "GET /api/integrations/cafe/students/{studentId}/profile");
        return new ModuleInfoResponse(
                "library",
                "Latte and Letters",
                "1.0.0",
                "ready",
                LocalDateTime.now(),
                endpoints
        );
    }

    public List<BookSummaryResponse> books(String keyword, boolean availableOnly) {
        return bookRepository.searchBooks(blankToNull(keyword), null, null, null, availableOnly).stream()
                .map(this::toBookSummary)
                .toList();
    }

    public BookSummaryResponse bookById(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .filter(found -> !found.isArchived())
                .orElseThrow(() -> new IllegalArgumentException("Book not found."));
        return toBookSummary(book);
    }

    public BorrowerEligibilityResponse borrowerEligibility(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));

        long activeLoanCount = issueRecordRepository.countByStudent_IdAndStatusIn(
                student.getId(),
                List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE)
        );
        long unpaidFineCount = fineRepository.countByStudent_IdAndStatus(student.getId(), FineStatus.UNPAID);
        BigDecimal unpaidFineTotal = fineRepository.sumAmountByStudentIdAndStatus(student.getId(), FineStatus.UNPAID);
        List<String> reasons = new ArrayList<>();

        if (!UserStatus.ACTIVE.equals(student.getUser().getStatus())) {
            reasons.add("Student account is not active.");
        }
        if (activeLoanCount >= 5) {
            reasons.add("Student already reached the maximum active loan count.");
        }
        if (unpaidFineCount > 0) {
            reasons.add("Student has unpaid library fines.");
        }

        return new BorrowerEligibilityResponse(
                student.getStudentId(),
                student.getUser().getName(),
                student.getUser().getEmail(),
                student.getCourse(),
                student.getYearLevel(),
                student.getUser().getStatus().name(),
                reasons.isEmpty(),
                activeLoanCount,
                unpaidFineCount,
                unpaidFineTotal,
                reasons
        );
    }

    public LibrarySummaryResponse summary() {
        return new LibrarySummaryResponse(
                bookRepository.count(),
                bookRepository.countByAvailableQuantityGreaterThan(0),
                bookRepository.countByVisibleInCatalogTrueAndArchivedFalse(),
                bookRepository.countByArchivedTrue(),
                studentRepository.countByUser_Status(UserStatus.ACTIVE),
                studentRepository.countByUser_Status(UserStatus.INACTIVE),
                issueRecordRepository.countByStatus(IssueStatus.ISSUED),
                issueRecordRepository.countByStatus(IssueStatus.OVERDUE),
                fineRepository.countByStatus(FineStatus.UNPAID)
        );
    }

    private BookSummaryResponse toBookSummary(Book book) {
        return new BookSummaryResponse(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getBarcode(),
                book.getCategory() == null ? null : book.getCategory().getName(),
                book.getAuthor() == null ? null : book.getAuthor().getName(),
                book.getPublicationYear(),
                book.getQuantity(),
                book.getAvailableQuantity(),
                book.getShelfLocation(),
                book.isDigital(),
                book.isVisibleInCatalog()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
