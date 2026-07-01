package com.latteandletters.controller;

import com.latteandletters.dto.PasswordResetOtpDispatchResult;
import com.latteandletters.dto.PasswordResetOtpState;
import com.latteandletters.dto.RegistrationAvailabilityResult;
import com.latteandletters.dto.RegistrationOtpState;
import com.latteandletters.dto.StudentRegistrationResult;
import com.latteandletters.model.Student;
import com.latteandletters.service.AuthService;
import com.latteandletters.service.PasswordResetService;
import com.latteandletters.service.RegistrationEmailOtpService;
import com.latteandletters.service.RegistrationOtpService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final RegistrationOtpService registrationOtpService;
    private final RegistrationEmailOtpService registrationEmailOtpService;

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService,
                          RegistrationOtpService registrationOtpService,
                          RegistrationEmailOtpService registrationEmailOtpService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.registrationOtpService = registrationOtpService;
        this.registrationEmailOtpService = registrationEmailOtpService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Authentication authentication, HttpSession session, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        populateRegisterOptions(model);
        populateRegistrationEmailVerification(model, session, resolveRegisterEmail(model));
        return "auth/register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(@RequestParam(required = false) String email,
                                     Authentication authentication,
                                     Model model) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        populateForgotPasswordModel(model, email);
        return "auth/forgot-password";
    }

    @GetMapping("/register/barangays")
    @ResponseBody
    public ResponseEntity<List<String>> loadBarangays(@RequestParam(required = false) String cityMunicipality) {
        try {
            return ResponseEntity.ok(authService.getBarangaysForCityMunicipality(cityMunicipality));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(503).build();
        }
    }

    @GetMapping("/register/availability")
    @ResponseBody
    public ResponseEntity<RegistrationAvailabilityResult> checkRegistrationAvailability(@RequestParam(required = false) String field,
                                                                                        @RequestParam(required = false) String value) {
        RegistrationAvailabilityResult result = authService.checkRegistrationAvailability(field, value);
        if (!result.valid() && "Unsupported registration field.".equals(result.message())) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/register/otp-state")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrationOtpState(@RequestParam(required = false) String email,
                                                                    HttpSession session) {
        return ResponseEntity.ok(buildRegistrationOtpResponse(email, session));
    }

    @PostMapping("/register/request-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> requestRegistrationOtp(@RequestParam(required = false) String email,
                                                                      HttpSession session) {
        try {
            RegistrationOtpState state = registrationEmailOtpService.requestOtp(email, session);
            Map<String, Object> response = buildRegistrationOtpResponse(email, session);
            response.put("success", true);
            response.put("message", "A verification code was generated. Check your email, or open the local email outbox if mail delivery is unavailable.");
            response.put("maskedEmail", state.getMaskedEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(buildRegistrationOtpErrorResponse(email, session, exception.getMessage()));
        }
    }

    @PostMapping("/register/verify-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyRegistrationOtp(@RequestParam(required = false) String email,
                                                                     @RequestParam(required = false) String otpCode,
                                                                     HttpSession session) {
        try {
            registrationEmailOtpService.verifyOtp(email, otpCode, session);
            Map<String, Object> response = buildRegistrationOtpResponse(email, session);
            response.put("success", true);
            response.put("message", "Email verified. You can now create your account.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(buildRegistrationOtpErrorResponse(email, session, exception.getMessage()));
        }
    }

    @PostMapping("/register")
    public String register(@RequestParam(required = false) String firstName,
                           @RequestParam(required = false) String middleName,
                           @RequestParam(required = false) String lastName,
                           @RequestParam(required = false) String suffix,
                           @RequestParam(required = false) String program,
                           @RequestParam(required = false) String yearLevel,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String contactNumber,
                           @RequestParam(required = false) String birthDate,
                           @RequestParam(required = false) String province,
                           @RequestParam(required = false) String cityMunicipality,
                           @RequestParam(required = false) String barangay,
                           @RequestParam(required = false) String street,
                           @RequestParam(required = false) String zipcode,
                           @RequestParam(required = false) String agree,
                           HttpServletRequest request,
                           HttpSession session,
                           Model model) {
        try {
            if (!registrationEmailOtpService.isEmailVerified(email, session)) {
                throw new IllegalArgumentException("Verify your email with OTP before creating your account.");
            }
            StudentRegistrationResult registrationResult = authService.registerStudent(
                    firstName,
                    middleName,
                    lastName,
                    suffix,
                    program,
                    yearLevel,
                    email,
                    contactNumber,
                    birthDate,
                    province,
                    cityMunicipality,
                    barangay,
                    street,
                    zipcode,
                    agree != null,
                    true
            );
            Student student = registrationResult.student();
            registrationEmailOtpService.clear(session);
            request.login(student.getUser().getEmail(), registrationResult.temporaryPassword());
            return "redirect:/student/password/change-temporary";
        } catch (ServletException exception) {
            populateRegisterModel(
                    model,
                    firstName,
                    middleName,
                    lastName,
                    suffix,
                    program,
                    yearLevel,
                    email,
                    contactNumber,
                    birthDate,
                    province,
                    cityMunicipality,
                    barangay,
                    street,
                    zipcode,
                    agree != null
            );
            model.addAttribute("error", "Account created, but automatic sign-in failed. Check your email or the local email outbox for the temporary password, then sign in manually.");
            return "auth/register";
        } catch (IllegalStateException exception) {
            populateRegisterModel(
                    model,
                    firstName,
                    middleName,
                    lastName,
                    suffix,
                    program,
                    yearLevel,
                    email,
                    contactNumber,
                    birthDate,
                    province,
                    cityMunicipality,
                    barangay,
                    street,
                    zipcode,
                    agree != null
            );
            model.addAttribute("error", exception.getMessage());
            populateRegistrationEmailVerification(model, session, email);
            return "auth/register";
        } catch (IllegalArgumentException exception) {
            populateRegisterModel(
                    model,
                    firstName,
                    middleName,
                    lastName,
                    suffix,
                    program,
                    yearLevel,
                    email,
                    contactNumber,
                    birthDate,
                    province,
                    cityMunicipality,
                    barangay,
                    street,
                    zipcode,
                    agree != null
            );
            if (!applyRegisterFieldError(model, exception.getMessage())) {
                model.addAttribute("error", exception.getMessage());
            }
            populateRegistrationEmailVerification(model, session, email);
            return "auth/register";
        }
    }

    @GetMapping("/register/verify")
    public String verifyEmailPage(@RequestParam(required = false) String email,
                                  Authentication authentication,
                                  Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        RegistrationOtpState state = registrationOtpService.getActiveOtpState(email);
        if (state == null && (email == null || email.isBlank())) {
            return "redirect:/register";
        }
        populateVerifyModel(model, email, state);
        return "auth/verify-email";
    }

    @PostMapping("/register/verify")
    public String verifyEmail(@RequestParam(required = false) String email,
                              @RequestParam(required = false) String otpCode,
                              RedirectAttributes redirectAttributes) {
        try {
            registrationOtpService.verifyAndActivate(email, otpCode);
            return "redirect:/login?registered&email=" + encodeEmail(email);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/register/verify?email=" + encodeEmail(email);
        }
    }

    @PostMapping("/register/resend-otp")
    public String resendRegistrationOtp(@RequestParam(required = false) String email,
                                        RedirectAttributes redirectAttributes) {
        try {
            registrationOtpService.resendOtp(email);
            redirectAttributes.addFlashAttribute("success", "A new verification code has been sent to your email.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/register/verify?email=" + encodeEmail(email);
    }

    @PostMapping("/forgot-password/request-otp")
    public String requestPasswordResetOtp(@RequestParam(required = false) String email,
                                          RedirectAttributes redirectAttributes) {
        try {
            PasswordResetOtpDispatchResult dispatchResult = passwordResetService.requestOtp(email);
            applyPasswordResetStateFlashAttributes(dispatchResult.getOtpState(), redirectAttributes);
            redirectAttributes.addFlashAttribute("openResetPanel", true);
            if (dispatchResult.isCooldownActive()) {
                redirectAttributes.addFlashAttribute("info", "A password reset OTP is already active.");
            } else if (dispatchResult.isDelivered()) {
                redirectAttributes.addFlashAttribute("success", "A password reset OTP has been sent to your email.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Unable to send OTP email right now.");
            }
            return "redirect:/forgot-password?email=" + dispatchResult.getOtpState().getEmail();
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("emailValue", email);
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/forgot-password";
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("emailValue", email);
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/forgot-password";
        }
    }

    @PostMapping("/forgot-password/resend-otp")
    public String resendPasswordResetOtp(@RequestParam(required = false) String email,
                                         RedirectAttributes redirectAttributes) {
        try {
            PasswordResetOtpDispatchResult dispatchResult = passwordResetService.resendOtp(email);
            applyPasswordResetStateFlashAttributes(dispatchResult.getOtpState(), redirectAttributes);
            redirectAttributes.addFlashAttribute("openResetPanel", true);
            if (dispatchResult.isCooldownActive()) {
                redirectAttributes.addFlashAttribute("info", "Please wait before requesting another OTP.");
            } else if (dispatchResult.isDelivered()) {
                redirectAttributes.addFlashAttribute("success", "A fresh password reset OTP has been sent to your email.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Unable to send OTP email right now.");
            }
            return "redirect:/forgot-password?email=" + dispatchResult.getOtpState().getEmail();
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("emailValue", email);
            redirectAttributes.addFlashAttribute("openResetPanel", true);
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/forgot-password";
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("emailValue", email);
            redirectAttributes.addFlashAttribute("openResetPanel", true);
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/forgot-password";
        }
    }

    @PostMapping("/forgot-password/reset")
    public String resetPassword(@RequestParam(required = false) String email,
                                @RequestParam(required = false) String otpCode,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.resetPassword(email, otpCode, newPassword, confirmPassword);
            return "redirect:/login?resetSuccess";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("emailValue", email);
            redirectAttributes.addFlashAttribute("openResetPanel", true);
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            try {
                PasswordResetOtpState otpState = passwordResetService.getActiveOtpState(email);
                applyPasswordResetStateFlashAttributes(otpState, redirectAttributes);
            } catch (IllegalArgumentException ignored) {
                // Keep the entered email visible even if no active OTP exists.
            }
            return "redirect:/forgot-password";
        }
    }

    private void populateRegisterModel(Model model,
                                       String firstName,
                                       String middleName,
                                       String lastName,
                                       String suffix,
                                       String program,
                                       String yearLevel,
                                       String email,
                                       String contactNumber,
                                       String birthDate,
                                       String province,
                                       String cityMunicipality,
                                       String barangay,
                                       String street,
                                       String zipcode,
                                       boolean agreeChecked) {
        populateRegisterOptions(model);
        model.addAttribute("firstNameValue", firstName);
        model.addAttribute("middleNameValue", middleName);
        model.addAttribute("lastNameValue", lastName);
        model.addAttribute("suffixValue", suffix);
        model.addAttribute("programValue", program);
        model.addAttribute("yearLevelValue", yearLevel);
        model.addAttribute("emailValue", email);
        model.addAttribute("contactNumberValue", contactNumber);
        model.addAttribute("birthDateValue", birthDate);
        model.addAttribute("provinceValue", province);
        model.addAttribute("cityMunicipalityValue", cityMunicipality);
        model.addAttribute("barangayValue", barangay);
        model.addAttribute("streetValue", street);
        model.addAttribute("zipcodeValue", zipcode);
        model.addAttribute("agreeChecked", agreeChecked);
    }

    private void populateRegistrationEmailVerification(Model model,
                                                       HttpSession session,
                                                       String email) {
        RegistrationOtpState otpState = registrationEmailOtpService.getOtpState(email, session);
        model.addAttribute("registrationEmailVerified", registrationEmailOtpService.isEmailVerified(email, session));
        model.addAttribute("registrationOtpMaskedEmail", otpState == null ? null : otpState.getMaskedEmail());
        model.addAttribute("registrationOtpExpiresAtEpochMs", otpState == null ? null : toEpochMillis(otpState.getExpiresAt()));
        model.addAttribute("registrationOtpResendAvailableAtEpochMs", otpState == null ? null : toEpochMillis(otpState.getResendAvailableAt()));
    }

    private String resolveRegisterEmail(Model model) {
        if (!model.containsAttribute("emailValue")) {
            return null;
        }
        Object value = model.getAttribute("emailValue");
        return value == null ? null : value.toString();
    }

    private void populateVerifyModel(Model model, String email, RegistrationOtpState state) {
        model.addAttribute("emailValue", email);
        if (state != null) {
            model.addAttribute("maskedEmail", state.getMaskedEmail());
            model.addAttribute("otpExpiresAtEpochMs", toEpochMillis(state.getExpiresAt()));
            model.addAttribute("otpResendAvailableAtEpochMs", toEpochMillis(state.getResendAvailableAt()));
        }
    }

    private String encodeEmail(String email) {
        if (email == null) return "";
        try {
            return java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return email;
        }
    }

    private void populateRegisterOptions(Model model) {
        model.addAttribute("registrationCityZipCodes", authService.getLagunaCityZipCodes());
        if (!model.containsAttribute("provinceValue")) {
            model.addAttribute("provinceValue", "Laguna");
        }
    }

    private boolean applyRegisterFieldError(Model model, String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        if ("This email is already taken.".equals(message)) {
            model.addAttribute("emailFieldError", message);
            return true;
        }
        if ("This contact number is already used.".equals(message)) {
            model.addAttribute("contactNumberFieldError", message);
            return true;
        }
        return false;
    }

    private void populateForgotPasswordModel(Model model, String email) {
        String effectiveEmail = resolveEmailForForgotPassword(model, email);
        if (!model.containsAttribute("openResetPanel")) {
            model.addAttribute("openResetPanel", false);
        }

        if (!model.containsAttribute("emailValue")) {
            model.addAttribute("emailValue", effectiveEmail);
        }

        if (effectiveEmail == null || effectiveEmail.isBlank()) {
            if (!model.containsAttribute("hasPendingResetOtp")) {
                model.addAttribute("hasPendingResetOtp", false);
            }
            return;
        }

        try {
            PasswordResetOtpState activeState = passwordResetService.getActiveOtpState(effectiveEmail);
            if (activeState == null) {
                model.addAttribute("hasPendingResetOtp", false);
                return;
            }

            model.addAttribute("hasPendingResetOtp", true);
            if (!model.containsAttribute("maskedResetEmail")) {
                model.addAttribute("maskedResetEmail", activeState.getMaskedEmail());
            }
            if (!model.containsAttribute("resetOtpExpiresAtEpochMs")) {
                model.addAttribute("resetOtpExpiresAtEpochMs", toEpochMillis(activeState.getExpiresAt()));
            }
            if (!model.containsAttribute("resetOtpResendAvailableAtEpochMs")) {
                model.addAttribute("resetOtpResendAvailableAtEpochMs", toEpochMillis(activeState.getResendAvailableAt()));
            }
            if (!(Boolean.TRUE.equals(model.getAttribute("openResetPanel")))) {
                model.addAttribute("openResetPanel", true);
            }
        } catch (IllegalArgumentException ignored) {
            if (!model.containsAttribute("hasPendingResetOtp")) {
                model.addAttribute("hasPendingResetOtp", false);
            }
        }
    }

    private String resolveEmailForForgotPassword(Model model, String email) {
        if (model.containsAttribute("emailValue")) {
            Object existingValue = model.getAttribute("emailValue");
            return existingValue == null ? null : existingValue.toString();
        }
        return email;
    }

    private void applyPasswordResetStateFlashAttributes(PasswordResetOtpState otpState,
                                                        RedirectAttributes redirectAttributes) {
        if (otpState == null) {
            return;
        }
        redirectAttributes.addFlashAttribute("emailValue", otpState.getEmail());
        redirectAttributes.addFlashAttribute("maskedResetEmail", otpState.getMaskedEmail());
        redirectAttributes.addFlashAttribute("hasPendingResetOtp", true);
        redirectAttributes.addFlashAttribute("resetOtpExpiresAtEpochMs", toEpochMillis(otpState.getExpiresAt()));
        redirectAttributes.addFlashAttribute("resetOtpResendAvailableAtEpochMs", toEpochMillis(otpState.getResendAvailableAt()));
    }

    private Map<String, Object> buildRegistrationOtpResponse(String email, HttpSession session) {
        RegistrationOtpState state = registrationEmailOtpService.getOtpState(email, session);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("email", email == null ? "" : email.trim().toLowerCase());
        response.put("verified", registrationEmailOtpService.isEmailVerified(email, session));
        response.put("hasPendingOtp", state != null);
        response.put("maskedEmail", state == null ? null : state.getMaskedEmail());
        response.put("expiresAtEpochMs", state == null ? null : toEpochMillis(state.getExpiresAt()));
        response.put("resendAvailableAtEpochMs", state == null ? null : toEpochMillis(state.getResendAvailableAt()));
        return response;
    }

    private Map<String, Object> buildRegistrationOtpErrorResponse(String email,
                                                                  HttpSession session,
                                                                  String message) {
        Map<String, Object> response = buildRegistrationOtpResponse(email, session);
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    private Long toEpochMillis(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
