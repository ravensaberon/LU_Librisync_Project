package com.latteandletters.service;

import com.latteandletters.dto.AdminDashboardChartPoint;
import com.latteandletters.dto.AdminDashboardChartSeries;
import com.latteandletters.model.AdminNotificationType;
import com.latteandletters.model.Book;
import com.latteandletters.model.IssueRecord;
import com.latteandletters.model.IssueStatus;
import com.latteandletters.model.Reservation;
import com.latteandletters.model.ReservationStatus;
import com.latteandletters.model.Student;
import com.latteandletters.model.User;
import com.latteandletters.model.UserStatus;
import com.latteandletters.repository.BookRepository;
import com.latteandletters.repository.IssueRecordRepository;
import com.latteandletters.repository.StudentRepository;
import com.latteandletters.repository.UserRepository;
import com.latteandletters.repository.projection.BookBorrowStat;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

@Service
@SuppressWarnings("null")
public class IssueService {

    private final IssueRecordRepository issueRecordRepository;
    private final BookRepository bookRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final StudentService studentService;
    private final ReservationService reservationService;
    private final EmailNotificationService emailNotificationService;
    private final AdminNotificationService adminNotificationService;
    private final FineService fineService;
    private final CirculationPolicyService circulationPolicyService;
    private final BigDecimal dailyFine;
    private final BigDecimal reserveDailyFine;

    public IssueService(IssueRecordRepository issueRecordRepository,
                        BookRepository bookRepository,
                        StudentRepository studentRepository,
                        UserRepository userRepository,
                        StudentService studentService,
                        ReservationService reservationService,
                        EmailNotificationService emailNotificationService,
                        AdminNotificationService adminNotificationService,
                        FineService fineService,
                        CirculationPolicyService circulationPolicyService,
                        @org.springframework.beans.factory.annotation.Value("${latteandletters.circulation.daily-fine:2.00}") BigDecimal dailyFine,
                        @org.springframework.beans.factory.annotation.Value("${latteandletters.circulation.reserve-daily-fine:50.00}") BigDecimal reserveDailyFine) {
        this.issueRecordRepository = issueRecordRepository;
        this.bookRepository = bookRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.studentService = studentService;
        this.reservationService = reservationService;
        this.emailNotificationService = emailNotificationService;
        this.adminNotificationService = adminNotificationService;
        this.fineService = fineService;
        this.circulationPolicyService = circulationPolicyService;
        this.dailyFine = dailyFine == null ? new BigDecimal("2.00") : dailyFine;
        this.reserveDailyFine = reserveDailyFine == null ? new BigDecimal("50.00") : reserveDailyFine;
    }

