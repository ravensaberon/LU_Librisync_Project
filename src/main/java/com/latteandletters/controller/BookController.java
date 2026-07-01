package com.latteandletters.controller;

import com.latteandletters.service.BookService;
import com.latteandletters.service.AuditLogService;
import com.latteandletters.service.CirculationPolicyService;
import com.latteandletters.service.DigitalLibraryService;
import com.latteandletters.service.IssueService;
import com.latteandletters.service.ReservationService;
import com.latteandletters.util.PaginationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.ui.Model;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@SuppressWarnings("null")
public class BookController {

    private static final int ADMIN_BOOKS_PAGE_SIZE = 10;
    private static final int STUDENT_CATALOG_PAGE_SIZE = 12;

    private final BookService bookService;
    private final ReservationService reservationService;
    private final DigitalLibraryService digitalLibraryService;
    private final AuditLogService auditLogService;
    private final IssueService issueService;
    private final CirculationPolicyService circulationPolicyService;

    public BookController(BookService bookService,
                          ReservationService reservationService,
                          DigitalLibraryService digitalLibraryService,
                          AuditLogService auditLogService,
                          IssueService issueService,
                          CirculationPolicyService circulationPolicyService) {
        this.bookService = bookService;
        this.reservationService = reservationService;
        this.digitalLibraryService = digitalLibraryService;
        this.auditLogService = auditLogService;
        this.issueService = issueService;
        this.circulationPolicyService = circulationPolicyService;
    }

