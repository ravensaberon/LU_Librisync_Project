package com.latteandletters.service;

import com.latteandletters.model.Book;
import com.latteandletters.model.AdminNotificationType;
import com.latteandletters.model.Reservation;
import com.latteandletters.model.ReservationRequestType;
import com.latteandletters.model.ReservationStatus;
import com.latteandletters.model.Student;
import com.latteandletters.model.IssueStatus;
import com.latteandletters.repository.BookRepository;
import com.latteandletters.repository.IssueRecordRepository;
import com.latteandletters.repository.ReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@SuppressWarnings("null")
public class ReservationService {

    private static final List<ReservationStatus> ACTIVE_STATUSES = List.of(ReservationStatus.PENDING, ReservationStatus.PENDING_APPROVAL, ReservationStatus.READY);

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final IssueRecordRepository issueRecordRepository;
    private final StudentService studentService;
    private final EmailNotificationService emailNotificationService;
    private final AdminNotificationService adminNotificationService;
    private final CirculationPolicyService circulationPolicyService;
    private final int claimWindowHours;
    private final int borrowRequestWindowMinutes;
    private final int maxPreferredPickupDays;

    public ReservationService(ReservationRepository reservationRepository,
                              BookRepository bookRepository,
                              IssueRecordRepository issueRecordRepository,
                              StudentService studentService,
                              EmailNotificationService emailNotificationService,
                              AdminNotificationService adminNotificationService,
                              CirculationPolicyService circulationPolicyService,
                              @org.springframework.beans.factory.annotation.Value("${latteandletters.reservations.claim-hours:24}") int claimWindowHours,
                              @org.springframework.beans.factory.annotation.Value("${latteandletters.reservations.borrow-request-minutes:30}") int borrowRequestWindowMinutes,
                              @org.springframework.beans.factory.annotation.Value("${latteandletters.reservations.max-preferred-pickup-days:30}") int maxPreferredPickupDays) {
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.issueRecordRepository = issueRecordRepository;
        this.studentService = studentService;
        this.emailNotificationService = emailNotificationService;
        this.adminNotificationService = adminNotificationService;
        this.circulationPolicyService = circulationPolicyService;
        this.claimWindowHours = Math.max(1, claimWindowHours);
        this.borrowRequestWindowMinutes = Math.max(1, borrowRequestWindowMinutes);
        this.maxPreferredPickupDays = Math.max(1, maxPreferredPickupDays);
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAllByOrderByReservedAtDesc();
    }

    public List<Reservation> getStudentReservations(String email) {
        Student student = studentService.getStudentByEmail(email);
        return reservationRepository.findByStudent_IdOrderByReservedAtDesc(student.getId());
    }

    public List<Reservation> getStudentBorrowRequests(String email) {
        Student student = studentService.getStudentByEmail(email);
        return reservationRepository.findByStudent_IdAndRequestTypeOrderByReservedAtDesc(student.getId(), ReservationRequestType.BORROW).stream()
                .filter(Reservation::isActive)
                .toList();
    }

    public List<Reservation> getStudentQueueReservations(String email) {
        Student student = studentService.getStudentByEmail(email);
        return reservationRepository.findByStudent_IdAndRequestTypeOrderByReservedAtDesc(student.getId(), ReservationRequestType.RESERVATION).stream()
                .filter(Reservation::isActive)
                .toList();
    }

    public List<Reservation> getBorrowRequests() {
        return reservationRepository.findByStatusInAndRequestTypeOrderByReservedAtAsc(ACTIVE_STATUSES, ReservationRequestType.BORROW);
    }

    public List<Reservation> getQueueReservations() {
        return reservationRepository.findByStatusInAndRequestTypeOrderByReservedAtAsc(ACTIVE_STATUSES, ReservationRequestType.RESERVATION);
    }

