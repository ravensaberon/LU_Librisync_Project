package com.latteandletters.service;

import com.latteandletters.config.LegacyAwarePasswordEncoder;
import com.latteandletters.dto.BorrowerStanding;
import com.latteandletters.dto.StudentProfileUpdateRequest;
import com.latteandletters.model.IssueStatus;
import com.latteandletters.model.Student;
import com.latteandletters.model.User;
import com.latteandletters.model.UserStatus;
import com.latteandletters.repository.IssueRecordRepository;
import com.latteandletters.repository.StudentRepository;
import com.latteandletters.repository.UserRepository;
import com.latteandletters.util.YearLevelOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@SuppressWarnings("null")
public class StudentService {

    private static final Pattern PROFILE_PHONE_PATTERN = Pattern.compile("^\\+?[0-9 ()\\-]{7,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{12,100}$");

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final IssueRecordRepository issueRecordRepository;
    private final LegacyAwarePasswordEncoder passwordEncoder;
    private final CirculationPolicyService circulationPolicyService;

    public StudentService(StudentRepository studentRepository,
                          UserRepository userRepository,
                          IssueRecordRepository issueRecordRepository,
                          LegacyAwarePasswordEncoder passwordEncoder,
                          CirculationPolicyService circulationPolicyService) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.issueRecordRepository = issueRecordRepository;
        this.passwordEncoder = passwordEncoder;
        this.circulationPolicyService = circulationPolicyService;
    }

    public Student getStudentByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        return studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found."));
    }

    public boolean mustChangePassword(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        return user.isMustChangePassword();
    }

    public Student getStudentByStudentId(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Student ID is required.");
        }

        return studentRepository.findByStudentId(studentId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));
    }

    public List<Student> searchStudents(String studentIdKeyword) {
        return searchStudents(studentIdKeyword, false);
    }

    public List<Student> searchStudents(String studentIdKeyword, boolean archivedOnly) {
        if (studentIdKeyword == null || studentIdKeyword.isBlank()) {
            return studentRepository.findAllByOrderByStudentIdAsc().stream()
                    .filter(student -> archivedOnly
                            ? UserStatus.ARCHIVED.equals(student.getUser().getStatus())
                            : !UserStatus.ARCHIVED.equals(student.getUser().getStatus()))
                    .toList();
        }
        return studentRepository.findByStudentIdContainingIgnoreCaseOrderByStudentIdAsc(studentIdKeyword.trim()).stream()
                .filter(student -> archivedOnly
                        ? UserStatus.ARCHIVED.equals(student.getUser().getStatus())
                        : !UserStatus.ARCHIVED.equals(student.getUser().getStatus()))
                .toList();
    }

    public BorrowerStanding getBorrowerStanding(Student student) {
        return circulationPolicyService.evaluateStanding(student);
    }

    public BorrowerStanding getBorrowerStandingByStudentId(String studentId) {
        return circulationPolicyService.evaluateStanding(getStudentByStudentId(studentId));
    }

    public BorrowerStanding getBorrowerStandingByEmail(String email) {
        return circulationPolicyService.evaluateStanding(getStudentByEmail(email));
    }

    public StudentProfileUpdateRequest createProfileUpdateRequest(Student student) {
        return new StudentProfileUpdateRequest(
                student.getUser().getFirstName(),
                student.getUser().getMiddleName(),
                student.getUser().getLastName(),
                student.getUser().getSuffix(),
                student.getCourse(),
                student.getYearLevel(),
                student.getPhone(),
                student.getAddress(),
                student.getDateOfBirth()
        );
    }

    public StudentProfileUpdateRequest normalizeProfileUpdateRequest(StudentProfileUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Profile details are required.");
        }

        String normalizedFirstName = normalizeRequiredText(request.getFirstName(), "First name", 50);
        String normalizedMiddleName = normalizeOptionalText(request.getMiddleName(), "Middle name", 50, null);
        String normalizedLastName = normalizeRequiredText(request.getLastName(), "Last name", 50);
        String normalizedSuffix = normalizeOptionalText(request.getSuffix(), "Suffix", 20, null);
        String normalizedCourse = normalizeOptionalText(request.getCourse(), "Course", 100, "Not set");
        String normalizedYearLevel = normalizeOptionalYearLevel(request.getYearLevel());
        String normalizedPhone = normalizeOptionalPhone(request.getPhone());
        String normalizedAddress = normalizeOptionalText(request.getAddress(), "Address", 200, null);
        LocalDate normalizedDateOfBirth = request.getDateOfBirth();

        if (normalizedDateOfBirth != null) {
            validateBirthDate(normalizedDateOfBirth);
        }

        return new StudentProfileUpdateRequest(
                normalizedFirstName,
                normalizedMiddleName,
                normalizedLastName,
                normalizedSuffix,
                normalizedCourse,
                normalizedYearLevel,
                normalizedPhone,
                normalizedAddress,
                normalizedDateOfBirth
        );
    }

    @Transactional
    public Student updateProfile(String email,
                                 String name,
                                 String course,
                                 String yearLevel,
                                 String phone,
                                 String address,
                                 LocalDate dateOfBirth) {
        return updateProfile(email, new StudentProfileUpdateRequest(name, course, yearLevel, phone, address, dateOfBirth));
    }

    @Transactional
    public Student updateProfile(String email, StudentProfileUpdateRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        Student student = studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found."));

        StudentProfileUpdateRequest normalizedRequest = normalizeProfileUpdateRequest(request);

        if (normalizedRequest.getPhone() != null
                && studentRepository.existsByPhone(normalizedRequest.getPhone())
                && !normalizedRequest.getPhone().equals(student.getPhone())) {
            throw new IllegalArgumentException("This contact number is already used.");
        }

        user.setFirstName(normalizedRequest.getFirstName());
        user.setMiddleName(normalizedRequest.getMiddleName());
        user.setLastName(normalizedRequest.getLastName());
        user.setSuffix(normalizedRequest.getSuffix());
        student.setCourse(normalizedRequest.getCourse());
        student.setYearLevel(normalizedRequest.getYearLevel());
        student.setPhone(normalizedRequest.getPhone());
        student.setAddress(normalizedRequest.getAddress());
        student.setDateOfBirth(normalizedRequest.getDateOfBirth());

        userRepository.save(user);
        return studentRepository.save(student);
    }

    @Transactional
    public Student updateStudentByAdmin(String studentId,
                                        String name,
                                        String email,
                                        String course,
                                        String yearLevel,
                                        String phone,
                                        String address,
                                        LocalDate dateOfBirth,
                                        UserStatus status) {
        Student student = getStudentByStudentId(studentId);
        User user = student.getUser();

        String normalizedName = required(name, "Student name is required.");
        String normalizedEmail = normalizeEmail(required(email, "Email address is required."));

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, user.getId())) {
            throw new IllegalArgumentException("Email already exists.");
        }
        String normalizedPhone = blankToNull(phone);
        if (normalizedPhone != null && studentRepository.existsByPhone(normalizedPhone) && !normalizedPhone.equals(student.getPhone())) {
            throw new IllegalArgumentException("This contact number is already used.");
        }
        if (dateOfBirth != null) {
            validateBirthDate(dateOfBirth);
        }

        user.setName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setStatus(status == null ? UserStatus.ACTIVE : status);

        student.setCourse(normalizeOptionalText(course, "Course", 100, "Not set"));
        student.setYearLevel(normalizeOptionalYearLevel(yearLevel));
        student.setPhone(normalizedPhone);
        student.setAddress(normalizeOptionalText(address, "Address", 200, null));
        student.setDateOfBirth(dateOfBirth);

        userRepository.save(user);
        return studentRepository.save(student);
    }

    @Transactional
    public void resetStudentPassword(String studentId, String newPassword, String confirmPassword) {
        Student student = getStudentByStudentId(studentId);
        User user = student.getUser();

        String normalizedPassword = required(newPassword, "New password is required.");
        String normalizedConfirmPassword = required(confirmPassword, "Confirm password is required.");

        if (!normalizedPassword.equals(normalizedConfirmPassword)) {
            throw new IllegalArgumentException("Password and confirmation do not match.");
        }
        if (normalizedPassword.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters.");
        }
        if (!PASSWORD_PATTERN.matcher(normalizedPassword).matches()) {
            throw new IllegalArgumentException("Password must include uppercase, lowercase, number, and special character.");
        }
        if (passwordEncoder.matches(normalizedPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Choose a different password from the current one.");
        }

        user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String email,
                               String currentPassword,
                               String newPassword,
                               String confirmPassword) {
        User user = userRepository.findByEmailIgnoreCase(required(email, "User not found."))
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        String normalizedCurrentPassword = required(currentPassword, "Current password is required.");
        String normalizedNewPassword = required(newPassword, "New password is required.");
        String normalizedConfirmPassword = required(confirmPassword, "Confirm password is required.");

        if (!passwordEncoder.matches(normalizedCurrentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (!normalizedNewPassword.equals(normalizedConfirmPassword)) {
            throw new IllegalArgumentException("Password and confirmation do not match.");
        }
        if (normalizedNewPassword.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters.");
        }
        if (!PASSWORD_PATTERN.matcher(normalizedNewPassword).matches()) {
            throw new IllegalArgumentException("Password must include uppercase, lowercase, number, and special character.");
        }
        if (passwordEncoder.matches(normalizedNewPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Choose a different password from the current one.");
        }

        user.setPasswordHash(passwordEncoder.encode(normalizedNewPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Transactional
    public void completeRequiredPasswordChange(String email,
                                               String newPassword,
                                               String confirmPassword) {
        User user = userRepository.findByEmailIgnoreCase(required(email, "User not found."))
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!user.isMustChangePassword()) {
            throw new IllegalArgumentException("A required password change is not active for this account.");
        }

        String normalizedNewPassword = required(newPassword, "New password is required.");
        String normalizedConfirmPassword = required(confirmPassword, "Confirm password is required.");

        if (!normalizedNewPassword.equals(normalizedConfirmPassword)) {
            throw new IllegalArgumentException("Password and confirmation do not match.");
        }
        if (normalizedNewPassword.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters.");
        }
        if (!PASSWORD_PATTERN.matcher(normalizedNewPassword).matches()) {
            throw new IllegalArgumentException("Password must include uppercase, lowercase, number, and special character.");
        }
        if (passwordEncoder.matches(normalizedNewPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Choose a different password from the temporary password.");
        }

        user.setPasswordHash(passwordEncoder.encode(normalizedNewPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Transactional
    public void deleteStudent(String studentId) {
        Student student = getStudentByStudentId(studentId);
        long activeIssues = issueRecordRepository.countByStudent_IdAndStatusIn(student.getId(), List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE));
        if (activeIssues > 0) {
            throw new IllegalArgumentException("Resolve all active issues before deleting this student account.");
        }

        User user = student.getUser();
        studentRepository.delete(student);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Transactional
    public void archiveStudent(String studentId) {
        Student student = getStudentByStudentId(studentId);
        long activeIssues = issueRecordRepository.countByStudent_IdAndStatusIn(student.getId(), List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE));
        if (activeIssues > 0) {
            throw new IllegalArgumentException("Resolve all active issues before archiving this student account.");
        }
        student.getUser().setStatus(UserStatus.ARCHIVED);
        userRepository.save(student.getUser());
    }

    @Transactional
    public void restoreArchivedStudent(String studentId) {
        Student student = getStudentByStudentId(studentId);
        if (!UserStatus.ARCHIVED.equals(student.getUser().getStatus())) {
            throw new IllegalArgumentException("Only archived student accounts can be restored.");
        }
        student.getUser().setStatus(UserStatus.ACTIVE);
        userRepository.save(student.getUser());
    }

    @Transactional
    public void permanentlyDeleteArchivedStudent(String studentId) {
        Student student = getStudentByStudentId(studentId);
        if (!UserStatus.ARCHIVED.equals(student.getUser().getStatus())) {
            throw new IllegalArgumentException("Archive the student account first before permanent deletion.");
        }
        deleteStudent(studentId);
    }

    public UserStatus[] getAvailableStatuses() {
        return UserStatus.values();
    }

    public long countActiveStudents() {
        return studentRepository.countByUser_Status(UserStatus.ACTIVE);
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeEmail(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains("@") || normalized.startsWith("@") || normalized.endsWith("@")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        return normalized;
    }

    private String normalizeRequiredText(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " is too long.");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, String label, int maxLength, String fallback) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return fallback;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " is too long.");
        }
        return normalized;
    }

    private String normalizeOptionalYearLevel(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return "Not set";
        }
        if (!YearLevelOptions.isSupported(normalized)) {
            throw new IllegalArgumentException("Select a valid year level.");
        }
        return normalized;
    }

    private String normalizeOptionalPhone(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (!PROFILE_PHONE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Enter a valid phone number.");
        }
        return normalized;
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birth date cannot be in the future.");
        }

        int age = birthDate.until(LocalDate.now()).getYears();
        if (age < 5 || age > 120) {
            throw new IllegalArgumentException("Age must be between 5 and 120.");
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