    @Transactional
    public IssueRecord issueBook(Long bookId, Long studentId, LocalDate dueDate, String issuerEmail, String remarks) {
        if (bookId == null) {
            throw new IllegalArgumentException("Book is required.");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("Student is required.");
        }
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date is required.");
        }
        if (dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Due date cannot be earlier than today.");
        }
        if (dueDate.isAfter(LocalDate.now().plusDays(circulationPolicyService.getMaxLoanDays()))) {
            throw new IllegalArgumentException("Due date exceeds the maximum loan period allowed by current circulation policy.");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found."));
        if (book.getAvailableQuantity() == null || book.getAvailableQuantity() < 1) {
            throw new IllegalArgumentException("Selected book is not available.");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));
        if (!UserStatus.ACTIVE.equals(student.getUser().getStatus())) {
            throw new IllegalArgumentException("This student account is inactive and cannot borrow items.");
        }
        if (issueRecordRepository.existsByBook_IdAndStudent_IdAndStatusIn(bookId, studentId, List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE))) {
            throw new IllegalArgumentException("This student already has an active loan for the selected book.");
        }
        circulationPolicyService.validateBorrowingEligibility(student);
        User issuedBy = userRepository.findByEmailIgnoreCase(issuerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Issuing user not found."));
        reservationService.beforeIssueValidation(bookId, studentId);

        IssueRecord issueRecord = new IssueRecord();
        issueRecord.setBook(book);
        issueRecord.setStudent(student);
        issueRecord.setIssuedBy(issuedBy);
        issueRecord.setIssueDate(LocalDateTime.now());
        issueRecord.setDueDate(dueDate.atTime(17, 0));
        issueRecord.setStatus(IssueStatus.ISSUED);
        issueRecord.setFineAmount(BigDecimal.ZERO);
        issueRecord.setRemarks(blankToNull(remarks));
        issueRecord.setQrIssueCode(buildIssueCode(book, student));

        book.setAvailableQuantity(book.getAvailableQuantity() - 1);
        bookRepository.save(book);
        IssueRecord savedIssueRecord = issueRecordRepository.save(issueRecord);
        reservationService.markReservationClaimed(bookId, studentId);
        emailNotificationService.queueDueReminder(savedIssueRecord);
        adminNotificationService.notifyUser(
                student.getUser().getEmail(),
                AdminNotificationType.BORROW_STATUS,
                "Book issued",
                "Your loan for " + book.getTitle() + " is now active until " + savedIssueRecord.getDueDateDisplay() + ".",
                "/student/history"
        );
        fineService.syncFineForIssue(savedIssueRecord);
        return savedIssueRecord;
    }

    @Transactional
    public IssueRecord issueReservationPickup(Long reservationId, String issuerEmail, String remarks) {
        Reservation reservation = reservationService.getReservationById(reservationId);

        // Auto-approve PENDING_APPROVAL borrow requests on desk scan
        if (ReservationStatus.PENDING_APPROVAL.equals(reservation.getStatus())) {
            reservationService.approveBorrowRequest(reservation.getId());
            reservation = reservationService.getReservationById(reservation.getId());
        }

        // Promote PENDING queue reservations if a copy is now available
        if (ReservationStatus.PENDING.equals(reservation.getStatus())) {
            reservationService.promoteReservationsForBook(reservation.getBook().getId());
            reservation = reservationService.getReservationById(reservation.getId());
        }

        if (!ReservationStatus.READY.equals(reservation.getStatus())) {
            if (ReservationStatus.PENDING.equals(reservation.getStatus())) {
                throw new IllegalArgumentException("This " + (reservation.isBorrowRequest() ? "borrow request" : "reservation") + " is not ready for desk release yet — no copy is currently available.");
            }
            if (ReservationStatus.CLAIMED.equals(reservation.getStatus())) {
                throw new IllegalArgumentException("This " + (reservation.isBorrowRequest() ? "borrow request" : "reservation") + " has already been processed.");
            }
            if (ReservationStatus.CANCELLED.equals(reservation.getStatus())) {
                throw new IllegalArgumentException("This " + (reservation.isBorrowRequest() ? "borrow request" : "reservation") + " was cancelled and can no longer be issued.");
            }
            if (ReservationStatus.DENIED.equals(reservation.getStatus())) {
                throw new IllegalArgumentException("This borrow request was denied and cannot be issued.");
            }
            throw new IllegalArgumentException("This " + (reservation.isBorrowRequest() ? "borrow request" : "reservation") + " is not in a valid state for desk release (status: " + reservation.getStatus() + ").");
        }

        return issueBook(
                reservation.getBook().getId(),
                reservation.getStudent().getId(),
                LocalDate.now().plusDays(circulationPolicyService.getMaxLoanDays()),
                issuerEmail,
                remarks
        );
    }

    @Transactional
    public IssueRecord returnBook(Long issueRecordId) {
        IssueRecord issueRecord = issueRecordRepository.findById(issueRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Issue record not found."));

        if (issueRecord.isReturned()) {
            throw new IllegalArgumentException("Book is already returned.");
        }

        LocalDateTime returnTimestamp = LocalDateTime.now();
        issueRecord.setReturnDate(returnTimestamp);
        issueRecord.setReturnRequestedAt(null);
        issueRecord.setFineAmount(calculateFine(issueRecord.getBook(), issueRecord.getDueDate(), returnTimestamp));
        issueRecord.setStatus(IssueStatus.RETURNED);

        Book book = issueRecord.getBook();
        int currentAvailable = book.getAvailableQuantity() == null ? 0 : book.getAvailableQuantity();
        int totalQuantity = book.getQuantity() == null ? 1 : book.getQuantity();
        book.setAvailableQuantity(Math.min(totalQuantity, currentAvailable + 1));
        bookRepository.save(book);
        IssueRecord savedIssueRecord = issueRecordRepository.save(issueRecord);
        emailNotificationService.cancelDueReminder(savedIssueRecord);
        reservationService.promoteReservationsForBook(book.getId());
        adminNotificationService.notifyUser(
                issueRecord.getStudent().getUser().getEmail(),
                AdminNotificationType.RETURN_STATUS,
                "Return confirmed",
                "Your return for " + issueRecord.getBook().getTitle() + " was confirmed by the circulation desk.",
                "/student/history?tab=returned"
        );
        fineService.syncFineForIssue(savedIssueRecord);
        return savedIssueRecord;
    }

    @Transactional
    public void refreshOverdueStatuses() {
        List<IssueRecord> activeIssues = issueRecordRepository.findByStatusInOrderByIssueDateDesc(List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE));
        LocalDateTime now = LocalDateTime.now();

        for (IssueRecord issueRecord : activeIssues) {
            if (issueRecord.getReturnDate() != null) {
                continue;
            }

            BigDecimal fine = calculateFine(issueRecord.getBook(), issueRecord.getDueDate(), now);
            if (now.isAfter(issueRecord.getDueDate())) {
                issueRecord.setStatus(IssueStatus.OVERDUE);
                issueRecord.setFineAmount(fine);
            } else {
                issueRecord.setStatus(IssueStatus.ISSUED);
                issueRecord.setFineAmount(BigDecimal.ZERO);
            }
        }

        List<IssueRecord> savedIssues = issueRecordRepository.saveAll(activeIssues);
        for (IssueRecord savedIssue : savedIssues) {
            fineService.syncFineForIssue(savedIssue);
        }
    }

    public List<IssueRecord> getActiveIssues() {
        refreshOverdueStatuses();
        return issueRecordRepository.findActiveIssuesOrdered(List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE)).stream()
                .sorted(Comparator
                        .comparing(IssueRecord::isReturnRequested).reversed()
                        .thenComparing(IssueRecord::getIssueDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<IssueRecord> getRecentIssues() {
        refreshOverdueStatuses();
        return issueRecordRepository.findTop8ByOrderByIssueDateDesc();
    }

    public List<IssueRecord> getAllIssues() {
        refreshOverdueStatuses();
        return issueRecordRepository.findAllByOrderByIssueDateDesc();
    }

    public List<IssueRecord> getStudentIssues(String email) {
        refreshOverdueStatuses();
        Student student = studentService.getStudentByEmail(email);
        return issueRecordRepository.findByStudent_IdOrderByIssueDateDesc(student.getId());
    }

    public List<IssueRecord> getStudentIssuesByStudentId(String studentId) {
        refreshOverdueStatuses();
        Student student = studentService.getStudentByStudentId(studentId);
        return issueRecordRepository.findByStudent_IdOrderByIssueDateDesc(student.getId());
    }

    public Map<Long, String> getActiveIssueStatusesForStudentBooks(String email) {
        Map<Long, String> activeIssueStatuses = new LinkedHashMap<>();
        for (IssueRecord issueRecord : getStudentIssues(email)) {
            if (!issueRecord.isReturned() && !activeIssueStatuses.containsKey(issueRecord.getBook().getId())) {
                activeIssueStatuses.put(issueRecord.getBook().getId(), issueRecord.getStatus().name());
            }
        }
        return activeIssueStatuses;
    }

    public Map<Long, Long> getActiveIssueIdsForStudentBooks(String email) {
        Map<Long, Long> activeIssueIds = new LinkedHashMap<>();
        for (IssueRecord issueRecord : getStudentIssues(email)) {
            if (!issueRecord.isReturned() && !activeIssueIds.containsKey(issueRecord.getBook().getId())) {
                activeIssueIds.put(issueRecord.getBook().getId(), issueRecord.getId());
            }
        }
        return activeIssueIds;
    }

    public Map<Long, Boolean> getActiveIssueReturnRequestedByBookIds(String email) {
        Map<Long, Boolean> activeIssueReturnRequested = new LinkedHashMap<>();
        for (IssueRecord issueRecord : getStudentIssues(email)) {
            if (!issueRecord.isReturned() && !activeIssueReturnRequested.containsKey(issueRecord.getBook().getId())) {
                activeIssueReturnRequested.put(issueRecord.getBook().getId(), issueRecord.isReturnRequested());
            }
        }
        return activeIssueReturnRequested;
    }

    public long countPendingReturnRequests() {
        return getActiveIssues().stream()
                .filter(IssueRecord::isReturnRequested)
                .count();
    }

    public Map<Long, LocalDate> getActiveIssueDueDatesForStudentBooks(String email) {
        Map<Long, LocalDate> activeIssueDueDates = new LinkedHashMap<>();
        for (IssueRecord issueRecord : getStudentIssues(email)) {
            if (!issueRecord.isReturned() && !activeIssueDueDates.containsKey(issueRecord.getBook().getId()) && issueRecord.getDueDate() != null) {
                activeIssueDueDates.put(issueRecord.getBook().getId(), issueRecord.getDueDate().toLocalDate());
            }
        }
        return activeIssueDueDates;
    }

    public Map<Long, LocalDate> getNextAvailableDatesByBookIds(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, LocalDate> nextAvailableDates = new LinkedHashMap<>();
        for (IssueRecord issueRecord : issueRecordRepository.findByBook_IdInAndStatusInOrderByDueDateAsc(
                bookIds,
                List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE))) {
            Long bookId = issueRecord.getBook().getId();
            if (!nextAvailableDates.containsKey(bookId) && issueRecord.getDueDate() != null) {
                nextAvailableDates.put(bookId, issueRecord.getDueDate().toLocalDate());
            }
        }
        return nextAvailableDates;
    }

    public List<BookBorrowStat> getMostBorrowedBooks() {
        return issueRecordRepository.findMostBorrowedBooks(PageRequest.of(0, 5));
    }

    public long countTransactionsManagedBy(String adminEmail) {
        return issueRecordRepository.countByIssuedBy_EmailIgnoreCase(adminEmail);
    }

    public IssueRecord getIssueById(Long issueId) {
        if (issueId == null) {
            throw new IllegalArgumentException("Issue record is required.");
        }
        return issueRecordRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue record not found."));
    }

    @Transactional
    public IssueRecord requestReturnByStudent(Long issueId, String email) {
        Student student = studentService.getStudentByEmail(email);
        IssueRecord issueRecord = getIssueById(issueId);
        if (!issueRecord.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException("You can only manage return requests for your own loans.");
        }
        if (issueRecord.isReturned()) {
            throw new IllegalArgumentException("This book has already been returned.");
        }
        if (issueRecord.isReturnRequested()) {
            throw new IllegalArgumentException("A desk return request is already pending for this loan.");
        }

        issueRecord.setReturnRequestedAt(LocalDateTime.now());
        IssueRecord savedIssueRecord = issueRecordRepository.save(issueRecord);
        adminNotificationService.notifyUser(
                student.getUser().getEmail(),
                AdminNotificationType.RETURN_STATUS,
                "Return request sent",
                "Your desk return request for " + issueRecord.getBook().getTitle() + " has been submitted.",
                "/student/history?tab=requests"
        );
        adminNotificationService.notifyAdmins(
                AdminNotificationType.RETURN_REQUEST,
                "New return request",
                student.getUser().getName() + " requested a desk return for " + issueRecord.getBook().getTitle() + ".",
                "/admin/issues"
        );
        return savedIssueRecord;
    }

    @Transactional
    public IssueRecord cancelReturnRequestByStudent(Long issueId, String email) {
        Student student = studentService.getStudentByEmail(email);
        IssueRecord issueRecord = getIssueById(issueId);
        if (!issueRecord.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException("You can only manage return requests for your own loans.");
        }
        if (issueRecord.isReturned()) {
            throw new IllegalArgumentException("This book has already been returned.");
        }
        if (!issueRecord.isReturnRequested()) {
            throw new IllegalArgumentException("There is no pending desk return request for this loan.");
        }

        issueRecord.setReturnRequestedAt(null);
        IssueRecord savedIssueRecord = issueRecordRepository.save(issueRecord);
        adminNotificationService.notifyUser(
                student.getUser().getEmail(),
                AdminNotificationType.RETURN_STATUS,
                "Return request cancelled",
                "Your return request for " + issueRecord.getBook().getTitle() + " was cancelled.",
                "/student/history?tab=current"
        );
        return savedIssueRecord;
    }

    @Transactional
    public IssueRecord updateIssue(Long issueId,
                                   LocalDate dueDate,
                                   String remarks) {
        IssueRecord issueRecord = getIssueById(issueId);
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date is required.");
        }
        if (!issueRecord.isReturned()) {
            validateActiveLoanDueDate(dueDate);
        }

        issueRecord.setDueDate(dueDate.atTime(17, 0));
        issueRecord.setRemarks(blankToNull(remarks));

        if (!issueRecord.isReturned()) {
            LocalDateTime now = LocalDateTime.now();
            BigDecimal fine = calculateFine(issueRecord.getBook(), issueRecord.getDueDate(), now);
            if (now.isAfter(issueRecord.getDueDate())) {
                issueRecord.setStatus(IssueStatus.OVERDUE);
                issueRecord.setFineAmount(fine);
            } else {
                issueRecord.setStatus(IssueStatus.ISSUED);
                issueRecord.setFineAmount(BigDecimal.ZERO);
            }
        }

        IssueRecord savedIssueRecord = issueRecordRepository.save(issueRecord);
        if (!savedIssueRecord.isReturned()) {
            emailNotificationService.cancelDueReminder(savedIssueRecord);
            emailNotificationService.queueDueReminder(savedIssueRecord);
        }
        fineService.syncFineForIssue(savedIssueRecord);
        return savedIssueRecord;
    }

    private void validateActiveLoanDueDate(LocalDate dueDate) {
        if (dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Due date cannot be earlier than today.");
        }
        if (dueDate.isAfter(LocalDate.now().plusDays(circulationPolicyService.getMaxLoanDays()))) {
            throw new IllegalArgumentException("Due date exceeds the maximum loan period allowed by current circulation policy.");
        }
    }

    @Transactional
    public void deleteIssue(Long issueId) {
        IssueRecord issueRecord = getIssueById(issueId);
        emailNotificationService.cancelDueReminder(issueRecord);
        fineService.removeFineForIssue(issueId);
        if (!issueRecord.isReturned()) {
            Book book = issueRecord.getBook();
            int currentAvailable = book.getAvailableQuantity() == null ? 0 : book.getAvailableQuantity();
            int totalQuantity = book.getQuantity() == null ? 1 : book.getQuantity();
            book.setAvailableQuantity(Math.min(totalQuantity, currentAvailable + 1));
            bookRepository.save(book);
            reservationService.promoteReservationsForBook(book.getId());
        }
        issueRecordRepository.delete(issueRecord);
    }

    public List<AdminDashboardChartSeries> getCirculationChartSeries() {
        List<IssueRecord> issueRecords = issueRecordRepository.findAll();
        return List.of(
                buildDailyChartSeries(issueRecords),
                buildWeeklyChartSeries(issueRecords),
                buildMonthlyChartSeries(issueRecords),
                buildYearlyChartSeries(issueRecords)
        );
    }

    public List<AdminDashboardChartPoint> getWeeklyCirculationChart() {
        return buildWeeklyChartSeries(issueRecordRepository.findAll()).getPoints();
    }

    private BigDecimal calculateFine(Book book, LocalDateTime dueDate, LocalDateTime referenceDate) {
        if (dueDate == null || referenceDate == null || !referenceDate.isAfter(dueDate)) {
            return BigDecimal.ZERO;
        }

        long overdueDays = ChronoUnit.DAYS.between(dueDate.toLocalDate(), referenceDate.toLocalDate());
        if (overdueDays < 1) {
            overdueDays = 1;
        }
        return resolveDailyFine(book).multiply(BigDecimal.valueOf(overdueDays));
    }

    private BigDecimal resolveDailyFine(Book book) {
        return isReserveBook(book) ? reserveDailyFine : dailyFine;
    }

    private boolean isReserveBook(Book book) {
        if (book == null) {
            return false;
        }

        return containsReserveKeyword(book.getShelfLocation())
                || (book.getCategory() != null && containsReserveKeyword(book.getCategory().getName()))
                || containsReserveKeyword(book.getTitle());
    }

    private boolean containsReserveKeyword(String value) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains("reserve");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String buildIssueCode(Book book, Student student) {
        String studentCode = student.getStudentId() == null ? "STUDENT" : student.getStudentId().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ENGLISH);
        String issueDateCode = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ENGLISH));
        return "LL-ISSUE-" + book.getId() + "-" + studentCode + "-" + issueDateCode;
    }

    private int heightPercentage(long value, long maxValue) {
        if (value < 1) {
            return 0;
        }
        return Math.max(18, (int) Math.round((value * 100.0) / maxValue));
    }

    private AdminDashboardChartSeries buildDailyChartSeries(List<IssueRecord> issueRecords) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(13);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
        List<LocalDate> buckets = IntStream.range(0, 14)
                .mapToObj(startDate::plusDays)
                .toList();
        return buildChartSeries(
                "day",
                "Daily",
                "Daily borrowing activity",
                "Track books issued and returned each day across the last 14 days.",
                "day",
                buckets,
                Function.identity(),
                labelFormatter::format,
                issueRecords
        );
    }

    private AdminDashboardChartSeries buildWeeklyChartSeries(List<IssueRecord> issueRecords) {
        LocalDate currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startWeek = currentWeekStart.minusWeeks(7);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
        List<LocalDate> buckets = IntStream.range(0, 8)
                .mapToObj(startWeek::plusWeeks)
                .toList();
        return buildChartSeries(
                "week",
                "Weekly",
                "Weekly borrowing trend",
                "Compare circulation volume week by week to catch demand spikes and return slowdowns.",
                "week",
                buckets,
                date -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                labelFormatter::format,
                issueRecords
        );
    }

    private AdminDashboardChartSeries buildMonthlyChartSeries(List<IssueRecord> issueRecords) {
        LocalDate currentMonthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate startMonth = currentMonthStart.minusMonths(11);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
        List<LocalDate> buckets = IntStream.range(0, 12)
                .mapToObj(startMonth::plusMonths)
                .toList();
        return buildChartSeries(
                "month",
                "Monthly",
                "Monthly borrowing trend",
                "Review long-range borrowing volume by month to support planning and collection decisions.",
                "month",
                buckets,
                date -> date.withDayOfMonth(1),
                labelFormatter::format,
                issueRecords
        );
    }

    private AdminDashboardChartSeries buildYearlyChartSeries(List<IssueRecord> issueRecords) {
        LocalDate currentYearStart = LocalDate.now().withDayOfYear(1);
        LocalDate startYear = currentYearStart.minusYears(4);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("yyyy", Locale.ENGLISH);
        List<LocalDate> buckets = IntStream.range(0, 5)
                .mapToObj(startYear::plusYears)
                .toList();
        return buildChartSeries(
                "year",
                "Yearly",
                "Year-over-year borrowing trend",
                "Keep an eye on broader circulation growth across the last five years.",
                "year",
                buckets,
                date -> date.withDayOfYear(1),
                labelFormatter::format,
                issueRecords
        );
    }

    private AdminDashboardChartSeries buildChartSeries(String key,
                                                       String label,
                                                       String title,
                                                       String description,
                                                       String bucketLabel,
                                                       List<LocalDate> buckets,
                                                       Function<LocalDate, LocalDate> bucketResolver,
                                                       Function<LocalDate, String> labelFormatter,
                                                       List<IssueRecord> issueRecords) {
        Map<LocalDate, long[]> chartData = new LinkedHashMap<>();
        for (LocalDate bucket : buckets) {
            chartData.put(bucket, new long[]{0L, 0L});
        }

        LocalDate firstBucket = buckets.get(0);
        LocalDate lastBucket = buckets.get(buckets.size() - 1);

        for (IssueRecord issueRecord : issueRecords) {
            if (issueRecord.getIssueDate() != null) {
                LocalDate issueBucket = bucketResolver.apply(issueRecord.getIssueDate().toLocalDate());
                if (!issueBucket.isBefore(firstBucket) && !issueBucket.isAfter(lastBucket) && chartData.containsKey(issueBucket)) {
                    chartData.get(issueBucket)[0]++;
                }
            }

            if (issueRecord.getReturnDate() != null) {
                LocalDate returnBucket = bucketResolver.apply(issueRecord.getReturnDate().toLocalDate());
                if (!returnBucket.isBefore(firstBucket) && !returnBucket.isAfter(lastBucket) && chartData.containsKey(returnBucket)) {
                    chartData.get(returnBucket)[1]++;
                }
            }
        }

        long issuedTotal = 0;
        long returnedTotal = 0;
        long peakIssued = 0;
        long peakReturned = 0;
        long chartMaxValue = 1;
        for (long[] values : chartData.values()) {
            issuedTotal += values[0];
            returnedTotal += values[1];
            peakIssued = Math.max(peakIssued, values[0]);
            peakReturned = Math.max(peakReturned, values[1]);
            chartMaxValue = Math.max(chartMaxValue, Math.max(values[0], values[1]));
        }

        final long maxValue = chartMaxValue;
        List<AdminDashboardChartPoint> points = chartData.entrySet().stream()
                .map(entry -> new AdminDashboardChartPoint(
                        labelFormatter.apply(entry.getKey()),
                        entry.getValue()[0],
                        entry.getValue()[1],
                        heightPercentage(entry.getValue()[0], maxValue),
                        heightPercentage(entry.getValue()[1], maxValue)
                ))
                .toList();

        return new AdminDashboardChartSeries(
                key,
                label,
                title,
                description,
                bucketLabel,
                points,
                issuedTotal,
                returnedTotal,
                peakIssued,
                peakReturned
        );
    }
}
