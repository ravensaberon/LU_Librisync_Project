package com.lulibrisync.service;

import com.lulibrisync.model.Student;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class StudentProfileImageService {

    private static final long MAX_UPLOAD_BYTES = 5L * 1024L * 1024L;
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

    private final Path profileImagesRoot;

    public StudentProfileImageService(@Value("${lulibrisync.storage.root:${user.dir}/storage}") String storageRootPath) {
        Path storageRoot = Path.of(storageRootPath).toAbsolutePath().normalize();
        this.profileImagesRoot = storageRoot.resolve("profile-pictures").resolve("students").normalize();
        try {
            Files.createDirectories(this.profileImagesRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize student profile image storage.", exception);
        }
    }

    public boolean hasProfileImage(Student student) {
        return resolveStoredImagePath(student) != null;
    }

    public String getProfileImageVersion(Student student) {
        Path storedImagePath = resolveStoredImagePath(student);
        if (storedImagePath == null) {
            return "";
        }

        try {
            return String.valueOf(Files.getLastModifiedTime(storedImagePath).toMillis());
        } catch (IOException exception) {
            return String.valueOf(Instant.now().toEpochMilli());
        }
    }

    public Resource getProfileImageResource(Student student) {
        Path storedImagePath = resolveStoredImagePath(student);
        if (storedImagePath == null || !Files.isRegularFile(storedImagePath)) {
            throw new IllegalArgumentException("Profile picture not found.");
        }
        return new PathResource(storedImagePath);
    }

    public MediaType getProfileImageMediaType(Student student) {
        Path storedImagePath = resolveStoredImagePath(student);
        if (storedImagePath == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaTypeFactory.getMediaType(storedImagePath.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    public void storeProfileImage(Student student, MultipartFile profileImage) {
        if (student == null || student.getStudentId() == null || student.getStudentId().isBlank()) {
            throw new IllegalArgumentException("Student profile was not found.");
        }
        if (profileImage == null || profileImage.isEmpty()) {
            throw new IllegalArgumentException("Choose an image file first.");
        }
        if (profileImage.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("Profile picture must not exceed 5 MB.");
        }

        String originalFilename = profileImage.getOriginalFilename();
        String extension = normalizeExtension(StringUtils.getFilenameExtension(originalFilename));
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only JPG, PNG, or WEBP images are allowed.");
        }

        String studentKey = slugify(student.getStudentId());
        String safeFileName = studentKey + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "." + extension;
        Path targetPath = profileImagesRoot.resolve(safeFileName).normalize();
        if (!targetPath.startsWith(profileImagesRoot)) {
            throw new IllegalArgumentException("Invalid profile picture path.");
        }

        deleteProfileImage(student);

        try {
            Files.copy(profileImage.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to store the uploaded profile picture.");
        }
    }

    public void deleteProfileImage(Student student) {
        Path storedImagePath = resolveStoredImagePath(student);
        if (storedImagePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(storedImagePath);
        } catch (IOException ignored) {
            // Keep cleanup best-effort so profile updates remain responsive.
        }
    }

    private Path resolveStoredImagePath(Student student) {
        if (student == null || student.getStudentId() == null || student.getStudentId().isBlank()) {
            return null;
        }

        String studentKey = slugify(student.getStudentId());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(profileImagesRoot, studentKey + "-*")) {
            return java.util.stream.StreamSupport.stream(stream.spliterator(), false)
                    .filter(Files::isRegularFile)
                    .max(Comparator.comparing(this::lastModifiedMillis))
                    .orElse(null);
        } catch (IOException exception) {
            return null;
        }
    }

    private long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return Long.MIN_VALUE;
        }
    }

    private String normalizeExtension(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value == null ? "student" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "student" : normalized;
    }
}