    @PostMapping("/admin/books/{bookId}/generate-barcode")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<Map<String, Object>> generateBarcode(@PathVariable Long bookId,
                                                               Authentication authentication) {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        try {
            var book = bookService.generateAndSaveBarcode(bookId);
            auditLogService.log(
                    authentication.getName(),
                    "BARCODE_GENERATED",
                    "BOOK",
                    book.getId().toString(),
                    "Barcode generated for book",
                    "Title: " + book.getTitle() + " | Barcode: " + book.getBarcode()
            );
            response.put("success", true);
            response.put("barcode", book.getBarcode());
            response.put("scanCode", book.getScanCode());
            response.put("scanCodeLabel", book.getScanCodeLabel());
            response.put("title", book.getTitle());
            response.put("isbn", book.getIsbn());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/admin/books")
    public String adminBooks(@RequestParam(required = false) Long editId,
                             @RequestParam(defaultValue = "1") Integer page,
                             Model model) {
        populateBookManagementModel(model);
        var books = bookService.getAllBooks();
        var booksPage = PaginationUtils.paginate(books, page, ADMIN_BOOKS_PAGE_SIZE);
        Map<Long, Boolean> readableBookCoverByBookId = new LinkedHashMap<>();
        for (var book : booksPage.getItems()) {
            readableBookCoverByBookId.put(book.getId(), digitalLibraryService.hasReadableBookCover(book));
        }
        model.addAttribute("books", booksPage.getItems());
        model.addAttribute("booksPage", booksPage);
        model.addAttribute("bookCount", books.size());
        model.addAttribute("digitalBookCount", books.stream().filter(book -> book.isDigital()).count());
        model.addAttribute("availableBookCount", books.stream().mapToInt(book -> book.getAvailableQuantity() == null ? 0 : book.getAvailableQuantity()).sum());
        model.addAttribute("totalCopyCount", books.stream().mapToInt(book -> book.getQuantity() == null ? 0 : book.getQuantity()).sum());
        model.addAttribute("archivedBookCount", bookService.countArchivedBooks());
        model.addAttribute("archivedBooks", bookService.getArchivedBooks());
        model.addAttribute("visibleCatalogBookCount", bookService.countVisibleCatalogBooks());
        model.addAttribute("readableBookCoverByBookId", readableBookCoverByBookId);
        if (editId != null) {
            model.addAttribute("editBook", bookService.getBookById(editId));
        }
        return "books/manage";
    }

    @PostMapping("/admin/books")
    public String createBook(@RequestParam String title,
                             @RequestParam String isbn,
                             @RequestParam(required = false) String barcode,
                             @RequestParam(required = false) Long categoryId,
                             @RequestParam(required = false) Long authorId,
                             @RequestParam(required = false) Integer publicationYear,
                             @RequestParam(required = false) Integer quantity,
                             @RequestParam(required = false) String shelfLocation,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) MultipartFile coverImageFile,
                             @RequestParam(defaultValue = "false") boolean digital,
                             @RequestParam(defaultValue = "false") boolean visibleInCatalog,
                             @RequestParam(required = false) String ebookPath,
                             @RequestParam(required = false) MultipartFile ebookFile,
                             @RequestParam(defaultValue = "1") Integer page,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String storedEbookPath = null;
        String storedCoverImagePath = null;
        boolean hasManualEbookPath = StringUtils.hasText(ebookPath);
        try {
            storedCoverImagePath = digitalLibraryService.storeBookCoverFile(title, coverImageFile);
            storedEbookPath = digitalLibraryService.storeEbookFile(title, ebookFile);
            var book = bookService.createBook(title, isbn, barcode, categoryId, authorId, publicationYear, quantity, shelfLocation, description, storedCoverImagePath, digital || storedEbookPath != null || hasManualEbookPath, storedEbookPath != null ? storedEbookPath : ebookPath, visibleInCatalog);
            auditLogService.log(
                    authentication.getName(),
                    "BOOK_CREATED",
                    "BOOK",
                    book.getId().toString(),
                    "Book created",
                    "Title: " + book.getTitle() + " | ISBN: " + book.getIsbn()
            );
            redirectAttributes.addFlashAttribute("success", "Book added to library successfully.");
        } catch (IllegalArgumentException exception) {
            digitalLibraryService.deleteManagedBookCover(storedCoverImagePath);
            digitalLibraryService.deleteManagedEbook(storedEbookPath);
            redirectAttributes.addFlashAttribute("openBookModal", true);
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/books?page=" + Math.max(1, page);
    }

    @PostMapping("/admin/books/{bookId}/update")
    public String updateBook(@RequestParam String title,
                             @RequestParam String isbn,
                             @RequestParam(required = false) String barcode,
                             @RequestParam(required = false) Long categoryId,
                             @RequestParam(required = false) Long authorId,
                             @RequestParam(required = false) Integer publicationYear,
                             @RequestParam(required = false) Integer quantity,
                             @RequestParam(required = false) String shelfLocation,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) MultipartFile coverImageFile,
                             @RequestParam(defaultValue = "false") boolean digital,
                             @RequestParam(defaultValue = "false") boolean visibleInCatalog,
                             @RequestParam(required = false) String ebookPath,
                             @RequestParam(required = false) MultipartFile ebookFile,
                             @org.springframework.web.bind.annotation.PathVariable Long bookId,
                             @RequestParam(defaultValue = "1") Integer page,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String storedEbookPath = null;
        String storedCoverImagePath = null;
        String previousManagedEbookPath = null;
        String previousManagedCoverImagePath = null;
        boolean hasManualEbookPath = StringUtils.hasText(ebookPath);
        try {
            var existingBook = bookService.getBookById(bookId);
            previousManagedEbookPath = existingBook.getEbookPath();
            previousManagedCoverImagePath = existingBook.getCoverImage();
            storedCoverImagePath = digitalLibraryService.storeBookCoverFile(title, coverImageFile);
            storedEbookPath = digitalLibraryService.storeEbookFile(title, ebookFile);
            var updatedBook = bookService.updateBook(bookId, title, isbn, barcode, categoryId, authorId, publicationYear, quantity, shelfLocation, description, storedCoverImagePath != null ? storedCoverImagePath : previousManagedCoverImagePath, digital || storedEbookPath != null || hasManualEbookPath, storedEbookPath != null ? storedEbookPath : ebookPath, visibleInCatalog);
            if (storedCoverImagePath != null
                    && digitalLibraryService.isManagedBookCoverPath(previousManagedCoverImagePath)
                    && (previousManagedCoverImagePath == null || !previousManagedCoverImagePath.equals(updatedBook.getCoverImage()))) {
                digitalLibraryService.deleteManagedBookCover(previousManagedCoverImagePath);
            }
            if (storedEbookPath != null
                    && digitalLibraryService.isManagedEbookPath(previousManagedEbookPath)
                    && (previousManagedEbookPath == null || !previousManagedEbookPath.equals(updatedBook.getEbookPath()))) {
                digitalLibraryService.deleteManagedEbook(previousManagedEbookPath);
            }
            reservationService.promoteReservationsForBook(updatedBook.getId());
            auditLogService.log(
                    authentication.getName(),
                    "BOOK_UPDATED",
                    "BOOK",
                    updatedBook.getId().toString(),
                    "Book updated",
                    "Title: " + updatedBook.getTitle() + " | Available: " + updatedBook.getAvailableQuantity() + "/" + updatedBook.getQuantity()
            );
            redirectAttributes.addFlashAttribute("success", "Book updated successfully.");
            return "redirect:/admin/books?page=" + Math.max(1, page);
        } catch (IllegalArgumentException exception) {
            digitalLibraryService.deleteManagedBookCover(storedCoverImagePath);
            digitalLibraryService.deleteManagedEbook(storedEbookPath);
            redirectAttributes.addFlashAttribute("openBookModal", true);
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admin/books?editId=" + bookId + "&page=" + Math.max(1, page);
        }
    }

    @PostMapping("/admin/books/{bookId}/archive")
    public String archiveBook(@org.springframework.web.bind.annotation.PathVariable Long bookId,
                             @RequestParam(defaultValue = "1") Integer page,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            var book = bookService.getBookById(bookId);
            bookService.archiveBook(bookId);
            auditLogService.log(
                    authentication.getName(),
                    "BOOK_ARCHIVED",
                    "BOOK",
                    bookId.toString(),
                    "Book archived",
                    "Title: " + book.getTitle() + " | ISBN: " + book.getIsbn()
            );
            redirectAttributes.addFlashAttribute("success", "Book archived successfully. You can restore it later from Books.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/books?page=" + Math.max(1, page);
    }

    @PostMapping("/admin/books/{bookId}/delete")
    public String deleteBook(@PathVariable Long bookId,
                             @RequestParam(defaultValue = "1") Integer page,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            var book = bookService.getBookById(bookId);
            String title = book.getTitle();
            String isbn = book.getIsbn();
            bookService.deleteBook(bookId);
            auditLogService.log(
                    authentication.getName(),
                    "BOOK_DELETED",
                    "BOOK",
                    bookId.toString(),
                    "Book permanently deleted",
                    "Title: " + title + " | ISBN: " + isbn
            );
            redirectAttributes.addFlashAttribute("success", "Book \"" + title + "\" has been permanently deleted.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/books?page=" + Math.max(1, page);
    }

    @PostMapping("/admin/books/{bookId}/restore")
    public String restoreBook(@PathVariable Long bookId,
                              @RequestParam(defaultValue = "1") Integer page,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            var book = bookService.restoreBook(bookId);
            auditLogService.log(
                    authentication.getName(),
                    "BOOK_RESTORED",
                    "BOOK",
                    bookId.toString(),
                    "Book restored",
                    "Title: " + book.getTitle() + " | ISBN: " + book.getIsbn()
            );
            redirectAttributes.addFlashAttribute("success", "Book restored and shown in the student catalog.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/books?page=" + Math.max(1, page);
    }

    @GetMapping("/books/{bookId}/cover")
    public ResponseEntity<Resource> bookCover(@PathVariable Long bookId) {
        var book = bookService.getBookById(bookId);
        Resource coverResource = digitalLibraryService.getBookCoverResource(book);

        String resourceName = coverResource.getFilename();
        String lowerPath = resourceName == null ? "" : resourceName.toLowerCase();
        MediaType contentType = MediaType.IMAGE_JPEG;
        if (lowerPath.endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG;
        } else if (lowerPath.endsWith(".webp")) {
            contentType = MediaType.parseMediaType("image/webp");
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(book.getTitle() + "-cover").build().toString())
                .body(coverResource);
    }

    @GetMapping("/student/catalog")
    public String studentCatalog(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) Long categoryId,
                                 @RequestParam(required = false) Long authorId,
                                 @RequestParam(required = false) String isbn,
                                 @RequestParam(defaultValue = "false") boolean availableOnly,
                                 @RequestParam(defaultValue = "1") Integer page,
                                 Authentication authentication,
                                 Model model) {
        reservationService.syncReadyReservations();
        var books = bookService.searchBooks(keyword, categoryId, authorId, isbn, availableOnly);
        var booksPage = PaginationUtils.paginate(books, page, STUDENT_CATALOG_PAGE_SIZE);
        Map<Long, Integer> readyReservationCountsByBook = reservationService.getReadyReservationCountsByBook();
        Map<Long, Integer> walkInBorrowableCopyCountByBook = new LinkedHashMap<>();
        Map<Long, Boolean> readableEbookByBookId = new LinkedHashMap<>();
        Map<Long, Boolean> readableBookCoverByBookId = new LinkedHashMap<>();
        for (var book : booksPage.getItems()) {
            int availableCopies = book.getAvailableQuantity() == null ? 0 : Math.max(0, book.getAvailableQuantity());
            int readyReservations = readyReservationCountsByBook.getOrDefault(book.getId(), 0);
            walkInBorrowableCopyCountByBook.put(book.getId(), Math.max(0, availableCopies - readyReservations));
            readableEbookByBookId.put(book.getId(), digitalLibraryService.hasReadableEbook(book));
            readableBookCoverByBookId.put(book.getId(), digitalLibraryService.hasReadableBookCover(book));
        }

        model.addAttribute("books", booksPage.getItems());
        model.addAttribute("booksPage", booksPage);
        model.addAttribute("categories", bookService.getAllCategories());
        model.addAttribute("authors", bookService.getAllAuthors());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedAuthorId", authorId);
        model.addAttribute("isbnValue", isbn);
        model.addAttribute("availableOnly", availableOnly);
        model.addAttribute("studentActiveIssueStatusByBookId", issueService.getActiveIssueStatusesForStudentBooks(authentication.getName()));
        model.addAttribute("studentActiveIssueIdByBookId", issueService.getActiveIssueIdsForStudentBooks(authentication.getName()));
        model.addAttribute("studentActiveIssueDueDateByBookId", issueService.getActiveIssueDueDatesForStudentBooks(authentication.getName()));
        model.addAttribute("studentActiveIssueReturnRequestedByBookId", issueService.getActiveIssueReturnRequestedByBookIds(authentication.getName()));
        model.addAttribute("studentReservationStatusByBookId", reservationService.getReservationStatusesForStudentBooks(authentication.getName()));
        model.addAttribute("reservationQueueSizes", reservationService.getActiveQueueSizesByBook());
        model.addAttribute("readyReservationCountsByBook", readyReservationCountsByBook);
        model.addAttribute("walkInBorrowableCopyCountByBook", walkInBorrowableCopyCountByBook);
        model.addAttribute("readableEbookByBookId", readableEbookByBookId);
        model.addAttribute("readableBookCoverByBookId", readableBookCoverByBookId);
        model.addAttribute("nextAvailableDateByBookId", issueService.getNextAvailableDatesByBookIds(booksPage.getItems().stream().map(book -> book.getId()).toList()));
        model.addAttribute("todayDate", LocalDate.now());
        model.addAttribute("defaultBorrowDueDate", LocalDate.now().plusDays(circulationPolicyService.getMaxLoanDays()));
        model.addAttribute("maxLoanDays", circulationPolicyService.getMaxLoanDays());
        return "student/catalog";
    }

    @GetMapping(value = "/student/catalog/barcode-lookup", produces = "application/json")
    public ResponseEntity<Map<String, Object>> barcodeLookup(@RequestParam String code) {
        return bookService.findBookByCode(code)
                .map(book -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("bookId", book.getId());
                    result.put("title", book.getTitle());
                    return ResponseEntity.ok(result);
                })
                .orElseGet(() -> ResponseEntity.notFound().<Map<String, Object>>build());
    }

    @PostMapping("/student/catalog/{bookId}/borrow")
    public String borrowFromCatalog(@PathVariable Long bookId,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            bookService.getCatalogBookById(bookId);
            var reservation = reservationService.placeBorrowRequest(bookId, authentication.getName());
            auditLogService.log(
                    authentication.getName(),
                    "BORROW_REQUEST_CREATED",
                    "RESERVATION",
                    reservation.getId().toString(),
                    "Student requested a physical checkout",
                    "Book: " + reservation.getBook().getTitle() + " | Status: " + reservation.getStatus()
            );
            redirectAttributes.addFlashAttribute("success", "Borrow request submitted. Please wait for staff approval.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/catalog";
    }

    private void populateBookManagementModel(Model model) {
        model.addAttribute("categories", bookService.getAllCategories());
        model.addAttribute("authors", bookService.getAllAuthors());
    }
}