    public Reservation getReservationById(Long reservationId) {
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation is required.");
        }
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));
    }

    public Reservation getReservationByDeskQrCode(String qrCode) {
        if (qrCode == null || qrCode.isBlank()) {
            throw new IllegalArgumentException("Reservation QR code is required.");
        }

        String normalizedQrCode = qrCode.trim();
        String[] segments = normalizedQrCode.split("-");
        if (segments.length < 5
                || !"LL".equalsIgnoreCase(segments[0])
                || !"RES".equalsIgnoreCase(segments[1])) {
            throw new IllegalArgumentException("Scanned QR code is not a valid Latte and Letters reservation code.");
        }

        Long reservationId;
        try {
            reservationId = Long.parseLong(segments[2]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Scanned QR code is not a valid Latte and Letters reservation code.");
        }

        Reservation reservation = getReservationById(reservationId);
        if (!reservation.getDeskQrCode().equalsIgnoreCase(normalizedQrCode)) {
            throw new IllegalArgumentException("Scanned QR code does not match the reservation record.");
        }
        return reservation;
    }

    public long countPendingReservations() {
        return reservationRepository.findByStatusInOrderByReservedAtAsc(List.of(ReservationStatus.PENDING)).size();
    }

    public long countReadyReservations() {
        return reservationRepository.findByStatusInOrderByReservedAtAsc(List.of(ReservationStatus.READY)).size();
    }

    public Map<Long, Integer> getActiveQueueSizesByBook() {
        Map<Long, Integer> queueSizes = new LinkedHashMap<>();
        for (Reservation reservation : reservationRepository.findByStatusInAndRequestTypeOrderByReservedAtAsc(ACTIVE_STATUSES, ReservationRequestType.RESERVATION)) {
            Long bookId = reservation.getBook().getId();
            queueSizes.put(bookId, queueSizes.getOrDefault(bookId, 0) + 1);
        }
        return queueSizes;
    }

    public Map<Long, Integer> getReadyReservationCountsByBook() {
        Map<Long, Integer> readyCounts = new LinkedHashMap<>();
        for (Reservation reservation : reservationRepository.findByStatusInOrderByReservedAtAsc(List.of(ReservationStatus.READY))) {
            Long bookId = reservation.getBook().getId();
            readyCounts.put(bookId, readyCounts.getOrDefault(bookId, 0) + 1);
        }
        return readyCounts;
    }

    public Map<Long, String> getReservationStatusesForStudentBooks(String email) {
        Student student = studentService.getStudentByEmail(email);
        Map<Long, String> statuses = new LinkedHashMap<>();
        for (Reservation reservation : reservationRepository.findByStudent_IdOrderByReservedAtDesc(student.getId())) {
            if (reservation.isActive() && !statuses.containsKey(reservation.getBook().getId())) {
                statuses.put(reservation.getBook().getId(), reservation.getRequestType().name() + ":" + reservation.getStatus().name());
            }
        }
        return statuses;
    }

    public int getMaxPreferredPickupDays() {
        return maxPreferredPickupDays;
    }

    public int getBorrowRequestWindowMinutes() {
        return borrowRequestWindowMinutes;
    }

    @Transactional
    public Reservation placeBorrowRequest(Long bookId, String email) {
        if (bookId == null) {
            throw new IllegalArgumentException("Book is required.");
        }

        promoteReservationsForBook(bookId);
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found."));
        Student student = studentService.getStudentByEmail(email);
        circulationPolicyService.validateBorrowingEligibility(student);

        if (issueRecordRepository.existsByBook_IdAndStudent_IdAndStatusIn(bookId, student.getId(), List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE))) {
            throw new IllegalArgumentException("You already have this book on loan.");
        }
        if (reservationRepository.existsByBook_IdAndStudent_IdAndStatusIn(bookId, student.getId(), ACTIVE_STATUSES)) {
            throw new IllegalArgumentException("You already have an active pickup request or reservation for this book.");
        }
        if (getWalkInBorrowableCopyCount(book) < 1) {
            throw new IllegalArgumentException("No walk-in copy is available right now. Please join the reservation queue instead.");
        }

        Reservation reservation = new Reservation();
        reservation.setBook(book);
        reservation.setStudent(student);
        reservation.setRequestType(ReservationRequestType.BORROW);
        reservation.setStatus(ReservationStatus.PENDING_APPROVAL);
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setPreferredPickupDate(null);
        reservation.setQueuePosition(0);
        reservation.setExpiresAt(null);

        Reservation savedReservation = reservationRepository.save(reservation);
        adminNotificationService.notifyUser(
                student.getUser().getEmail(),
                AdminNotificationType.BORROW_STATUS,
                "Borrow request submitted",
                "Your borrow request for " + book.getTitle() + " is pending staff approval.",
                "/student/reservations?tab=borrow"
        );
        adminNotificationService.notifyAdmins(
                AdminNotificationType.BORROW_REQUEST,
                "New borrow request",
                student.getUser().getName() + " requested a walk-in borrow for " + book.getTitle() + ". Approve or deny from the reservation desk.",
                "/admin/issues#borrow-requests"
        );
        promoteReservationsForBook(bookId);
        return getReservationById(savedReservation.getId());
    }

    @Transactional
    public Reservation placeReservation(Long bookId, String email) {
        return placeReservation(bookId, email, null);
    }

    @Transactional
    public Reservation placeReservation(Long bookId, String email, LocalDate preferredPickupDate) {
        if (bookId == null) {
            throw new IllegalArgumentException("Book is required.");
        }

        promoteReservationsForBook(bookId);
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found."));
        Student student = studentService.getStudentByEmail(email);
        LocalDate normalizedPreferredPickupDate = normalizePreferredPickupDate(preferredPickupDate);
        circulationPolicyService.validateBorrowingEligibility(student);
        if (issueRecordRepository.existsByBook_IdAndStudent_IdAndStatusIn(bookId, student.getId(), List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE))) {
            throw new IllegalArgumentException("You already have this book on loan.");
        }
        if (reservationRepository.existsByBook_IdAndStudent_IdAndStatusIn(bookId, student.getId(), ACTIVE_STATUSES)) {
            throw new IllegalArgumentException("You already have an active reservation for this book.");
        }

        Reservation reservation = new Reservation();
        reservation.setBook(book);
        reservation.setStudent(student);
        reservation.setRequestType(ReservationRequestType.RESERVATION);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setPreferredPickupDate(normalizedPreferredPickupDate);
        reservation.setQueuePosition((int) reservationRepository.countByBook_IdAndStatusInAndRequestType(bookId, ACTIVE_STATUSES, ReservationRequestType.RESERVATION) + 1);

        Reservation savedReservation = reservationRepository.save(reservation);
        adminNotificationService.notifyUser(
                student.getUser().getEmail(),
                AdminNotificationType.RESERVATION_STATUS,
                "Reservation placed",
                "Your reservation for " + book.getTitle() + " was placed. You will be notified when a copy is ready for pickup.",
                "/student/reservations?tab=queue"
        );
        adminNotificationService.notifyAdmins(
                AdminNotificationType.RESERVATION_REQUEST,
                "New reservation request",
                student.getUser().getName() + " placed a reservation for " + book.getTitle() + ".",
                "/admin/issues#borrow-requests"
        );
        reindexQueue(bookId);
        promoteReservationsForBook(bookId);
        return getReservationById(savedReservation.getId());
    }

    @Transactional
    public void approveBorrowRequest(Long reservationId) {
        Reservation reservation = getReservationById(reservationId);
        if (!ReservationRequestType.BORROW.equals(reservation.getRequestType())) {
            throw new IllegalArgumentException("Only borrow requests can be approved.");
        }
        if (!ReservationStatus.PENDING_APPROVAL.equals(reservation.getStatus())) {
            throw new IllegalArgumentException("Only pending borrow requests can be approved.");
        }
        reservation.setStatus(ReservationStatus.READY);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(borrowRequestWindowMinutes));
        reservationRepository.save(reservation);
        emailNotificationService.queueReservationReadyNotification(reservation);
        adminNotificationService.notifyUserIfAbsent(
                reservation.getStudent().getUser().getEmail(),
                AdminNotificationType.BORROW_STATUS,
                "Borrow request approved",
                "Your borrow request for " + reservation.getBook().getTitle() + " was approved. You have " + borrowRequestWindowMinutes + " minutes to claim it at the circulation desk.",
                "/student/reservations?tab=borrow"
        );
    }

    @Transactional
    public void denyBorrowRequest(Long reservationId) {
        Reservation reservation = getReservationById(reservationId);
        if (!ReservationRequestType.BORROW.equals(reservation.getRequestType())) {
            throw new IllegalArgumentException("Only borrow requests can be denied.");
        }
        if (!ReservationStatus.PENDING_APPROVAL.equals(reservation.getStatus())) {
            throw new IllegalArgumentException("Only pending borrow requests can be denied.");
        }
        reservation.setStatus(ReservationStatus.DENIED);
        reservation.setExpiresAt(null);
        reservationRepository.save(reservation);
        adminNotificationService.notifyUser(
                reservation.getStudent().getUser().getEmail(),
                AdminNotificationType.BORROW_STATUS,
                "Borrow request denied",
                "Your borrow request for " + reservation.getBook().getTitle() + " was denied by library staff.",
                "/student/reservations?tab=borrow"
        );
        promoteReservationsForBook(reservation.getBook().getId());
    }

    @Transactional
    public void cancelReservationByStudent(Long reservationId, String email) {
        Student student = studentService.getStudentByEmail(email);
        Reservation reservation = getReservationById(reservationId);
        if (!reservation.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException("You can only cancel your own reservation.");
        }
        adminNotificationService.notifyUser(
                student.getUser().getEmail(),
                AdminNotificationType.RESERVATION_STATUS,
                reservation.isBorrowRequest() ? "Borrow request cancelled" : "Reservation cancelled",
                "Your " + (reservation.isBorrowRequest() ? "borrow request" : "reservation") + " for " + reservation.getBook().getTitle() + " was cancelled.",
                "/student/reservations"
        );
        cancelReservation(reservation);
    }

    @Transactional
    public void cancelReservationByAdmin(Long reservationId) {
        Reservation reservation = getReservationById(reservationId);
        adminNotificationService.notifyUser(
                reservation.getStudent().getUser().getEmail(),
                AdminNotificationType.RESERVATION_STATUS,
                reservation.isBorrowRequest() ? "Borrow request cancelled by staff" : "Reservation cancelled by staff",
                "Library staff cancelled your " + (reservation.isBorrowRequest() ? "borrow request" : "reservation") + " for " + reservation.getBook().getTitle() + ".",
                "/student/reservations"
        );
        cancelReservation(reservation);
    }

    @Transactional
    public void beforeIssueValidation(Long bookId, Long studentId) {
        promoteReservationsForBook(bookId);

        List<Reservation> activeReservations = reservationRepository.findByBook_IdAndStatusInOrderByQueuePositionAscReservedAtAsc(bookId, ACTIVE_STATUSES);
        if (activeReservations.isEmpty()) {
            return;
        }

        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            return;
        }

        boolean studentHasReadyReservation = activeReservations.stream()
                .anyMatch(reservation -> ReservationStatus.READY.equals(reservation.getStatus())
                        && reservation.getStudent().getId().equals(studentId));
        if (studentHasReadyReservation) {
            return;
        }

        int freeWalkInCopies = Math.max(0, getAvailableCopyCount(book) - (int) activeReservations.stream()
                .filter(reservation -> ReservationStatus.READY.equals(reservation.getStatus()))
                .count());
        if (freeWalkInCopies > 0) {
            return;
        }

        Reservation firstReadyReservation = activeReservations.stream()
                .filter(reservation -> ReservationStatus.READY.equals(reservation.getStatus()))
                .findFirst()
                .orElse(null);
        if (firstReadyReservation != null && !firstReadyReservation.getStudent().getId().equals(studentId)) {
            throw new IllegalArgumentException("This copy is reserved for the next student in the queue.");
        }
    }

    @Transactional
    public void markReservationClaimed(Long bookId, Long studentId) {
        List<Reservation> activeReservations = reservationRepository.findByBook_IdAndStatusInOrderByQueuePositionAscReservedAtAsc(bookId, ACTIVE_STATUSES);
        for (Reservation reservation : activeReservations) {
            if (reservation.getStudent().getId().equals(studentId)) {
                emailNotificationService.cancelReservationReadyNotification(reservation);
                reservation.setStatus(ReservationStatus.CLAIMED);
                reservation.setExpiresAt(null);
                reservationRepository.save(reservation);
                adminNotificationService.notifyUser(
                        reservation.getStudent().getUser().getEmail(),
                        AdminNotificationType.BORROW_STATUS,
                        "Pickup confirmed",
                        "Your " + (reservation.isBorrowRequest() ? "borrow request" : "reservation pickup") + " for " + reservation.getBook().getTitle() + " was confirmed by staff.",
                        "/student/history"
                );
                reindexQueue(bookId);
                return;
            }
        }
    }

    @Transactional
    public void promoteReservationsForBook(Long bookId) {
        if (bookId == null) {
            return;
        }

        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            return;
        }

        List<Reservation> activeReservations = reservationRepository.findByBook_IdAndStatusInOrderByQueuePositionAscReservedAtAsc(bookId, ACTIVE_STATUSES);
        List<Reservation> queueReservations = activeReservations.stream()
                .filter(Reservation::isQueueReservation)
                .toList();
        if (activeReservations.isEmpty()) {
            return;
        }

        int availableCopies = book.getAvailableQuantity() == null ? 0 : book.getAvailableQuantity();
        long readyCount = activeReservations.stream()
                .filter(reservation -> ReservationStatus.READY.equals(reservation.getStatus())
                        || ReservationStatus.PENDING_APPROVAL.equals(reservation.getStatus()))
                .count();
        int promotableSlots = Math.max(0, availableCopies - (int) readyCount);
        if (promotableSlots < 1) {
            reindexQueue(bookId);
            return;
        }

        for (Reservation reservation : queueReservations) {
            if (promotableSlots < 1) {
                break;
            }
            if (ReservationStatus.PENDING.equals(reservation.getStatus()) && isReadyForPickup(reservation)) {
                reservation.setStatus(ReservationStatus.READY);
                reservation.setExpiresAt(LocalDateTime.now().plusHours(claimWindowHours));
                reservationRepository.save(reservation);
                emailNotificationService.queueReservationReadyNotification(reservation);
                adminNotificationService.notifyUserIfAbsent(
                        reservation.getStudent().getUser().getEmail(),
                        AdminNotificationType.RESERVATION_STATUS,
                        reservation.isBorrowRequest() ? "Borrow request ready" : "Reservation ready for pickup",
                        reservation.isBorrowRequest()
                                ? "Your borrow request for " + reservation.getBook().getTitle() + " is ready at the circulation desk."
                                : "Your reservation for " + reservation.getBook().getTitle() + " is now ready for pickup.",
                        "/student/reservations?tab=" + (reservation.isBorrowRequest() ? "borrow" : "queue")
                );
                promotableSlots--;
            }
        }

        reindexQueue(bookId);
    }

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void expireReadyReservations() {
        List<Reservation> expiredReservations = reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.READY, LocalDateTime.now());
        for (Reservation reservation : expiredReservations) {
            emailNotificationService.cancelReservationReadyNotification(reservation);
            emailNotificationService.queueReservationExpiredNotification(reservation);
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservation.setExpiresAt(null);
            reservationRepository.save(reservation);
            adminNotificationService.notifyUser(
                    reservation.getStudent().getUser().getEmail(),
                    AdminNotificationType.RESERVATION_STATUS,
                    reservation.isBorrowRequest() ? "Borrow request expired" : "Reservation expired",
                    "Your " + (reservation.isBorrowRequest() ? "borrow request" : "reservation hold") + " for " + reservation.getBook().getTitle() + " expired before pickup.",
                    "/student/reservations"
            );
            reindexQueue(reservation.getBook().getId());
            promoteReservationsForBook(reservation.getBook().getId());
        }
    }

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void syncReadyReservations() {
        Set<Long> processedBookIds = new LinkedHashSet<>();
        for (Reservation reservation : reservationRepository.findByStatusInOrderByReservedAtAsc(ACTIVE_STATUSES)) {
            if (processedBookIds.add(reservation.getBook().getId())) {
                promoteReservationsForBook(reservation.getBook().getId());
            }
        }
    }

    private void cancelReservation(Reservation reservation) {
        if (!reservation.isActive()) {
            throw new IllegalArgumentException("Only active reservations can be cancelled.");
        }

        emailNotificationService.cancelReservationReadyNotification(reservation);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setExpiresAt(null);
        reservationRepository.save(reservation);
        reindexQueue(reservation.getBook().getId());
        promoteReservationsForBook(reservation.getBook().getId());
    }

    private void reindexQueue(Long bookId) {
        List<Reservation> activeReservations = reservationRepository.findByBook_IdAndStatusInOrderByQueuePositionAscReservedAtAsc(bookId, ACTIVE_STATUSES);
        List<Reservation> queueReservations = activeReservations.stream()
                .filter(Reservation::isQueueReservation)
                .sorted(Comparator
                .comparing(this::resolvePriorityDate)
                .thenComparing(Reservation::getReservedAt)
                .thenComparing(Reservation::getId))
                .toList();
        int index = 1;
        for (Reservation reservation : queueReservations) {
            reservation.setQueuePosition(index++);
        }
        if (!queueReservations.isEmpty()) {
            reservationRepository.saveAll(queueReservations);
        }
    }

    private int getWalkInBorrowableCopyCount(Book book) {
        if (book == null || book.getId() == null) {
            return 0;
        }
        int readyOrPendingCount = (int) reservationRepository.countByBook_IdAndStatusIn(book.getId(), List.of(ReservationStatus.READY, ReservationStatus.PENDING_APPROVAL));
        return Math.max(0, getAvailableCopyCount(book) - readyOrPendingCount);
    }

    private int getAvailableCopyCount(Book book) {
        return book == null || book.getAvailableQuantity() == null ? 0 : Math.max(0, book.getAvailableQuantity());
    }

    private boolean isReadyForPickup(Reservation reservation) {
        LocalDate preferredPickupDate = reservation.getPreferredPickupDate();
        return preferredPickupDate == null || !preferredPickupDate.isAfter(LocalDate.now());
    }

    private LocalDate normalizePreferredPickupDate(LocalDate preferredPickupDate) {
        if (preferredPickupDate == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        if (preferredPickupDate.isBefore(today)) {
            throw new IllegalArgumentException("Preferred pickup date cannot be in the past.");
        }
        if (preferredPickupDate.isAfter(today.plusDays(maxPreferredPickupDays))) {
            throw new IllegalArgumentException("Preferred pickup date is too far ahead.");
        }
        return preferredPickupDate;
    }

    private LocalDate resolvePriorityDate(Reservation reservation) {
        if (reservation.getPreferredPickupDate() != null) {
            return reservation.getPreferredPickupDate();
        }
        return reservation.getReservedAt() == null ? LocalDate.now() : reservation.getReservedAt().toLocalDate();
    }
}
