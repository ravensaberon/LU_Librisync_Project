package com.latteandletters.service;

import com.latteandletters.model.Book;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
@SuppressWarnings("null")
public class DigitalLibraryService {

    private final Path storageRoot;
    private final Path ebooksRoot;
    private final Path bookCoversRoot;

    public DigitalLibraryService(@Value("${latteandletters.storage.root:${user.dir}/storage}") String storageRootPath) {
        this.storageRoot = Path.of(storageRootPath).toAbsolutePath().normalize();
        this.ebooksRoot = this.storageRoot.resolve("ebooks").normalize();
        this.bookCoversRoot = this.storageRoot.resolve("book-covers").normalize();
        try {
            Files.createDirectories(this.ebooksRoot);
            Files.createDirectories(this.bookCoversRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize digital library storage.", exception);
        }
    }

    public String storeEbookFile(String bookTitle, MultipartFile ebookFile) {
        if (ebookFile == null || ebookFile.isEmpty()) {
            return null;
        }

        String originalFilename = ebookFile.getOriginalFilename();
        String fileExtension = StringUtils.getFilenameExtension(originalFilename);
        if (fileExtension == null || !"pdf".equalsIgnoreCase(fileExtension)) {
            throw new IllegalArgumentException("Only PDF files are allowed for e-book upload.");
        }

        String safeFileName = slugify(bookTitle) + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".pdf";
        Path targetPath = ebooksRoot.resolve(safeFileName).normalize();
        if (!targetPath.startsWith(ebooksRoot)) {
            throw new IllegalArgumentException("Invalid e-book upload path.");
        }

        try {
            Files.copy(ebookFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to store the uploaded PDF file.");
        }

        return "ebooks/" + safeFileName;
    }

    public String storeBookCoverFile(String bookTitle, MultipartFile coverImageFile) {
        if (coverImageFile == null || coverImageFile.isEmpty()) {
            return null;
        }

        String originalFilename = coverImageFile.getOriginalFilename();
        String fileExtension = StringUtils.getFilenameExtension(originalFilename);
        if (!isSupportedCoverExtension(fileExtension)) {
            throw new IllegalArgumentException("Only JPG, PNG, or WEBP files are allowed for book cover upload.");
        }

        String normalizedFileExtension = fileExtension.toLowerCase(Locale.ROOT);
        String safeFileName = slugify(bookTitle) + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "." + normalizedFileExtension;
        Path targetPath = bookCoversRoot.resolve(safeFileName).normalize();
        if (!targetPath.startsWith(bookCoversRoot)) {
            throw new IllegalArgumentException("Invalid book cover upload path.");
        }

        try {
            Files.copy(coverImageFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to store the uploaded book cover image.");
        }

        return "book-covers/" + safeFileName;
    }

    public Resource getEbookResource(Book book) {
        if (book == null || !StringUtils.hasText(book.getEbookPath())) {
            throw new IllegalArgumentException("No e-book file is linked to this title.");
        }
        if (isExternalResourcePath(book.getEbookPath())) {
            throw new IllegalArgumentException("This title uses an external digital copy link.");
        }

        Path resolvedPath = resolveBookPath(book.getEbookPath());
        if (!Files.exists(resolvedPath) || !Files.isRegularFile(resolvedPath)) {
            throw new IllegalArgumentException("The linked e-book file could not be found.");
        }

        return new PathResource(resolvedPath);
    }

    public Resource getBookCoverResource(Book book) {
        Path resolvedPath = resolveBookCoverPath(book);
        if (resolvedPath == null) {
            throw new IllegalArgumentException("No readable book cover is available for this title.");
        }

        return new PathResource(resolvedPath);
    }

    public boolean hasReadableEbook(Book book) {
        if (book == null || !StringUtils.hasText(book.getEbookPath())) {
            return false;
        }
        if (isExternalResourcePath(book.getEbookPath())) {
            return true;
        }

        try {
            Path resolvedPath = resolveBookPath(book.getEbookPath());
            return Files.exists(resolvedPath) && Files.isRegularFile(resolvedPath);
        } catch (InvalidPathException exception) {
            return false;
        }
    }

    public boolean hasReadableBookCover(Book book) {
        return resolveBookCoverPath(book) != null;
    }

    public void deleteManagedEbook(String ebookPath) {
        if (!StringUtils.hasText(ebookPath) || !isManagedEbookPath(ebookPath)) {
            return;
        }

        Path resolvedPath = resolveManagedPath(ebookPath);
        try {
            Files.deleteIfExists(resolvedPath);
        } catch (IOException ignored) {
            // Keep delete cleanup best-effort so catalog operations do not fail.
        }
    }

    public boolean isManagedEbookPath(String ebookPath) {
        return StringUtils.hasText(ebookPath)
                && ebookPath.replace('\\', '/').startsWith("ebooks/");
    }

    public void deleteManagedBookCover(String coverImagePath) {
        if (!StringUtils.hasText(coverImagePath) || !isManagedBookCoverPath(coverImagePath)) {
            return;
        }

        Path resolvedPath = resolveManagedPath(coverImagePath);
        try {
            Files.deleteIfExists(resolvedPath);
        } catch (IOException ignored) {
            // Keep delete cleanup best-effort so catalog operations do not fail.
        }
    }

    public boolean isManagedBookCoverPath(String coverImagePath) {
        return StringUtils.hasText(coverImagePath)
                && coverImagePath.replace('\\', '/').startsWith("book-covers/");
    }

    private Path resolveManagedPath(String relativeStoragePath) {
        Path resolvedPath = storageRoot.resolve(relativeStoragePath).toAbsolutePath().normalize();
        if (!resolvedPath.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Invalid managed storage path.");
        }
        return resolvedPath;
    }

    private Path resolveBookPath(String storedPath) {
        String normalizedPath = storedPath == null ? "" : storedPath.trim();
        if (isManagedEbookPath(normalizedPath) || isManagedBookCoverPath(normalizedPath)) {
            return resolveManagedPath(normalizedPath);
        }

        Path directPath = Path.of(normalizedPath);
        if (!directPath.isAbsolute()) {
            directPath = Path.of(System.getProperty("user.dir")).resolve(directPath);
        }
        return directPath.toAbsolutePath().normalize();
    }

    private Path resolveBookCoverPath(Book book) {
        if (book == null) {
            return null;
        }

        if (StringUtils.hasText(book.getCoverImage())) {
            try {
                Path resolvedPath = resolveBookPath(book.getCoverImage());
                if (Files.exists(resolvedPath) && Files.isRegularFile(resolvedPath)) {
                    return resolvedPath;
                }
            } catch (InvalidPathException ignored) {
                // Fall back to title-based lookup below.
            }
        }

        for (String candidateName : buildCoverFileNameCandidates(book.getTitle())) {
            for (String extension : new String[]{"jpg", "jpeg", "png", "webp"}) {
                Path candidatePath = bookCoversRoot.resolve(candidateName + "." + extension).normalize();
                if (Files.exists(candidatePath) && Files.isRegularFile(candidatePath)) {
                    return candidatePath;
                }
            }
        }

        return null;
    }

    public boolean isExternalEbookUrl(Book book) {
        return book != null && isExternalResourcePath(book.getEbookPath());
    }

    public String getExternalEbookUrl(Book book) {
        if (book == null || !isExternalResourcePath(book.getEbookPath())) {
            throw new IllegalArgumentException("This title does not use an external digital link.");
        }
        return book.getEbookPath().trim();
    }

    private boolean isExternalResourcePath(String storedPath) {
        if (!StringUtils.hasText(storedPath)) {
            return false;
        }

        String normalizedPath = storedPath.trim();
        try {
            URI uri = new URI(normalizedPath);
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value == null ? "ebook" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "ebook" : normalized;
    }

    private Set<String> buildCoverFileNameCandidates(String title) {
        Set<String> candidates = new LinkedHashSet<>();
        String normalizedTitle = title == null ? "" : title.trim();
        if (!normalizedTitle.isBlank()) {
            addCoverCandidate(candidates, normalizedTitle);

            int colonIndex = normalizedTitle.indexOf(':');
            if (colonIndex > 0) {
                addCoverCandidate(candidates, normalizedTitle.substring(0, colonIndex));
            }

            int dashIndex = normalizedTitle.indexOf(" - ");
            if (dashIndex > 0) {
                addCoverCandidate(candidates, normalizedTitle.substring(0, dashIndex));
            }
        }
        return candidates;
    }

    private void addCoverCandidate(Set<String> candidates, String rawValue) {
        String slug = slugify(rawValue);
        if (!slug.isBlank()) {
            candidates.add(slug);
            candidates.add(slug.replace("-", ""));
        }
    }

    private boolean isSupportedCoverExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return "jpg".equalsIgnoreCase(extension)
                || "jpeg".equalsIgnoreCase(extension)
                || "png".equalsIgnoreCase(extension)
                || "webp".equalsIgnoreCase(extension);
    }
}
