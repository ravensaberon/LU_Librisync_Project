package com.latteandletters.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.latteandletters.util.DisplayFormatUtils;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName = "";

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName = "";

    @Column(name = "suffix", length = 20)
    private String suffix;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.STUDENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user")
    private Student student;

    @OneToOne(mappedBy = "user")
    private Admin admin;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = normalizePart(firstName);
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = normalizeNullablePart(middleName);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = normalizePart(lastName);
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = normalizeNullablePart(suffix);
    }

    public String getName() {
        return buildFullName(firstName, middleName, lastName, suffix);
    }

    public void setName(String name) {
        ParsedName parsedName = parseName(name);
        this.firstName = parsedName.firstName();
        this.middleName = parsedName.middleName();
        this.lastName = parsedName.lastName();
        this.suffix = parsedName.suffix();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedAtDisplay() {
        return DisplayFormatUtils.formatDateTime(createdAt);
    }

    public String getCreatedAtDateDisplay() {
        return DisplayFormatUtils.formatDate(createdAt == null ? null : createdAt.toLocalDate());
    }

    public String getCreatedAtTimeDisplay() {
        return DisplayFormatUtils.formatTime(createdAt);
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    private static String buildFullName(String firstName,
                                        String middleName,
                                        String lastName,
                                        String suffix) {
        StringBuilder fullName = new StringBuilder();
        appendPart(fullName, firstName);
        appendPart(fullName, middleName);
        appendPart(fullName, lastName);
        appendPart(fullName, suffix);
        return fullName.toString().trim();
    }

    private static void appendPart(StringBuilder builder, String value) {
        String normalized = normalizeNullablePart(value);
        if (normalized == null) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(normalized);
    }

    private static String normalizePart(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return normalized;
    }

    private static String normalizeNullablePart(String value) {
        String normalized = normalizePart(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private static ParsedName parseName(String rawName) {
        String normalized = normalizePart(rawName);
        if (normalized.isEmpty()) {
            return new ParsedName("", null, "", null);
        }

        String[] tokens = normalized.split("\\s+");
        if (tokens.length == 1) {
            return new ParsedName(tokens[0], null, "", null);
        }
        if (tokens.length == 2) {
            return new ParsedName(tokens[0], null, tokens[1], null);
        }

        String detectedSuffix = null;
        int lastNameIndex = tokens.length - 1;
        if (looksLikeSuffix(tokens[tokens.length - 1])) {
            detectedSuffix = tokens[tokens.length - 1];
            lastNameIndex = tokens.length - 2;
        }

        String detectedFirstName = tokens[0];
        String detectedLastName = lastNameIndex <= 0 ? "" : tokens[lastNameIndex];
        String detectedMiddleName = lastNameIndex > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(tokens, 1, lastNameIndex))
                : null;

        return new ParsedName(
                detectedFirstName,
                normalizeNullablePart(detectedMiddleName),
                normalizePart(detectedLastName),
                normalizeNullablePart(detectedSuffix)
        );
    }

    private static boolean looksLikeSuffix(String token) {
        if (token == null) {
            return false;
        }
        String normalized = token.trim().replace(".", "").toUpperCase();
        return normalized.matches("JR|SR|I|II|III|IV|V|VI|PHD|MD|RN|CPA|ESQ");
    }

    private record ParsedName(String firstName, String middleName, String lastName, String suffix) {
    }
}
