package com.latteandletters.service;

import com.latteandletters.config.LegacyAwarePasswordEncoder;
import com.latteandletters.dto.RegistrationAvailabilityResult;
import com.latteandletters.dto.StudentRegistrationResult;
import com.latteandletters.model.Role;
import com.latteandletters.model.Student;
import com.latteandletters.model.User;
import com.latteandletters.model.UserStatus;
import com.latteandletters.repository.StudentRepository;
import com.latteandletters.repository.UserRepository;
import com.latteandletters.util.AddressFormValue;
import com.latteandletters.util.YearLevelOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class AuthService {

    private static final Pattern NAME_ALLOWED_PATTERN = Pattern.compile("^[A-Za-z](?:[A-Za-z .'-]{0,48}[A-Za-z])?$");
    private static final Pattern TRIPLE_REPEATED_LETTER_PATTERN = Pattern.compile("([A-Za-z])\\1{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-z0-9+_.-]+@[a-z0-9.-]+\\.[a-z]{2,}$");
    private static final Pattern CONTACT_PATTERN = Pattern.compile("^\\+?\\d{10,15}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{12,100}$");
    private static final Pattern ADDRESS_SUFFIX_PATTERN = Pattern.compile("^(.+?)\\s+(\\d{4})$");
    private static final int MAX_NAME_TOKEN_LENGTH = 12;
    private static final Map<String, String> LAGUNA_CITY_ZIP_CODES = createLagunaCityZipCodes();
    private static final Map<String, String> LAGUNA_CITY_CODES = createLagunaCityCodes();
    private static final String PSGC_BARANGAYS_API_URL = "https://psgc.cloud/api/v2/cities-municipalities/%s/barangays";
    private static final Set<String> ALLOWED_EMAIL_DOMAINS = Set.of(
            "gmail.com",
            "yahoo.com",
            "yahoo.com.ph",
            "outlook.com",
            "hotmail.com",
            "live.com",
            "icloud.com",
            "proton.me",
            "protonmail.com",
            "aol.com",
            "gmx.com",
            "mail.com"
    );
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password",
            "password123",
            "12345678",
            "123456789",
            "qwerty123",
            "admin123",
            "welcome123",
            "letmein123",
            "iloveyou",
            "abc12345",
            "passw0rd",
            "student123",
            "adminadmin",
            "11111111",
            "12341234"
    );

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final LegacyAwarePasswordEncoder passwordEncoder;
    private final EmailNotificationService emailNotificationService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private HttpClient httpClient;
    private final Map<String, List<String>> lagunaBarangaysByCityCache = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       LegacyAwarePasswordEncoder passwordEncoder,
                       EmailNotificationService emailNotificationService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNotificationService = emailNotificationService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public StudentRegistrationResult registerStudent(String firstName,
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
                                                     boolean agreed,
                                                     boolean activateImmediately) {
        String normalizedFirstName = normalizeAndValidateName(firstName, "First name", false);
        String normalizedMiddleName = normalizeAndValidateName(middleName, "Middle name", true);
        String normalizedLastName = normalizeAndValidateName(lastName, "Last name", false);
        String normalizedSuffix = normalizeAndValidateSuffix(suffix);
        String normalizedFullName = buildFullName(normalizedFirstName, normalizedMiddleName, normalizedLastName, normalizedSuffix);
        String normalizedProgram = normalizeAndValidateProgram(program);
        String normalizedYearLevel = validateRequiredYearLevel(yearLevel);
        String normalizedEmail = normalizeAndValidateEmail(email);
        String normalizedContactNumber = normalizeAndValidateContact(contactNumber);
        LocalDate parsedBirthDate = parseAndValidateBirthDate(birthDate);
        String normalizedProvince = validateProvince(province);
        String normalizedCityMunicipality = validateCityMunicipality(cityMunicipality);
        String normalizedBarangay = validateBarangayForCityMunicipality(normalizedCityMunicipality, barangay);
        String normalizedStreet = hasText(street) ? normalizeAndValidateAddressPart(street, "Street", 80) : "";
        String normalizedZipCode = validateZipCode(zipcode, normalizedCityMunicipality);
        validateTerms(agreed);

        String generatedStudentId = generateStudentId();
        String generatedPassword = generateTemporaryPassword(
                normalizedFirstName,
                normalizedLastName,
                parsedBirthDate,
                generatedStudentId
        );

        User user = new User();
        user.setName(normalizedFullName);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(generatedPassword));
        user.setRole(Role.STUDENT);
        user.setStatus(activateImmediately ? UserStatus.ACTIVE : UserStatus.PENDING);
        user.setMustChangePassword(true);
        userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentId(generatedStudentId);
        student.setCourse(normalizedProgram);
        student.setYearLevel(normalizedYearLevel);
        student.setPhone(normalizedContactNumber);
        student.setAddress(buildAddress(normalizedStreet, normalizedBarangay, normalizedCityMunicipality, normalizedProvince, normalizedZipCode));
        student.setDateOfBirth(parsedBirthDate);

        Student savedStudent = studentRepository.save(student);
        emailNotificationService.sendRegistrationTemporaryPassword(savedStudent.getUser(), generatedPassword);
        return new StudentRegistrationResult(savedStudent, generatedPassword);
    }

    @Transactional
    public Student createStudentByAdmin(String name,
                                        String email,
                                        String password,
                                        String course,
                                        String yearLevel,
                                        String phone,
                                        String address,
                                        LocalDate dateOfBirth,
                                        UserStatus status) {
        String normalizedName = normalizeFullName(required(name, "Student name is required."));
        if (countLetters(normalizedName) < 2) {
            throw new IllegalArgumentException("Student name must contain at least 2 letters.");
        }

        String normalizedEmail = normalizeAndValidateEmail(email);
        String normalizedPassword = validateAdminPassword(password);
        String normalizedYearLevel = validateRequiredYearLevel(yearLevel);
        if (dateOfBirth != null) {
            validateBirthDate(dateOfBirth);
        }

        String generatedStudentId = generateStudentId();

        User user = new User();
        user.setName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        user.setRole(Role.STUDENT);
        user.setStatus(status == null ? UserStatus.ACTIVE : status);
        user.setMustChangePassword(true);
        userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentId(generatedStudentId);
        student.setCourse(defaultText(course, "Not set", 100, "Course"));
        student.setYearLevel(normalizedYearLevel);
        student.setPhone(blankToNull(phone, 30, "Phone"));
        student.setAddress(blankToNull(address, 200, "Address"));
        student.setDateOfBirth(dateOfBirth);

        return studentRepository.save(student);
    }

    public Map<String, String> getLagunaCityZipCodes() {
        return LAGUNA_CITY_ZIP_CODES;
    }

    public List<String> getBarangaysForCityMunicipality(String cityMunicipality) {
        String normalizedCityMunicipality = validateCityMunicipality(cityMunicipality);
        return lagunaBarangaysByCityCache.computeIfAbsent(normalizedCityMunicipality, this::fetchBarangaysForCityMunicipality);
    }

    public RegistrationAvailabilityResult checkRegistrationAvailability(String field, String value) {
        String normalizedField = field == null ? "" : field.trim().toLowerCase(Locale.ROOT);

        try {
            return switch (normalizedField) {
                case "email" -> {
                    String normalizedValue = normalizeAndValidateEmailFormat(value);
                    boolean available = !userRepository.existsByEmailIgnoreCase(normalizedValue);
                    yield new RegistrationAvailabilityResult(
                            true,
                            available,
                            normalizedValue,
                            available ? "" : "This email is already taken."
                    );
                }
                case "contactnumber", "contact-number", "contact_number", "phone" -> {
                    String normalizedValue = normalizeAndValidateContactFormat(value);
                    boolean available = !studentRepository.existsByPhone(normalizedValue);
                    yield new RegistrationAvailabilityResult(
                            true,
                            available,
                            normalizedValue,
                            available ? "" : "This contact number is already used."
                    );
                }
                default -> throw new IllegalArgumentException("Unsupported registration field.");
            };
        } catch (IllegalArgumentException exception) {
            return new RegistrationAvailabilityResult(false, false, value == null ? "" : value.trim(), exception.getMessage());
        }
    }

    public String normalizeAndBuildOptionalAddress(String province,
                                                   String cityMunicipality,
                                                   String barangay,
                                                   String street,
                                                   String zipcode) {
        boolean hasAddressInput = hasText(cityMunicipality)
                || hasText(barangay)
                || hasText(street)
                || hasText(zipcode);
        if (!hasAddressInput) {
            return null;
        }

        String normalizedProvince = validateProvince(hasText(province) ? province : "Laguna");
        String normalizedCityMunicipality = validateCityMunicipality(cityMunicipality);
        String normalizedBarangay = validateBarangayForCityMunicipality(normalizedCityMunicipality, barangay);
        String normalizedStreet = hasText(street) ? normalizeAndValidateAddressPart(street, "Street", 180) : "";
        String normalizedZipCode = validateZipCode(zipcode, normalizedCityMunicipality);
        return buildAddress(normalizedStreet, normalizedBarangay, normalizedCityMunicipality, normalizedProvince, normalizedZipCode);
    }

    public AddressFormValue parseAddress(String address) {
        AddressFormValue formValue = new AddressFormValue();
        if (!hasText(address)) {
            return formValue;
        }

        String[] segments = address.trim().split("\\s*,\\s*");
        if (segments.length >= 4) {
            String suffix = segments[segments.length - 1].trim();
            Matcher matcher = ADDRESS_SUFFIX_PATTERN.matcher(suffix);
            if (matcher.matches()) {
                formValue.setProvince(matcher.group(1).trim());
                formValue.setZipcode(matcher.group(2).trim());
                formValue.setCityMunicipality(segments[segments.length - 2].trim());
                formValue.setBarangay(segments[segments.length - 3].trim());
                formValue.setStreet(String.join(", ", java.util.Arrays.copyOfRange(segments, 0, segments.length - 3)).trim());
                return formValue;
            }
        }

        formValue.setStreet(address.trim());
        return formValue;
    }

    private String generateStudentId() {
        long nextSequence = studentRepository.findTopByOrderByIdDesc()
                .map(Student::getId)
                .orElse(0L) + 1;
        String prefix = LocalDate.now().getYear() % 100 + "1";
        return String.format("%s-%04d", prefix, nextSequence);
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeAndValidateName(String value, String label, boolean optional) {
        String normalized = normalizeName(value);

        if (normalized.isBlank()) {
            if (optional) {
                return "";
            }
            throw new IllegalArgumentException(label + " is required.");
        }
        if (!NAME_ALLOWED_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Use letters only. Spaces, apostrophe, hyphen, and period are allowed.");
        }
        if (countLetters(normalized) < 2) {
            throw new IllegalArgumentException(label + " must be at least 2 letters.");
        }
        if (TRIPLE_REPEATED_LETTER_PATTERN.matcher(normalized).find()) {
            throw new IllegalArgumentException("Avoid triple repeated letters in names.");
        }
        if (hasTooLongNameToken(normalized)) {
            throw new IllegalArgumentException("Please avoid random long letter sequences in names.");
        }
        return normalized;
    }

    private String normalizeAndValidateSuffix(String value) {
        String normalized = normalizeName(value);
        if (normalized.isBlank()) {
            return "";
        }
        if (!NAME_ALLOWED_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Use letters only. Spaces, apostrophe, hyphen, and period are allowed.");
        }
        if (countLetters(normalized) < 2) {
            throw new IllegalArgumentException("Suffix must be at least 2 letters.");
        }
        if (TRIPLE_REPEATED_LETTER_PATTERN.matcher(normalized).find()) {
            throw new IllegalArgumentException("Avoid triple repeated letters in names.");
        }
        if (hasTooLongNameToken(normalized)) {
            throw new IllegalArgumentException("Please avoid random long letter sequences in names.");
        }
        return normalized;
    }

    private String buildFullName(String firstName, String middleName, String lastName, String suffix) {
        String fullName = String.join(" ", firstName, middleName, lastName, suffix)
                .trim()
                .replaceAll("\\s+", " ");
        if (fullName.length() > 100) {
            throw new IllegalArgumentException("Combined full name is too long.");
        }
        return fullName;
    }

    private String normalizeAndValidateEmail(String email) {
        String normalizedEmail = normalizeAndValidateEmailFormat(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("This email is already taken.");
        }
        return normalizedEmail;
    }

    private String normalizeAndValidateProgram(String program) {
        String normalizedProgram = required(program, "Program is required.")
                .replaceAll("\\s+", " ");
        if (normalizedProgram.length() < 3) {
            throw new IllegalArgumentException("Program must be at least 3 characters.");
        }
        if (normalizedProgram.length() > 100) {
            throw new IllegalArgumentException("Program is too long.");
        }
        return normalizedProgram;
    }

    private String validateRequiredYearLevel(String yearLevel) {
        String normalizedYearLevel = required(yearLevel, "Year level is required.")
                .replaceAll("\\s+", " ");
        if (!YearLevelOptions.isSupported(normalizedYearLevel)) {
            throw new IllegalArgumentException("Select a valid year level.");
        }
        return normalizedYearLevel;
    }

    private String normalizeAndValidateContact(String contactNumber) {
        String normalizedContact = normalizeAndValidateContactFormat(contactNumber);
        if (studentRepository.existsByPhone(normalizedContact)) {
            throw new IllegalArgumentException("This contact number is already used.");
        }
        return normalizedContact;
    }

    private String normalizeAndValidateEmailFormat(String email) {
        String rawEmail = required(email, "Email address is required.");
        String trimmedEmail = rawEmail.trim();

        if (!trimmedEmail.equals(trimmedEmail.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Use lowercase email only.");
        }
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches() || hasInvalidEmailDots(trimmedEmail)) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }

        String domain = getEmailDomain(trimmedEmail);
        if (!isAllowedEmailDomain(domain)) {
            throw new IllegalArgumentException("Use a supported provider domain or school email.");
        }

        return trimmedEmail;
    }

    private String normalizeAndValidateContactFormat(String contactNumber) {
        String normalizedContact = normalizeContact(required(contactNumber, "Contact number is required."));
        if (!CONTACT_PATTERN.matcher(normalizedContact).matches()) {
            throw new IllegalArgumentException("Use 10 to 15 digits for the contact number.");
        }
        return normalizedContact;
    }

    private LocalDate parseAndValidateBirthDate(String birthDate) {
        String normalizedBirthDate = required(birthDate, "Birthday is required.");
        LocalDate parsedBirthDate;
        try {
            parsedBirthDate = LocalDate.parse(normalizedBirthDate);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Enter a valid birth date.");
        }

        if (parsedBirthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birth date cannot be in the future.");
        }

        int age = Period.between(parsedBirthDate, LocalDate.now()).getYears();
        if (age < 5 || age > 120) {
            throw new IllegalArgumentException("Age must be between 5 and 120.");
        }

        return parsedBirthDate;
    }

    private String validateProvince(String province) {
        String normalizedProvince = required(province, "Province is required.");
        if (!"Laguna".equalsIgnoreCase(normalizedProvince)) {
            throw new IllegalArgumentException("Province must be Laguna for this registration form.");
        }
        return "Laguna";
    }

    private String validateCityMunicipality(String cityMunicipality) {
        String normalizedCityMunicipality = required(cityMunicipality, "City or municipality is required.");
        if (!LAGUNA_CITY_ZIP_CODES.containsKey(normalizedCityMunicipality)) {
            throw new IllegalArgumentException("Select a valid Laguna city or municipality.");
        }
        return normalizedCityMunicipality;
    }

    private String validateBarangayForCityMunicipality(String cityMunicipality, String barangay) {
        String normalizedBarangay = normalizeAndValidateAddressPart(barangay, "Barangay", 60);

        List<String> barangays;
        try {
            barangays = getBarangaysForCityMunicipality(cityMunicipality);
        } catch (IllegalStateException exception) {
            throw new IllegalArgumentException("Unable to verify the barangay right now. Please try again.");
        }

        String barangayLookupKey = normalizeLookupValue(normalizedBarangay);
        for (String officialBarangayName : barangays) {
            if (normalizeLookupValue(officialBarangayName).equals(barangayLookupKey)) {
                return officialBarangayName;
            }
        }

        throw new IllegalArgumentException("Select a valid barangay for the chosen city or municipality.");
    }

    private String normalizeAndValidateAddressPart(String value, String label, int maxLength) {
        String normalizedValue = required(value, label + " is required.")
                .replaceAll("\\s+", " ");
        if (normalizedValue.length() < 2) {
            throw new IllegalArgumentException(label + " must be at least 2 characters.");
        }
        if (normalizedValue.length() > maxLength) {
            throw new IllegalArgumentException(label + " is too long.");
        }
        return normalizedValue;
    }

    private String validateZipCode(String zipcode, String cityMunicipality) {
        String normalizedZipCode = required(zipcode, "Zip code is required.");
        if (!normalizedZipCode.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("Zip code must contain 4 digits.");
        }
        String expectedZipCode = LAGUNA_CITY_ZIP_CODES.get(cityMunicipality);
        if (!normalizedZipCode.equals(expectedZipCode)) {
            throw new IllegalArgumentException("Zip code does not match the selected city or municipality.");
        }
        return normalizedZipCode;
    }

    private String validateAdminPassword(String password) {
        String normalizedPassword = required(password, "Password is required.");
        if (normalizedPassword.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters.");
        }
        if (!PASSWORD_PATTERN.matcher(normalizedPassword).matches()) {
            throw new IllegalArgumentException("Password must include uppercase, lowercase, number, and special character.");
        }
        if (COMMON_PASSWORDS.contains(normalizedPassword.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("That password is too common. Please choose a stronger password.");
        }
        if (hasRepeatedSequence(normalizedPassword, 4)) {
            throw new IllegalArgumentException("Avoid repeated characters in your password.");
        }
        return normalizedPassword;
    }

    private void validateTerms(boolean agreed) {
        if (!agreed) {
            throw new IllegalArgumentException("Please confirm your registration details before submitting.");
        }
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String defaultText(String value, String fallback, int maxLength, String label) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " is too long.");
        }
        return normalized;
    }

    private String blankToNull(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " is too long.");
        }
        return normalized;
    }

    private String normalizeFullName(String value) {
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("Student name is too long.");
        }
        return normalized;
    }

    private int countLetters(String value) {
        int count = 0;
        for (char ch : value.toCharArray()) {
            if (Character.isLetter(ch)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasTooLongNameToken(String value) {
        String[] tokens = value.split("[ .'-]+");
        for (String token : tokens) {
            if (!token.isBlank() && token.length() > MAX_NAME_TOKEN_LENGTH) {
                return true;
            }
        }
        return false;
    }

    private boolean hasInvalidEmailDots(String value) {
        int atIndex = value.indexOf('@');
        if (atIndex <= 0 || atIndex >= value.length() - 1) {
            return true;
        }

        String local = value.substring(0, atIndex);
        String domain = value.substring(atIndex + 1);
        return local.startsWith(".")
                || local.endsWith(".")
                || local.contains("..")
                || domain.startsWith(".")
                || domain.endsWith(".")
                || domain.contains("..");
    }

    private String getEmailDomain(String value) {
        int atIndex = value.indexOf('@');
        return atIndex < 0 ? "" : value.substring(atIndex + 1);
    }

    private boolean isAllowedEmailDomain(String domain) {
        return ALLOWED_EMAIL_DOMAINS.contains(domain)
                || domain.endsWith(".edu")
                || domain.endsWith(".edu.ph");
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birth date cannot be in the future.");
        }

        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 5 || age > 120) {
            throw new IllegalArgumentException("Age must be between 5 and 120.");
        }
    }

    private String normalizeContact(String value) {
        return value.replaceAll("[ .\\-()]", "");
    }

    private String buildAddress(String street,
                                String barangay,
                                String cityMunicipality,
                                String province,
                                String zipcode) {
        String prefix = (street == null || street.isBlank()) ? "" : street + ", ";
        return prefix + barangay + ", " + cityMunicipality + ", " + province + " " + zipcode;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private boolean hasRepeatedSequence(String value, int minLength) {
        int repeats = 1;
        for (int index = 1; index < value.length(); index++) {
            if (value.charAt(index) == value.charAt(index - 1)) {
                repeats++;
                if (repeats >= minLength) {
                    return true;
                }
            } else {
                repeats = 1;
            }
        }
        return false;
    }

    private boolean containsPersonalInfo(String password,
                                         String firstName,
                                         String middleName,
                                         String lastName,
                                         String email,
                                         String contactNumber,
                                         String birthDate) {
        String lowerPassword = password.toLowerCase(Locale.ROOT);
        String[] tokens = {
                normalizeToken(firstName),
                normalizeToken(middleName),
                normalizeToken(lastName),
                normalizeToken(getEmailLocalPart(email)),
                normalizeToken(contactNumber),
                normalizeToken(birthDate)
        };

        for (String token : tokens) {
            if (token.length() >= 3 && lowerPassword.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String generateTemporaryPassword(String firstName,
                                             String lastName,
                                             LocalDate birthDate,
                                             String studentId) {
        String uppercase = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lowercase = "abcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        String symbols = "!@#$%?";
        String all = uppercase + lowercase + digits + symbols;

        List<Character> characters = new ArrayList<>();
        characters.add(randomChar(uppercase));
        characters.add(randomChar(lowercase));
        characters.add(randomChar(digits));
        characters.add(randomChar(symbols));
        while (characters.size() < 14) {
            characters.add(randomChar(all));
        }
        Collections.shuffle(characters, secureRandom);

        StringBuilder password = new StringBuilder(characters.size());
        for (Character character : characters) {
            password.append(character);
        }
        return password.toString();
    }

    private char randomChar(String source) {
        return source.charAt(secureRandom.nextInt(source.length()));
    }

    private String stripToLetters(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z]", "");
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String getEmailLocalPart(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private List<String> fetchBarangaysForCityMunicipality(String cityMunicipality) {
        String cityCode = LAGUNA_CITY_CODES.get(cityMunicipality);
        if (cityCode == null) {
            throw new IllegalArgumentException("Select a valid Laguna city or municipality.");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(PSGC_BARANGAYS_API_URL, cityCode)))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        try {
            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Unable to load barangay data at the moment.");
            }

            JsonNode dataNode = objectMapper.readTree(response.body()).path("data");
            if (!dataNode.isArray()) {
                throw new IllegalStateException("Unexpected barangay response format.");
            }

            List<String> barangays = new ArrayList<>();
            for (JsonNode barangayNode : dataNode) {
                String barangayName = barangayNode.path("name").asText("").trim();
                if (!barangayName.isEmpty() && !containsBarangayIgnoreCase(barangays, barangayName)) {
                    barangays.add(barangayName);
                }
            }

            barangays.sort(String.CASE_INSENSITIVE_ORDER);
            return Collections.unmodifiableList(barangays);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to load barangay data at the moment.", exception);
        } catch (IOException | UncheckedIOException exception) {
            throw new IllegalStateException("Unable to load barangay data at the moment.", exception);
        }
    }

    private HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        }
        return httpClient;
    }

    private boolean containsBarangayIgnoreCase(List<String> barangays, String candidate) {
        String lookupKey = normalizeLookupValue(candidate);
        for (String barangay : barangays) {
            if (normalizeLookupValue(barangay).equals(lookupKey)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeLookupValue(String value) {
        String normalizedValue = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalizedValue
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static Map<String, String> createLagunaCityZipCodes() {
        Map<String, String> cityZipCodes = new LinkedHashMap<>();
        cityZipCodes.put("Alaminos", "4001");
        cityZipCodes.put("Bay", "4033");
        cityZipCodes.put("Binan City", "4024");
        cityZipCodes.put("Cabuyao City", "4025");
        cityZipCodes.put("Calamba City", "4027");
        cityZipCodes.put("Calauan", "4012");
        cityZipCodes.put("Cavinti", "4013");
        cityZipCodes.put("Famy", "4021");
        cityZipCodes.put("Kalayaan", "4015");
        cityZipCodes.put("Liliw", "4004");
        cityZipCodes.put("Los Banos", "4030");
        cityZipCodes.put("Luisiana", "4032");
        cityZipCodes.put("Lumban", "4014");
        cityZipCodes.put("Mabitac", "4020");
        cityZipCodes.put("Magdalena", "4007");
        cityZipCodes.put("Majayjay", "4005");
        cityZipCodes.put("Nagcarlan", "4002");
        cityZipCodes.put("Paete", "4016");
        cityZipCodes.put("Pagsanjan", "4008");
        cityZipCodes.put("Pakil", "4017");
        cityZipCodes.put("Pangil", "4018");
        cityZipCodes.put("Pila", "4010");
        cityZipCodes.put("Rizal", "4003");
        cityZipCodes.put("San Pablo City", "4000");
        cityZipCodes.put("San Pedro City", "4023");
        cityZipCodes.put("Santa Cruz", "4009");
        cityZipCodes.put("Santa Maria", "4022");
        cityZipCodes.put("Santa Rosa City", "4026");
        cityZipCodes.put("Siniloan", "4019");
        cityZipCodes.put("Victoria", "4011");
        return Collections.unmodifiableMap(cityZipCodes);
    }

    private static Map<String, String> createLagunaCityCodes() {
        Map<String, String> cityCodes = new LinkedHashMap<>();
        cityCodes.put("Alaminos", "0403401000");
        cityCodes.put("Bay", "0403402000");
        cityCodes.put("Binan City", "0403403000");
        cityCodes.put("Cabuyao City", "0403404000");
        cityCodes.put("Calamba City", "0403405000");
        cityCodes.put("Calauan", "0403406000");
        cityCodes.put("Cavinti", "0403407000");
        cityCodes.put("Famy", "0403408000");
        cityCodes.put("Kalayaan", "0403409000");
        cityCodes.put("Liliw", "0403410000");
        cityCodes.put("Los Banos", "0403411000");
        cityCodes.put("Luisiana", "0403412000");
        cityCodes.put("Lumban", "0403413000");
        cityCodes.put("Mabitac", "0403414000");
        cityCodes.put("Magdalena", "0403415000");
        cityCodes.put("Majayjay", "0403416000");
        cityCodes.put("Nagcarlan", "0403417000");
        cityCodes.put("Paete", "0403418000");
        cityCodes.put("Pagsanjan", "0403419000");
        cityCodes.put("Pakil", "0403420000");
        cityCodes.put("Pangil", "0403421000");
        cityCodes.put("Pila", "0403422000");
        cityCodes.put("Rizal", "0403423000");
        cityCodes.put("San Pablo City", "0403424000");
        cityCodes.put("San Pedro City", "0403425000");
        cityCodes.put("Santa Cruz", "0403426000");
        cityCodes.put("Santa Maria", "0403427000");
        cityCodes.put("Santa Rosa City", "0403428000");
        cityCodes.put("Siniloan", "0403429000");
        cityCodes.put("Victoria", "0403430000");
        return Collections.unmodifiableMap(cityCodes);
    }
}
