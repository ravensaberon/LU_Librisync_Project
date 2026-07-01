package com.latteandletters.controller;

import com.latteandletters.dto.PasswordResetOtpDispatchResult;
import com.latteandletters.dto.PasswordResetOtpState;
import com.latteandletters.dto.StudentProfileOtpDispatchResult;
import com.latteandletters.dto.StudentProfileOtpState;
import com.latteandletters.dto.StudentProfileUpdateRequest;
import com.latteandletters.model.IssueRecord;
import com.latteandletters.model.IssueStatus;
import com.latteandletters.model.Student;
import com.latteandletters.service.AdminNotificationService;
import com.latteandletters.service.AuthService;
import com.latteandletters.service.FineService;
import com.latteandletters.service.IssueService;
import com.latteandletters.service.PasswordResetService;
import com.latteandletters.service.ReservationService;
import com.latteandletters.service.StudentProfileImageService;
import com.latteandletters.service.StudentProfileOtpService;
import com.latteandletters.service.StudentService;
import com.latteandletters.util.PaginationUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class StudentController {

    private static final int STUDENT_ACTIVE_HISTORY_PAGE_SIZE = 8;
    private static final int STUDENT_HISTORY_PAGE_SIZE = 10;
    private static final int STUDENT_RETURN_REQUESTS_PAGE_SIZE = 8;
    private static final int STUDENT_RETURNED_HISTORY_PAGE_SIZE = 10;
    private static final String PASSWORD_RESET_VERIFIED_TOKEN_SESSION_KEY = "studentPasswordResetVerifiedTokenId";
    private final StudentService studentService;
    private final IssueService issueService;
    private final ReservationService reservationService;
    private final AdminNotificationService adminNotificationService;
    private final StudentProfileOtpService studentProfileOtpService;
    private final PasswordResetService passwordResetService;
    private final FineService fineService;
    private final StudentProfileImageService studentProfileImageService;
    private final AuthService authService;

    public StudentController(StudentService studentService,
                             IssueService issueService,
                             ReservationService reservationService,
                             AdminNotificationService adminNotificationService,
                             StudentProfileOtpService studentProfileOtpService,
                             PasswordResetService passwordResetService,
                             FineService fineService,
                             StudentProfileImageService studentProfileImageService,
                             AuthService authService) {
        this.studentService = studentService;
        this.issueService = issueService;
        this.reservationService = reservationService;
        this.adminNotificationService = adminNotificationService;
        this.studentProfileOtpService = studentProfileOtpService;
        this.passwordResetService = passwordResetService;
        this.fineService = fineService;
        this.studentProfileImageService = studentProfileImageService;
        this.authService = authService;
    }

    @GetMapping("/student/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        List<IssueRecord> issueRecords = issueService.getStudentIssues(authentication.getName());

        long activeCount = issueRecords.stream()
                .filter(record -> !record.isReturned())
                .count();
        long overdueCount = issueRecords.stream()
                .filter(record -> IssueStatus.OVERDUE.equals(record.getStatus()))
                .count();

        model.addAttribute("student", student);
        model.addAttribute("issueRecords", issueRecords);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("historyCount", issueRecords.size());
        model.addAttribute("reservationCount", reservationService.getStudentReservations(authentication.getName()).stream().filter(reservation -> reservation.isActive()).count());
        model.addAttribute("borrowerStanding", studentService.getBorrowerStanding(student));
        model.addAttribute("outstandingFineTotal", fineService.getOutstandingFineTotalByStudent(student.getId()));
        model.addAttribute("studentFines", fineService.getStudentFines(student.getId()));
        model.addAttribute("popularBooks", issueService.getMostBorrowedBooks());
        return "student/dashboard";
    }

    @PostMapping("/student/notifications/read-all")
    @ResponseBody
    public Map<String, Object> markAllStudentNotificationsRead(Authentication authentication) {
        adminNotificationService.markAllAsRead(authentication.getName());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("unreadCount", 0);
        return response;
    }

    @GetMapping("/student/notifications/panel")
    @ResponseBody
    public Map<String, Object> studentNotificationPanel(Authentication authentication) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("unreadCount", adminNotificationService.countUnreadNotifications(authentication.getName()));
        response.put("items", adminNotificationService.getRecentNotifications(authentication.getName(), 5).stream()
                .map(this::serializeNotification)
                .toList());
        return response;
    }

    @GetMapping("/student/notifications/history")
    @ResponseBody
    public Map<String, Object> studentNotificationHistory(Authentication authentication,
                                                          @RequestParam(defaultValue = "1") Integer page) {
        var notificationPage = adminNotificationService.getNotificationPage(authentication.getName(), page, 8);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", notificationPage.getItems().stream()
                .map(this::serializeNotification)
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

    @GetMapping("/student/profile")
    public String profile(Authentication authentication, Model model) {
        if (isAdmin(authentication)) {
            return "redirect:/admin/profile";
        }
        Student student = studentService.getStudentByEmail(authentication.getName());
        populateProfilePageModel(model, student);
        return "student/profile";
    }

    @GetMapping("/student/password/change-temporary")
    public String temporaryPasswordChangePage(Authentication authentication,
                                              Model model) {
        if (isAdmin(authentication)) {
            return "redirect:/admin/dashboard";
        }
        if (!studentService.mustChangePassword(authentication.getName())) {
            return "redirect:/student/dashboard";
        }
        Student student = studentService.getStudentByEmail(authentication.getName());
        model.addAttribute("student", student);
        model.addAttribute("mustChangePassword", studentService.mustChangePassword(authentication.getName()));
        return "student/force-password-change";
    }

    @PostMapping("/student/password/change-temporary")
    public String changeTemporaryPassword(Authentication authentication,
                                          @RequestParam(required = false) String newPassword,
                                          @RequestParam(required = false) String confirmPassword,
                                          RedirectAttributes redirectAttributes) {
        try {
            studentService.completeRequiredPasswordChange(authentication.getName(), newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("success", "Password updated successfully.");
            return "redirect:/student/dashboard";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/student/password/change-temporary";
        }
    }

    @GetMapping("/student/profile/avatar")
    public ResponseEntity<Resource> profileAvatar(Authentication authentication) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        Resource resource = studentProfileImageService.getProfileImageResource(student);

        return ResponseEntity.ok()
                .contentType(studentProfileImageService.getProfileImageMediaType(student))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @PostMapping("/student/profile/avatar")
    public String uploadProfileAvatar(Authentication authentication,
                                      @RequestParam("profileImage") MultipartFile profileImage,
                                      RedirectAttributes redirectAttributes) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        try {
            studentProfileImageService.storeProfileImage(student, profileImage);
            redirectAttributes.addFlashAttribute("success", "Profile picture updated successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/profile";
    }

    @PostMapping("/student/profile/avatar/remove")
    public String removeProfileAvatar(Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        studentProfileImageService.deleteProfileImage(student);
        redirectAttributes.addFlashAttribute("success", "Profile picture removed.");
        return "redirect:/student/profile";
    }

    @PostMapping("/student/profile/request-otp")
    public String requestProfileUpdateOtp(Authentication authentication,
                                          @RequestParam String firstName,
                                          @RequestParam(required = false) String middleName,
                                          @RequestParam String lastName,
                                          @RequestParam(required = false) String suffix,
                                          @RequestParam(required = false) String course,
                                          @RequestParam(required = false) String yearLevel,
                                          @RequestParam(required = false) String phone,
                                          @RequestParam(required = false) String province,
                                          @RequestParam(required = false) String cityMunicipality,
                                          @RequestParam(required = false) String barangay,
                                          @RequestParam(required = false) String street,
                                          @RequestParam(required = false) String zipcode,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
                                          RedirectAttributes redirectAttributes) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        StudentProfileUpdateRequest submittedRequest = new StudentProfileUpdateRequest(
                firstName,
                middleName,
                lastName,
                suffix,
                course,
                yearLevel,
                phone,
                null,
                dateOfBirth
        );

        try {
            submittedRequest.setAddress(authService.normalizeAndBuildOptionalAddress(province, cityMunicipality, barangay, street, zipcode));
            StudentProfileOtpDispatchResult dispatchResult = studentProfileOtpService.requestOtp(student, submittedRequest);
            applyOtpStateFlashAttributes(dispatchResult.getOtpState(), redirectAttributes);
            redirectAttributes.addFlashAttribute("openOtpModal", true);
            if (dispatchResult.isCooldownActive()) {
                redirectAttributes.addFlashAttribute("info", "A profile OTP is already active.");
            } else if (dispatchResult.isDelivered()) {
                redirectAttributes.addFlashAttribute("success", "An OTP has been sent to your registered email.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Unable to send OTP email right now.");
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("profileForm", submittedRequest);
            applyAddressFlashAttributes("profile", province, cityMunicipality, barangay, street, zipcode, redirectAttributes);
            redirectAttributes.addFlashAttribute("openEditModal", true);
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("profileForm", submittedRequest);
            applyAddressFlashAttributes("profile", province, cityMunicipality, barangay, street, zipcode, redirectAttributes);
            redirectAttributes.addFlashAttribute("openEditModal", true);
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/profile";
    }

    @PostMapping("/student/profile/resend-otp")
    public String resendProfileUpdateOtp(Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        try {
            StudentProfileOtpDispatchResult dispatchResult = studentProfileOtpService.resendOtp(student);
            applyOtpStateFlashAttributes(dispatchResult.getOtpState(), redirectAttributes);
            redirectAttributes.addFlashAttribute("openOtpModal", true);
            if (dispatchResult.isCooldownActive()) {
                redirectAttributes.addFlashAttribute("info", "Please wait before requesting another OTP.");
            } else if (dispatchResult.isDelivered()) {
                redirectAttributes.addFlashAttribute("success", "A new OTP has been sent to your registered email.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Unable to send OTP email right now.");
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("openEditModal", true);
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("openEditModal", true);
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/profile";
    }

    @PostMapping("/student/profile/verify-otp")
    public String verifyProfileUpdateOtp(Authentication authentication,
                                         @RequestParam(required = false) String otpCode,
                                         RedirectAttributes redirectAttributes) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        StudentProfileOtpState latestOtpState = studentProfileOtpService.getLatestOtpState(student);
        if (latestOtpState != null) {
            applyOtpStateFlashAttributes(latestOtpState, redirectAttributes);
            redirectAttributes.addFlashAttribute("profileForm", latestOtpState.getUpdateRequest());
        }

        try {
            StudentProfileUpdateRequest verifiedRequest = studentProfileOtpService.verifyOtp(student, otpCode);
            studentService.updateProfile(authentication.getName(), verifiedRequest);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        } catch (IllegalArgumentException exception) {
            boolean hasActiveOtp = latestOtpState != null
                    && latestOtpState.getExpiresAt() != null
                    && latestOtpState.getExpiresAt().isAfter(LocalDateTime.now());
            redirectAttributes.addFlashAttribute("openOtpModal", hasActiveOtp);
            redirectAttributes.addFlashAttribute("openEditModal", !hasActiveOtp);
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/profile";
    }

    @PostMapping("/student/profile/password")
    public String changePassword(Authentication authentication,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        try {
            studentService.changePassword(authentication.getName(), currentPassword, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/profile";
    }

    @GetMapping("/student/password/state")
    @ResponseBody
    public Map<String, Object> studentPasswordState(Authentication authentication,
                                                    HttpSession session) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        PasswordResetOtpState otpState = passwordResetService.getActiveOtpState(student.getUser().getEmail());
        return buildPasswordStateResponse(otpState, isPasswordOtpVerified(session));
    }

    @PostMapping("/student/password/request-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> requestStudentPasswordOtp(Authentication authentication,
                                                                         HttpSession session) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        clearPasswordOtpVerification(session);
        try {
            PasswordResetOtpDispatchResult dispatchResult = passwordResetService.requestOtp(student.getUser().getEmail());
            Map<String, Object> response = buildPasswordStateResponse(dispatchResult.getOtpState(), false);
            response.put("success", true);
            if (dispatchResult.isCooldownActive()) {
                response.put("message", "Please wait before requesting another OTP.");
            } else if (dispatchResult.isDelivered()) {
                response.put("message", "An OTP has been sent to your registered email.");
            } else {
                response.put("message", "OTP generated but email delivery failed. Check the outbox folder or contact your administrator.");
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return buildPasswordErrorResponse(exception.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException exception) {
            return buildPasswordErrorResponse(exception.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @PostMapping("/student/password/resend-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resendStudentPasswordOtp(Authentication authentication,
                                                                        HttpSession session) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        clearPasswordOtpVerification(session);
        try {
            PasswordResetOtpDispatchResult dispatchResult = passwordResetService.resendOtp(student.getUser().getEmail());
            Map<String, Object> response = buildPasswordStateResponse(dispatchResult.getOtpState(), false);
            response.put("success", true);
            if (dispatchResult.isCooldownActive()) {
                response.put("message", "Please wait before requesting another OTP.");
            } else if (dispatchResult.isDelivered()) {
                response.put("message", "A new OTP has been sent to your registered email.");
            } else {
                response.put("message", "OTP generated but email delivery failed. Check the outbox folder or contact your administrator.");
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return buildPasswordErrorResponse(exception.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException exception) {
            return buildPasswordErrorResponse(exception.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @PostMapping("/student/password/verify-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyStudentPasswordOtp(Authentication authentication,
                                                                        @RequestParam(required = false) String otpCode,
                                                                        HttpSession session) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        try {
            Long verifiedTokenId = passwordResetService.verifyOtp(student.getUser().getEmail(), otpCode);
            session.setAttribute(PASSWORD_RESET_VERIFIED_TOKEN_SESSION_KEY, verifiedTokenId);
            PasswordResetOtpState otpState = passwordResetService.getActiveOtpState(student.getUser().getEmail());
            Map<String, Object> response = buildPasswordStateResponse(otpState, true);
            response.put("success", true);
            response.put("message", "OTP verified. You can now set a new password.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            clearPasswordOtpVerification(session);
            return buildPasswordErrorResponse(exception.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/student/password/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStudentPassword(Authentication authentication,
                                                                     @RequestParam(required = false) String newPassword,
                                                                     @RequestParam(required = false) String confirmPassword,
                                                                     HttpSession session) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        try {
            passwordResetService.updatePasswordWithVerifiedOtp(
                    student.getUser().getEmail(),
                    getVerifiedPasswordTokenId(session),
                    newPassword,
                    confirmPassword
            );
            clearPasswordOtpVerification(session);
            Map<String, Object> response = buildPasswordStateResponse(null, false);
            response.put("success", true);
            response.put("message", "Password changed successfully.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return buildPasswordErrorResponse(exception.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/student/history")
    public String history(Authentication authentication,
                          @RequestParam(defaultValue = "all") String tab,
                          @RequestParam(defaultValue = "1") Integer activePage,
                          @RequestParam(defaultValue = "1") Integer historyPage,
                          @RequestParam(defaultValue = "1") Integer requestPage,
                          @RequestParam(defaultValue = "1") Integer returnedPage,
                          Model model) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        List<IssueRecord> issueRecords = issueService.getStudentIssues(authentication.getName());
        List<IssueRecord> activeIssues = issueRecords.stream()
                .filter(record -> !record.isReturned())
                .toList();
        List<IssueRecord> returnRequestIssues = issueRecords.stream()
                .filter(IssueRecord::isReturnRequested)
                .toList();
        List<IssueRecord> returnedIssues = issueRecords.stream()
                .filter(IssueRecord::isReturned)
                .toList();
        var activeIssuesPage = PaginationUtils.paginate(activeIssues, activePage, STUDENT_ACTIVE_HISTORY_PAGE_SIZE);
        var historyIssuesPage = PaginationUtils.paginate(issueRecords, historyPage, STUDENT_HISTORY_PAGE_SIZE);
        var returnRequestIssuesPage = PaginationUtils.paginate(returnRequestIssues, requestPage, STUDENT_RETURN_REQUESTS_PAGE_SIZE);
        var returnedIssuesPage = PaginationUtils.paginate(returnedIssues, returnedPage, STUDENT_RETURNED_HISTORY_PAGE_SIZE);
        model.addAttribute("student", student);
        model.addAttribute("activeIssues", activeIssuesPage.getItems());
        model.addAttribute("activeIssuesPage", activeIssuesPage);
        model.addAttribute("historyIssues", historyIssuesPage.getItems());
        model.addAttribute("historyIssuesPage", historyIssuesPage);
        model.addAttribute("returnRequestIssues", returnRequestIssuesPage.getItems());
        model.addAttribute("returnRequestIssuesPage", returnRequestIssuesPage);
        model.addAttribute("returnedIssues", returnedIssuesPage.getItems());
        model.addAttribute("returnedIssuesPage", returnedIssuesPage);
        model.addAttribute("borrowerStanding", studentService.getBorrowerStanding(student));
        model.addAttribute("outstandingFineTotal", fineService.getOutstandingFineTotalByStudent(student.getId()));
        model.addAttribute("activeCount", activeIssues.size());
        model.addAttribute("overdueCount", issueRecords.stream().filter(record -> IssueStatus.OVERDUE.equals(record.getStatus())).count());
        model.addAttribute("returnRequestCount", returnRequestIssues.size());
        model.addAttribute("returnedCount", returnedIssues.size());
        model.addAttribute("reservationCount", reservationService.getStudentReservations(authentication.getName()).stream().filter(reservation -> reservation.isActive()).count());
        String normalizedTab = switch (tab == null ? "" : tab.toLowerCase()) {
            case "current", "requests", "returned", "all" -> tab.toLowerCase();
            default -> "all";
        };
        model.addAttribute("activeTab", normalizedTab);
        return "student/history";
    }

    private void populateProfilePageModel(Model model, Student student) {
        StudentProfileOtpState activeOtpState = studentProfileOtpService.getActiveOtpState(student);
        StudentProfileOtpState latestOtpState = studentProfileOtpService.getLatestOtpState(student);

        model.addAttribute("student", student);
        model.addAttribute("studentInitials", buildInitials(student.getUser().getName()));
        model.addAttribute("hasProfileImage", studentProfileImageService.hasProfileImage(student));
        model.addAttribute("profileImageVersion", studentProfileImageService.getProfileImageVersion(student));
        model.addAttribute("hasPendingProfileOtp", activeOtpState != null);
        model.addAttribute("borrowerStanding", studentService.getBorrowerStanding(student));
        model.addAttribute("studentFines", fineService.getStudentFines(student.getId()));
        model.addAttribute("outstandingFineTotal", fineService.getOutstandingFineTotalByStudent(student.getId()));

        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", latestOtpState != null
                    ? latestOtpState.getUpdateRequest()
                    : studentService.createProfileUpdateRequest(student));
        }

        StudentProfileUpdateRequest profileForm = (StudentProfileUpdateRequest) model.getAttribute("profileForm");
        populateAddressModelAttributes("profile", profileForm == null ? null : profileForm.getAddress(), model);

        if (!model.containsAttribute("otpMaskedEmail") && activeOtpState != null) {
            model.addAttribute("otpMaskedEmail", maskEmail(activeOtpState.getDestinationEmail()));
        }
        if (!model.containsAttribute("otpExpiresAtEpochMs")) {
            model.addAttribute("otpExpiresAtEpochMs", toEpochMillis(activeOtpState == null ? null : activeOtpState.getExpiresAt()));
        }
        if (!model.containsAttribute("otpResendAvailableAtEpochMs")) {
            model.addAttribute("otpResendAvailableAtEpochMs", toEpochMillis(activeOtpState == null ? null : activeOtpState.getResendAvailableAt()));
        }
        if (!model.containsAttribute("openEditModal")) {
            model.addAttribute("openEditModal", false);
        }
        if (!model.containsAttribute("openOtpModal")) {
            model.addAttribute("openOtpModal", false);
        }
    }

    private void applyOtpStateFlashAttributes(StudentProfileOtpState otpState,
                                              RedirectAttributes redirectAttributes) {
        if (otpState == null) {
            return;
        }
        redirectAttributes.addFlashAttribute("profileForm", otpState.getUpdateRequest());
        applyAddressFlashAttributesFromAddress("profile", otpState.getUpdateRequest().getAddress(), redirectAttributes);
        redirectAttributes.addFlashAttribute("otpMaskedEmail", maskEmail(otpState.getDestinationEmail()));
        redirectAttributes.addFlashAttribute("otpExpiresAtEpochMs", toEpochMillis(otpState.getExpiresAt()));
        redirectAttributes.addFlashAttribute("otpResendAvailableAtEpochMs", toEpochMillis(otpState.getResendAvailableAt()));
    }

    private void populateAddressModelAttributes(String prefix, String address, Model model) {
        com.latteandletters.util.AddressFormValue addressFormValue = authService.parseAddress(address);
        if (!model.containsAttribute(prefix + "ProvinceValue")) {
            model.addAttribute(prefix + "ProvinceValue", addressFormValue.getProvince());
        }
        if (!model.containsAttribute(prefix + "CityMunicipalityValue")) {
            model.addAttribute(prefix + "CityMunicipalityValue", addressFormValue.getCityMunicipality());
        }
        if (!model.containsAttribute(prefix + "BarangayValue")) {
            model.addAttribute(prefix + "BarangayValue", addressFormValue.getBarangay());
        }
        if (!model.containsAttribute(prefix + "StreetValue")) {
            model.addAttribute(prefix + "StreetValue", addressFormValue.getStreet());
        }
        if (!model.containsAttribute(prefix + "ZipcodeValue")) {
            model.addAttribute(prefix + "ZipcodeValue", addressFormValue.getZipcode());
        }
    }

    private void applyAddressFlashAttributes(String prefix,
                                             String province,
                                             String cityMunicipality,
                                             String barangay,
                                             String street,
                                             String zipcode,
                                             RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(prefix + "ProvinceValue", province);
        redirectAttributes.addFlashAttribute(prefix + "CityMunicipalityValue", cityMunicipality);
        redirectAttributes.addFlashAttribute(prefix + "BarangayValue", barangay);
        redirectAttributes.addFlashAttribute(prefix + "StreetValue", street);
        redirectAttributes.addFlashAttribute(prefix + "ZipcodeValue", zipcode);
    }

    private void applyAddressFlashAttributesFromAddress(String prefix,
                                                        String address,
                                                        RedirectAttributes redirectAttributes) {
        com.latteandletters.util.AddressFormValue addressFormValue = authService.parseAddress(address);
        applyAddressFlashAttributes(
                prefix,
                addressFormValue.getProvince(),
                addressFormValue.getCityMunicipality(),
                addressFormValue.getBarangay(),
                addressFormValue.getStreet(),
                addressFormValue.getZipcode(),
                redirectAttributes
        );
    }

    private String buildInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "ST";
        }

        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.isEmpty() ? "ST" : initials.toString();
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "your registered email";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domain;
    }

    private Long toEpochMillis(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private Map<String, Object> buildPasswordStateResponse(PasswordResetOtpState otpState,
                                                           boolean verified) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("hasPendingOtp", otpState != null);
        response.put("verified", verified);
        response.put("maskedEmail", otpState == null ? null : otpState.getMaskedEmail());
        response.put("expiresAtEpochMs", toEpochMillis(otpState == null ? null : otpState.getExpiresAt()));
        response.put("resendAvailableAtEpochMs", toEpochMillis(otpState == null ? null : otpState.getResendAvailableAt()));
        return response;
    }

    private ResponseEntity<Map<String, Object>> buildPasswordErrorResponse(String message,
                                                                           HttpStatus status) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

    private void clearPasswordOtpVerification(HttpSession session) {
        session.removeAttribute(PASSWORD_RESET_VERIFIED_TOKEN_SESSION_KEY);
    }

    private boolean isPasswordOtpVerified(HttpSession session) {
        return getVerifiedPasswordTokenId(session) != null;
    }

    private Map<String, Object> serializeNotification(com.latteandletters.model.AdminNotification notification) {
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

    private Long getVerifiedPasswordTokenId(HttpSession session) {
        Object value = session.getAttribute(PASSWORD_RESET_VERIFIED_TOKEN_SESSION_KEY);
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
