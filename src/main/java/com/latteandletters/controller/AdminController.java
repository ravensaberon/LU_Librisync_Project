package com.latteandletters.controller;

import com.latteandletters.dto.BorrowerStanding;
import com.latteandletters.model.Fine;
import com.latteandletters.model.IssueRecord;
import com.latteandletters.model.IssueStatus;
import com.latteandletters.model.Student;
import com.latteandletters.model.User;
import com.latteandletters.model.UserStatus;
import com.latteandletters.repository.BookRepository;
import com.latteandletters.repository.IssueRecordRepository;
import com.latteandletters.service.AuditLogService;
import com.latteandletters.service.AdminService;
import com.latteandletters.service.AdminNotificationService;
import com.latteandletters.service.AuthService;
import com.latteandletters.service.FineService;
import com.latteandletters.service.IssueService;
import com.latteandletters.service.ReservationService;
import com.latteandletters.service.StudentService;
import com.latteandletters.util.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private static final int STUDENT_DIRECTORY_PAGE_SIZE = 10;

    private final BookRepository bookRepository;
    private final IssueRecordRepository issueRecordRepository;
    private final IssueService issueService;
    private final StudentService studentService;
    private final AdminService adminService;
    private final AdminNotificationService adminNotificationService;
    private final AuthService authService;
    private final ReservationService reservationService;
    private final FineService fineService;
    private final AuditLogService auditLogService;

    public AdminController(BookRepository bookRepository,
                           IssueRecordRepository issueRecordRepository,
                           IssueService issueService,
                           StudentService studentService,
                           AdminService adminService,
                           AdminNotificationService adminNotificationService,
                           AuthService authService,
                           ReservationService reservationService,
                           FineService fineService,
                           AuditLogService auditLogService) {
        this.bookRepository = bookRepository;
        this.issueRecordRepository = issueRecordRepository;
        this.issueService = issueService;
        this.studentService = studentService;
        this.adminService = adminService;
        this.adminNotificationService = adminNotificationService;
        this.authService = authService;
        this.reservationService = reservationService;
        this.fineService = fineService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication,
                            @RequestParam(defaultValue = "1") Integer auditPage,
                            Model model) {
        issueService.refreshOverdueStatuses();
        long issuedCount = issueRecordRepository.countByStatus(IssueStatus.ISSUED);
        long overdueCount = issueRecordRepository.countByStatus(IssueStatus.OVERDUE);
        long blockedBorrowerCount;
        try {
            blockedBorrowerCount = studentService.searchStudents(null).stream()
                    .map(studentService::getBorrowerStanding)
                    .filter(BorrowerStanding::isBlocked)
                    .count();
        } catch (IllegalArgumentException exception) {
            logger.warn("Unable to calculate blocked borrower count for admin dashboard.", exception);
            blockedBorrowerCount = 0;
        }

        model.addAttribute("bookCount", bookRepository.count());
        model.addAttribute("availableCount", bookRepository.countByAvailableQuantityGreaterThan(0));
        model.addAttribute("studentCount", studentService.countActiveStudents());
        model.addAttribute("issuedCount", issuedCount);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("overdueRate", (issuedCount + overdueCount) == 0 ? 0 : (overdueCount * 100) / (issuedCount + overdueCount));
        model.addAttribute("recentIssues", issueService.getRecentIssues());
        model.addAttribute("mostBorrowedBooks", issueService.getMostBorrowedBooks());
        model.addAttribute("circulationChartSeries", issueService.getCirculationChartSeries());
        var recentAuditLogsPage = PaginationUtils.paginate(auditLogService.getRecentLogs(), auditPage, 4);
        model.addAttribute("recentAuditLogsPage", recentAuditLogsPage);
        model.addAttribute("pendingReservationCount", reservationService.countPendingReservations());
        model.addAttribute("readyReservationCount", reservationService.countReadyReservations());
        model.addAttribute("outstandingFineCount", fineService.countOutstandingFines());
        model.addAttribute("outstandingFineTotal", fineService.getOutstandingFineTotal());
        model.addAttribute("blockedBorrowerCount", blockedBorrowerCount);
        model.addAttribute("recentOutstandingFines", fineService.getRecentOutstandingFines());
        return "admin/dashboard";
    }

    @PostMapping("/notifications/read-all")
    @ResponseBody
    public Map<String, Object> markAllNotificationsRead(Authentication authentication) {
        adminNotificationService.markAllAsRead(authentication.getName());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("unreadCount", 0);
        return response;
    }

    @GetMapping("/notifications")
    public String notifications() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/notifications/panel")
    @ResponseBody
    public Map<String, Object> notificationPanel(Authentication authentication) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("unreadCount", adminNotificationService.countUnreadNotifications(authentication.getName()));
        response.put("items", adminNotificationService.getRecentNotifications(authentication.getName(), 5).stream()
                .map(this::serializeAdminNotification)
                .toList());
        return response;
    }

    @GetMapping("/notifications/history")
    @ResponseBody
    public Map<String, Object> notificationHistory(Authentication authentication,
                                                   @RequestParam(defaultValue = "1") Integer page) {
        var notificationPage = adminNotificationService.getNotificationPage(authentication.getName(), page, 8);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", notificationPage.getItems().stream()
                .map(this::serializeAdminNotification)
                .toList());
        response.put("page", notificationPage.getPage());
        response.put("totalPages", notificationPage.getTotalPages());
        response.put("hasPrevious", notificationPage.isHasPrevious());
        response.put("hasNext", notificationPage.isHasNext());
        response.put("previousPage", notificationPage.getPreviousPage());
        response.put("nextPage", notificationPage.getNextPage());
        response.put("startPage", notificationPage.getStartPage());
        response.put("endPage", notificationPage.getEndPage());
        response.put("totalItems", notificationPage.getTotalItems());
        response.put("unreadCount", adminNotificationService.countUnreadNotifications(authentication.getName()));
        return response;
    }

    @GetMapping("/students")
    public String students(@RequestParam(required = false) String studentId,
                           @RequestParam(required = false) String modalStudentId,
                           @RequestParam(defaultValue = "active") String view,
                           @RequestParam(defaultValue = "1") Integer page,
                           Model model) {
        boolean archivedView = "archived".equalsIgnoreCase(view);
        List<Student> students = studentService.searchStudents(studentId, archivedView);
        List<Student> activeStudents = studentService.searchStudents(null, false);
        List<Student> archivedStudents = studentService.searchStudents(null, true);
        var studentsPage = PaginationUtils.paginate(students, page, STUDENT_DIRECTORY_PAGE_SIZE);
        Map<String, BorrowerStanding> borrowerStandingByStudentId = students.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Student::getStudentId,
                        studentService::getBorrowerStanding,
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));
        long blockedStudentCount = borrowerStandingByStudentId.values().stream()
                .filter(BorrowerStanding::isBlocked)
                .count();
        long borrowingClearedCount = borrowerStandingByStudentId.values().stream()
                .filter(BorrowerStanding::isEligibleToBorrow)
                .count();
        long activeAccountCount = students.stream()
                .filter(student -> UserStatus.ACTIVE.equals(student.getUser().getStatus()))
                .count();

        model.addAttribute("students", studentsPage.getItems());
        model.addAttribute("studentsPage", studentsPage);
        model.addAttribute("studentIdFilter", studentId);
        model.addAttribute("studentView", archivedView ? "archived" : "active");
        model.addAttribute("userStatuses", studentService.getAvailableStatuses());
        model.addAttribute("modalStudentId", modalStudentId);
        model.addAttribute("borrowerStandingByStudentId", borrowerStandingByStudentId);
        model.addAttribute("studentDirectoryTotalCount", activeStudents.size());
        model.addAttribute("studentDirectoryArchivedCount", archivedStudents.size());
        model.addAttribute("studentDirectoryFilteredCount", students.size());
        model.addAttribute("studentDirectoryActiveCount", activeAccountCount);
        model.addAttribute("studentDirectoryBlockedCount", blockedStudentCount);
        model.addAttribute("studentDirectoryClearedCount", borrowingClearedCount);
        return "admin/students";
    }

    @PostMapping("/students")
    public String createStudent(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam String password,
                                @RequestParam(required = false) String course,
                                @RequestParam(required = false) String yearLevel,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String province,
                                @RequestParam(required = false) String cityMunicipality,
                                @RequestParam(required = false) String barangay,
                                @RequestParam(required = false) String street,
                                @RequestParam(required = false) String zipcode,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateOfBirth,
                                @RequestParam(defaultValue = "ACTIVE") UserStatus status,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String address = authService.normalizeAndBuildOptionalAddress(province, cityMunicipality, barangay, street, zipcode);
            Student student = authService.createStudentByAdmin(name, email, password, course, yearLevel, phone, address, dateOfBirth, status);
            auditLogService.log(
                    authentication.getName(),
                    "STUDENT_CREATED",
                    "STUDENT",
                    student.getStudentId(),
                    "Student account created",
                    "Student: " + student.getUser().getName() + " | Email: " + student.getUser().getEmail()
            );
            redirectAttributes.addFlashAttribute("success", "Student account created successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/students";
    }

    @GetMapping("/students/{studentId}")
    public String studentDetails(@PathVariable String studentId,
                                 @RequestParam(defaultValue = "active") String view,
                                 Model model) {
        populateStudentDetailModel(studentId, model);
        model.addAttribute("studentView", normalizeStudentView(view));
        return "admin/student-detail";
    }

    @GetMapping("/students/{studentId}/modal")
    public String studentDetailsModal(@PathVariable String studentId,
                                      @RequestParam(defaultValue = "active") String view,
                                      Model model) {
        populateStudentDetailModel(studentId, model);
        model.addAttribute("studentView", normalizeStudentView(view));
        return "admin/student-detail-modal";
    }

    private void populateStudentDetailModel(String studentId, Model model) {
        Student student = studentService.getStudentByStudentId(studentId);
        List<IssueRecord> issueRecords = issueService.getStudentIssuesByStudentId(studentId);
        List<IssueRecord> activeIssues = issueRecords.stream()
                .filter(record -> !record.isReturned())
                .toList();
        long overdueItems = issueRecords.stream()
                .filter(record -> IssueStatus.OVERDUE.equals(record.getStatus()))
                .count();
        BigDecimal totalFineAmount = issueRecords.stream()
                .map(IssueRecord::getFineAmount)
                .filter(fine -> fine != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BorrowerStanding borrowerStanding = studentService.getBorrowerStanding(student);
        List<Fine> studentFines = fineService.getStudentFines(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("issueRecords", issueRecords);
        model.addAttribute("activeIssues", activeIssues);
        model.addAttribute("activeCount", activeIssues.size());
        model.addAttribute("historyCount", issueRecords.size());
        model.addAttribute("overdueItems", overdueItems);
        model.addAttribute("totalFineAmount", totalFineAmount);
        model.addAttribute("userStatuses", studentService.getAvailableStatuses());
        model.addAttribute("borrowerStanding", borrowerStanding);
        model.addAttribute("studentFines", studentFines);
        populateAddressModelAttributes("studentAddress", student.getAddress(), model);
    }

    private Map<String, Object> serializeAdminNotification(com.latteandletters.model.AdminNotification notification) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", notification.getId());
        item.put("title", notification.getTitle());
        item.put("message", notification.getMessage());
        item.put("createdAtDisplay", notification.getCreatedAtDisplay());
        item.put("read", notification.isRead());
        item.put("linkUrl", notification.getLinkUrl());
        item.put("notificationTypeLabel", notification.getNotificationTypeLabel());
        return item;
    }

    @PostMapping("/students/{studentId}/update")
    public String updateStudent(@PathVariable String studentId,
                                @RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String course,
                                @RequestParam(required = false) String yearLevel,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String province,
                                @RequestParam(required = false) String cityMunicipality,
                                @RequestParam(required = false) String barangay,
                                @RequestParam(required = false) String street,
                                @RequestParam(required = false) String zipcode,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateOfBirth,
                                @RequestParam(defaultValue = "ACTIVE") UserStatus status,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String address = authService.normalizeAndBuildOptionalAddress(province, cityMunicipality, barangay, street, zipcode);
            Student student = studentService.updateStudentByAdmin(studentId, name, email, course, yearLevel, phone, address, dateOfBirth, status);
            auditLogService.log(
                    authentication.getName(),
                    "STUDENT_UPDATED",
                    "STUDENT",
                    studentId,
                    "Student account updated",
                    "Status: " + student.getUser().getStatus() + " | Program: " + student.getCourse()
            );
            redirectAttributes.addFlashAttribute("success", "Student details updated successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/students?modalStudentId=" + studentId;
    }

    private void populateAddressModelAttributes(String prefix, String address, Model model) {
        com.latteandletters.util.AddressFormValue addressFormValue = authService.parseAddress(address);
        model.addAttribute(prefix + "ProvinceValue", addressFormValue.getProvince());
        model.addAttribute(prefix + "CityMunicipalityValue", addressFormValue.getCityMunicipality());
        model.addAttribute(prefix + "BarangayValue", addressFormValue.getBarangay());
        model.addAttribute(prefix + "StreetValue", addressFormValue.getStreet());
        model.addAttribute(prefix + "ZipcodeValue", addressFormValue.getZipcode());
    }

    private String normalizeStudentView(String view) {
        return "archived".equalsIgnoreCase(view) ? "archived" : "active";
    }

    @PostMapping("/students/{studentId}/password")
    public String resetStudentPassword(@PathVariable String studentId,
                                       @RequestParam String newPassword,
                                       @RequestParam String confirmPassword,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            studentService.resetStudentPassword(studentId, newPassword, confirmPassword);
            auditLogService.log(
                    authentication.getName(),
                    "STUDENT_PASSWORD_RESET",
                    "STUDENT",
                    studentId,
                    "Student password reset",
                    "Temporary password reset was completed by admin."
            );
            redirectAttributes.addFlashAttribute("success", "Student password reset successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/students?modalStudentId=" + studentId;
    }

    @PostMapping("/students/{studentId}/archive")
    public String archiveStudent(@PathVariable String studentId,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            studentService.archiveStudent(studentId);
            auditLogService.log(
                    authentication.getName(),
                    "STUDENT_ARCHIVED",
                    "STUDENT",
                    studentId,
                    "Student account archived",
                    "Student account " + studentId + " was archived by admin."
            );
            redirectAttributes.addFlashAttribute("success", "Student account archived successfully.");
            return "redirect:/admin/students";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admin/students?modalStudentId=" + studentId;
        }
    }

    @PostMapping("/students/{studentId}/restore")
    public String restoreStudent(@PathVariable String studentId,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            studentService.restoreArchivedStudent(studentId);
            auditLogService.log(
                    authentication.getName(),
                    "STUDENT_RESTORED",
                    "STUDENT",
                    studentId,
                    "Archived student account restored",
                    "Student account " + studentId + " was restored by admin."
            );
            redirectAttributes.addFlashAttribute("success", "Archived student account restored successfully.");
            return "redirect:/admin/students";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admin/students?view=archived&modalStudentId=" + studentId;
        }
    }

    @PostMapping("/students/{studentId}/delete")
    public String deleteStudent(@PathVariable String studentId,
                                @RequestParam(defaultValue = "active") String view,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            studentService.permanentlyDeleteArchivedStudent(studentId);
            auditLogService.log(
                    authentication.getName(),
                    "STUDENT_DELETED",
                    "STUDENT",
                    studentId,
                    "Student account permanently deleted",
                    "Archived student account " + studentId + " was permanently removed by admin."
            );
            redirectAttributes.addFlashAttribute("success", "Student account deleted successfully.");
            return "redirect:/admin/students?view=" + normalizeStudentView(view);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admin/students?view=" + normalizeStudentView(view) + "&modalStudentId=" + studentId;
        }
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User admin = adminService.getAdminByEmail(authentication.getName());
        model.addAttribute("adminUser", admin);
        model.addAttribute("transactionsManaged", issueService.countTransactionsManagedBy(authentication.getName()));
        model.addAttribute("activeCirculation", issueRecordRepository.countByStatus(IssueStatus.ISSUED) + issueRecordRepository.countByStatus(IssueStatus.OVERDUE));
        model.addAttribute("studentCount", studentService.countActiveStudents());
        return "admin/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication authentication,
                                @RequestParam String name,
                                RedirectAttributes redirectAttributes) {
        try {
            adminService.updateProfile(authentication.getName(), name);
            redirectAttributes.addFlashAttribute("success", "Admin profile updated successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(Authentication authentication,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        try {
            adminService.changePassword(authentication.getName(), currentPassword, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/profile";
    }
}
