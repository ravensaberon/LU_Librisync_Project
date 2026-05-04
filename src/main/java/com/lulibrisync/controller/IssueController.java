package com.lulibrisync.controller;

import com.lulibrisync.service.AuditLogService;
import com.lulibrisync.service.BookService;
import com.lulibrisync.service.CirculationPolicyService;
import com.lulibrisync.service.IssueService;
import com.lulibrisync.service.ReservationService;
import com.lulibrisync.service.StudentService;
import com.lulibrisync.util.PaginationUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class IssueController {

    private static final int ACTIVE_ISSUES_PAGE_SIZE = 10;
    private static final int ISSUE_HISTORY_PAGE_SIZE = 5;

    private final IssueService issueService;
    private final BookService bookService;
    private final StudentService studentService;
    private final AuditLogService auditLogService;
    private final CirculationPolicyService circulationPolicyService;
    private final ReservationService reservationService;

    public IssueController(IssueService issueService,
                           BookService bookService,
                           StudentService studentService,
                           AuditLogService auditLogService,
                           CirculationPolicyService circulationPolicyService,
                           ReservationService reservationService) {
        this.issueService = issueService;
        this.bookService = bookService;
        this.studentService = studentService;
        this.auditLogService = auditLogService;
        this.circulationPolicyService = circulationPolicyService;
        this.reservationService = reservationService;
    }

    @GetMapping("/admin/issues")
    public String issues(@RequestParam(required = false) Long editId,
                         @RequestParam(defaultValue = "1") Integer activePage,
                         @RequestParam(defaultValue = "1") Integer historyPage,
                         @RequestParam(defaultValue = "1") Integer borrowPage,
                         Model model) {
        reservationService.syncReadyReservations();
        var activeIssues = issueService.getActiveIssues();
        var issueHistory = issueService.getAllIssues();
        var borrowRequests = reservationService.getBorrowRequests();
        var activeIssuesPage = PaginationUtils.paginate(activeIssues, activePage, ACTIVE_ISSUES_PAGE_SIZE);
        var issueHistoryPage = PaginationUtils.paginate(issueHistory, historyPage, ISSUE_HISTORY_PAGE_SIZE);
        var borrowRequestsPage = PaginationUtils.paginate(borrowRequests, borrowPage, 10);
        model.addAttribute("activeIssues", activeIssuesPage.getItems());
        model.addAttribute("activeIssuesPage", activeIssuesPage);
        model.addAttribute("issueHistory", issueHistoryPage.getItems());
        model.addAttribute("issueHistoryPage", issueHistoryPage);
        model.addAttribute("borrowRequests", borrowRequestsPage.getItems());
        model.addAttribute("borrowRequestsPage", borrowRequestsPage);
        model.addAttribute("availableBooks", bookService.getAvailableBooks());
        model.addAttribute("students", studentService.searchStudents(null));
        model.addAttribute("defaultDueDate", LocalDate.now().plusDays(7));
        model.addAttribute("activeIssueCount", activeIssues.size());
        model.addAttribute("historyCount", issueHistory.size());
        model.addAttribute("overdueIssueCount", activeIssues.stream().filter(issue -> issue.getStatus().name().equals("OVERDUE")).count());
        model.addAttribute("pendingReturnRequestCount", issueService.countPendingReturnRequests());
        model.addAttribute("borrowRequestCount", borrowRequests.size());
        model.addAttribute("pendingBorrowRequestCount", borrowRequests.stream().filter(r -> r.getStatus().name().equals("PENDING_APPROVAL")).count());
        model.addAttribute("readyBorrowRequestCount", borrowRequests.stream().filter(r -> r.getStatus().name().equals("READY")).count());
        model.addAttribute("maxLoanDays", circulationPolicyService.getMaxLoanDays());
        model.addAttribute("maxActiveLoans", circulationPolicyService.getMaxActiveLoans());
        if (editId != null) {
            var editIssue = issueService.getIssueById(editId);
            model.addAttribute("editIssue", editIssue);
            model.addAttribute("editIssueDueDate", editIssue.getDueDate().toLocalDate());
        }
        return "issues/manage";
    }

    @PostMapping("/admin/issues")
    public String issueBook(@RequestParam Long bookId,
                            @RequestParam Long studentId,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                            @RequestParam(required = false) String remarks,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            var issueRecord = issueService.issueBook(bookId, studentId, dueDate, authentication.getName(), remarks);
            auditLogService.log(
                    authentication.getName(),
                    "BOOK_ISSUED",
                    "ISSUE_RECORD",
                    issueRecord.getId().toString(),
                    "Book issued",
                    "Issue code: " + issueRecord.getQrIssueCode() + " | Borrower: " + issueRecord.getStudent().getStudentId()
            );
            redirectAttributes.addFlashAttribute("success", "Book issued successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/issues";
    }

    @PostMapping("/admin/issues/{issueId}/update")
    public String updateIssue(@PathVariable Long issueId,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                              @RequestParam(required = false) String remarks,
                              @RequestParam(defaultValue = "1") Integer activePage,
                              @RequestParam(defaultValue = "1") Integer historyPage,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            var issueRecord = issueService.updateIssue(issueId, dueDate, remarks);
            auditLogService.log(
                    authentication.getName(),
                    "ISSUE_UPDATED",
                    "ISSUE_RECORD",
                    issueId.toString(),
                    "Issue record updated",
                    "Due date: " + issueRecord.getDueDate() + " | Fine: " + issueRecord.getFineAmount()
            );
            redirectAttributes.addFlashAttribute("success", "Issue record updated successfully.");
            return buildIssueRedirect(activePage, historyPage, null);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return buildIssueRedirect(activePage, historyPage, issueId);
        }
    }

    @PostMapping("/admin/issues/{issueId}/return")
    public String returnBook(@PathVariable Long issueId,
                             @RequestParam(defaultValue = "1") Integer activePage,
                             @RequestParam(defaultValue = "1") Integer historyPage,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            var issueRecord = issueService.returnBook(issueId);
            auditLogService.log(
                    authentication.getName(),
                    "BOOK_RETURNED",
                    "ISSUE_RECORD",
                    issueId.toString(),
                    "Book return confirmed at desk",
                    "Issue code: " + issueRecord.getQrIssueCode() + " | Fine: " + issueRecord.getFineAmount()
            );
            redirectAttributes.addFlashAttribute("success", "Book return confirmed successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return buildIssueRedirect(activePage, historyPage, null);
    }

    @PostMapping("/student/issues/{issueId}/request-return")
    public String requestReturnByStudent(@PathVariable Long issueId,
                                         @RequestParam(defaultValue = "/student/history") String redirectTo,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        try {
            var issueRecord = issueService.requestReturnByStudent(issueId, authentication.getName());
            auditLogService.log(
                    authentication.getName(),
                    "RETURN_REQUESTED",
                    "ISSUE_RECORD",
                    issueId.toString(),
                    "Student requested a desk return",
                    "Issue code: " + issueRecord.getQrIssueCode()
            );
            redirectAttributes.addFlashAttribute("success", "Return request sent. Please hand the physical book to the circulation desk for confirmation.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:" + resolveStudentRedirect(redirectTo);
    }

    @PostMapping("/student/issues/{issueId}/cancel-return-request")
    public String cancelReturnRequestByStudent(@PathVariable Long issueId,
                                               @RequestParam(defaultValue = "/student/history") String redirectTo,
                                               Authentication authentication,
                                               RedirectAttributes redirectAttributes) {
        try {
            var issueRecord = issueService.cancelReturnRequestByStudent(issueId, authentication.getName());
            auditLogService.log(
                    authentication.getName(),
                    "RETURN_REQUEST_CANCELLED",
                    "ISSUE_RECORD",
                    issueId.toString(),
                    "Student cancelled a desk return request",
                    "Issue code: " + issueRecord.getQrIssueCode()
            );
            redirectAttributes.addFlashAttribute("success", "Return request cancelled.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:" + resolveStudentRedirect(redirectTo);
    }

    @PostMapping("/admin/issues/{issueId}/delete")
    public String deleteIssue(@PathVariable Long issueId,
                              @RequestParam(defaultValue = "1") Integer activePage,
                              @RequestParam(defaultValue = "1") Integer historyPage,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            var issueRecord = issueService.getIssueById(issueId);
            issueService.deleteIssue(issueId);
            auditLogService.log(
                    authentication.getName(),
                    "ISSUE_DELETED",
                    "ISSUE_RECORD",
                    issueId.toString(),
                    "Issue record deleted",
                    "Issue code: " + issueRecord.getQrIssueCode()
            );
            redirectAttributes.addFlashAttribute("success", "Issue record deleted successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return buildIssueRedirect(activePage, historyPage, null);
    }

    private String buildIssueRedirect(Integer activePage, Integer historyPage, Long editId) {
        StringBuilder redirect = new StringBuilder("redirect:/admin/issues?activePage=")
                .append(activePage == null ? 1 : Math.max(1, activePage))
                .append("&historyPage=")
                .append(historyPage == null ? 1 : Math.max(1, historyPage));
        if (editId != null) {
            redirect.append("&editId=").append(editId);
        }
        return redirect.toString();
    }

    private String resolveStudentRedirect(String redirectTo) {
        if (redirectTo == null || redirectTo.isBlank()) {
            return "/student/history";
        }
        if (redirectTo.startsWith("/student/")) {
            return redirectTo;
        }
        return "/student/history";
    }
}
