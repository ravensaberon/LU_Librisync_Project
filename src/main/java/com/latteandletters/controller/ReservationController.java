package com.latteandletters.controller;

import com.latteandletters.service.AuditLogService;
import com.latteandletters.service.CirculationPolicyService;
import com.latteandletters.service.IssueService;
import com.latteandletters.service.ReservationService;
import com.latteandletters.util.PaginationUtils;
import com.latteandletters.model.Reservation;
import com.latteandletters.model.ReservationStatus;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class ReservationController {

    private static final int RESERVATION_SECTION_PAGE_SIZE = 8;

    private final ReservationService reservationService;
    private final IssueService issueService;
    private final AuditLogService auditLogService;
    private final CirculationPolicyService circulationPolicyService;

    public ReservationController(ReservationService reservationService,
                                 IssueService issueService,
                                 AuditLogService auditLogService,
                                 CirculationPolicyService circulationPolicyService) {
        this.reservationService = reservationService;
        this.issueService = issueService;
        this.auditLogService = auditLogService;
        this.circulationPolicyService = circulationPolicyService;
    }

    @GetMapping("/student/reservations")
    public String studentReservations(Authentication authentication,
                                      @RequestParam(defaultValue = "borrow") String tab,
                                      @RequestParam(defaultValue = "1") Integer borrowPage,
                                      @RequestParam(defaultValue = "1") Integer queuePage,
                                      Model model) {
        reservationService.syncReadyReservations();
        var borrowRequests = reservationService.getStudentBorrowRequests(authentication.getName());
        var queueReservations = reservationService.getStudentQueueReservations(authentication.getName());
        var borrowRequestsPage = PaginationUtils.paginate(borrowRequests, borrowPage, RESERVATION_SECTION_PAGE_SIZE);
        var queueReservationsPage = PaginationUtils.paginate(queueReservations, queuePage, RESERVATION_SECTION_PAGE_SIZE);
        model.addAttribute("borrowRequests", borrowRequestsPage.getItems());
        model.addAttribute("borrowRequestsPage", borrowRequestsPage);
        model.addAttribute("queueReservations", queueReservationsPage.getItems());
        model.addAttribute("queueReservationsPage", queueReservationsPage);
        model.addAttribute("borrowRequestWindowMinutes", reservationService.getBorrowRequestWindowMinutes());
        model.addAttribute("reservationScheduleMaxDate", LocalDate.now().plusDays(reservationService.getMaxPreferredPickupDays()));
        model.addAttribute("defaultIssueDueDate", LocalDate.now().plusDays(circulationPolicyService.getMaxLoanDays()));
        model.addAttribute("activeTab", "queue".equalsIgnoreCase(tab) ? "queue" : "borrow");
        return "student/reservations";
    }

    @PostMapping("/student/reservations")
    public String placeReservation(@RequestParam Long bookId,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate preferredPickupDate,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            var reservation = reservationService.placeReservation(bookId, authentication.getName(), preferredPickupDate);
            auditLogService.log(
                    authentication.getName(),
                    "RESERVATION_CREATED",
                    "RESERVATION",
                    reservation.getId().toString(),
                    "Reservation placed",
                    "Book: " + reservation.getBook().getTitle()
                            + " | Queue: " + reservation.getQueuePosition()
            );
            if (ReservationStatus.READY.equals(reservation.getStatus())) {
                redirectAttributes.addFlashAttribute("success", "Reservation placed. A copy is ready — you have 24 hours to claim it at the circulation desk.");
            } else {
                redirectAttributes.addFlashAttribute("success", "Reservation placed. You'll be notified when a copy is ready for pickup.");
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/catalog";
    }

    @PostMapping("/student/reservations/{reservationId}/claim")
    public String claimStudentReservation(@PathVariable Long reservationId,
                                          @RequestParam(defaultValue = "borrow") String tab,
                                          @RequestParam(defaultValue = "1") Integer borrowPage,
                                          @RequestParam(defaultValue = "1") Integer queuePage,
                                          Authentication authentication,
                                          RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("info", "Please proceed to the circulation desk. Staff must confirm the physical pickup before the book is issued to your account.");
        return "redirect:/student/reservations?tab=" + ("queue".equalsIgnoreCase(tab) ? "queue" : "borrow")
                + "&borrowPage=" + Math.max(1, borrowPage)
                + "&queuePage=" + Math.max(1, queuePage);
    }

    @PostMapping("/student/reservations/{reservationId}/cancel")
    public String cancelStudentReservation(@PathVariable Long reservationId,
                                           @RequestParam(defaultValue = "borrow") String tab,
                                           @RequestParam(defaultValue = "1") Integer borrowPage,
                                           @RequestParam(defaultValue = "1") Integer queuePage,
                                           Authentication authentication,
                                           RedirectAttributes redirectAttributes) {
        try {
            reservationService.cancelReservationByStudent(reservationId, authentication.getName());
            auditLogService.log(
                    authentication.getName(),
                    "RESERVATION_CANCELLED",
                    "RESERVATION",
                    reservationId.toString(),
                    "Reservation cancelled by student",
                    "Student cancelled reservation " + reservationId + "."
            );
            redirectAttributes.addFlashAttribute("success", "Reservation cancelled successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/reservations?tab=" + ("queue".equalsIgnoreCase(tab) ? "queue" : "borrow")
                + "&borrowPage=" + Math.max(1, borrowPage)
                + "&queuePage=" + Math.max(1, queuePage);
    }

    @GetMapping("/admin/reservations")
    public String adminReservations(@RequestParam(defaultValue = "1") Integer queuePage,
                                    Model model) {
        reservationService.syncReadyReservations();
        var queueReservations = reservationService.getQueueReservations();
        var queueReservationsPage = PaginationUtils.paginate(queueReservations, queuePage, RESERVATION_SECTION_PAGE_SIZE);
        model.addAttribute("queueReservations", queueReservationsPage.getItems());
        model.addAttribute("queueReservationsPage", queueReservationsPage);
        model.addAttribute("reservationCount", queueReservations.size());
        model.addAttribute("pendingReservationCount", queueReservations.stream().filter(r -> r.getStatus().name().equals("PENDING")).count());
        model.addAttribute("readyReservationCount", queueReservations.stream().filter(r -> r.getStatus().name().equals("READY")).count());
        model.addAttribute("defaultDueDate", java.time.LocalDate.now().plusDays(7));
        return "admin/reservations";
    }

    @PostMapping("/admin/reservations/{reservationId}/approve")
    public String approveBorrowRequest(@PathVariable Long reservationId,
                                       @RequestParam(required = false) String remarks,
                                       @RequestParam(defaultValue = "1") Integer borrowPage,
                                       @RequestParam(defaultValue = "1") Integer queuePage,
                                       @RequestParam(defaultValue = "reservations") String source,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            Reservation reservation = reservationService.getReservationById(reservationId);
            if (!reservation.isBorrowRequest()) {
                throw new IllegalArgumentException("Only borrow requests can be approved from Issue / Return.");
            }
            claimReservationForPickup(reservation, remarks, authentication, redirectAttributes);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return buildAdminReservationRedirect(source, borrowPage, queuePage);
    }

    @PostMapping("/admin/reservations/{reservationId}/deny")
    public String denyBorrowRequest(@PathVariable Long reservationId,
                                    @RequestParam(defaultValue = "1") Integer borrowPage,
                                    @RequestParam(defaultValue = "1") Integer queuePage,
                                    @RequestParam(defaultValue = "reservations") String source,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            reservationService.denyBorrowRequest(reservationId);
            auditLogService.log(
                    authentication.getName(),
                    "BORROW_REQUEST_DENIED",
                    "RESERVATION",
                    reservationId.toString(),
                    "Borrow request denied by admin",
                    "Admin denied borrow request " + reservationId + "."
            );
            redirectAttributes.addFlashAttribute("success", "Borrow request denied. Student has been notified.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return buildAdminReservationRedirect(source, borrowPage, queuePage);
    }

    @PostMapping("/admin/reservations/{reservationId}/claim")
    public String claimReservation(@PathVariable Long reservationId,
                                   @RequestParam(required = false) String remarks,
                                   @RequestParam(defaultValue = "1") Integer borrowPage,
                                   @RequestParam(defaultValue = "1") Integer queuePage,
                                   @RequestParam(defaultValue = "reservations") String source,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            Reservation reservation = reservationService.getReservationById(reservationId);
            claimReservationForPickup(reservation, remarks, authentication, redirectAttributes);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return buildAdminReservationRedirect(source, borrowPage, queuePage);
    }

    @PostMapping("/admin/reservations/claim-by-qr")
    public String claimReservationByQr(@RequestParam String qrCode,
                                       @RequestParam(required = false) String remarks,
                                       @RequestParam(defaultValue = "1") Integer borrowPage,
                                       @RequestParam(defaultValue = "1") Integer queuePage,
                                       @RequestParam(defaultValue = "reservations") String source,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            Reservation reservation = reservationService.getReservationByDeskQrCode(qrCode);
            claimReservationForPickup(reservation, remarks, authentication, redirectAttributes);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return buildAdminReservationRedirect(source, borrowPage, queuePage);
    }

    @GetMapping("/admin/reservations/claim-by-qr")
    public String claimReservationByQrFallback(@RequestParam(required = false) String qrCode,
                                               @RequestParam(required = false) String remarks,
                                               @RequestParam(defaultValue = "1") Integer borrowPage,
                                               @RequestParam(defaultValue = "1") Integer queuePage,
                                               @RequestParam(defaultValue = "reservations") String source,
                                               Authentication authentication,
                                               RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "The QR claim form did not submit correctly. Please scan the student's QR again, then press Confirm and issue.");
        return buildAdminReservationRedirect(source, borrowPage, queuePage);
    }

    @PostMapping("/admin/reservations/{reservationId}/cancel")
    public String cancelAdminReservation(@PathVariable Long reservationId,
                                         @RequestParam(defaultValue = "1") Integer borrowPage,
                                         @RequestParam(defaultValue = "1") Integer queuePage,
                                         @RequestParam(defaultValue = "reservations") String source,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        try {
            reservationService.cancelReservationByAdmin(reservationId);
            auditLogService.log(
                    authentication.getName(),
                    "RESERVATION_CANCELLED",
                    "RESERVATION",
                    reservationId.toString(),
                    "Reservation cancelled by admin",
                    "Reservation " + reservationId + " was cancelled by admin."
            );
            redirectAttributes.addFlashAttribute("success", "Reservation cancelled successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return buildAdminReservationRedirect(source, borrowPage, queuePage);
    }

    private String buildAdminReservationRedirect(String source, int borrowPage, int queuePage) {
        if ("issues".equals(source)) {
            return "redirect:/admin/issues?borrowPage=" + Math.max(1, borrowPage) + "#borrow-requests";
        }
        return "redirect:/admin/reservations?borrowPage=" + Math.max(1, borrowPage) + "&queuePage=" + Math.max(1, queuePage);
    }

    private void claimReservationForPickup(Reservation reservation,
                                           String remarks,
                                           Authentication authentication,
                                           RedirectAttributes redirectAttributes) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found.");
        }

        var issueRecord = issueService.issueReservationPickup(reservation.getId(), authentication.getName(), remarks);
        auditLogService.log(
                authentication.getName(),
                "RESERVATION_CLAIMED",
                "RESERVATION",
                reservation.getId().toString(),
                "Reserved book issued by staff",
                "Reservation claimed and issued as " + issueRecord.getQrIssueCode()
        );
        redirectAttributes.addFlashAttribute("success", "Book issued successfully.");
    }
}
