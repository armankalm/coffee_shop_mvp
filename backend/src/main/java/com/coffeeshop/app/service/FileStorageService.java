package com.coffeeshop.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final Map<String, Set<String>> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", Set.of(".jpg", ".jpeg"),
            "image/png", Set.of(".png"),
            "image/webp", Set.of(".webp"),
            "image/gif", Set.of(".gif")
    );

    private final Path uploadDir;
    private final SupabaseStorageClient supabase;

    @Autowired
    public FileStorageService(@Value("${app.upload.dir:./uploads/products}") String uploadDir,
                              SupabaseStorageClient supabase) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.supabase = supabase;
        if (!useSupabase()) {
            // On Render the disk is wiped on every deploy, so these images would 404 afterwards.
            log.warn("Supabase Storage is not configured (SUPABASE_URL / SUPABASE_SERVICE_KEY): product images "
                    + "are stored on local disk at {} and will be lost if the disk is not persistent", this.uploadDir);
        }
    }

    /** Local-disk only storage. */
    public FileStorageService(String uploadDir) {
        this(uploadDir, null);
    }

    private boolean useSupabase() {
        return supabase != null && supabase.isEnabled();
    }

    public String store(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only image files are allowed (JPEG, PNG, WebP, GIF)");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        }

        Set<String> allowedExtensions = CONTENT_TYPE_EXTENSIONS.get(contentType);
        if (allowedExtensions == null || !allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException(
                    "File extension '" + extension + "' does not match content type '" + contentType + "'");
        }

        String filename = UUID.randomUUID() + extension;

        if (useSupabase()) {
            // Returns an absolute public URL; the frontend uses absolute URLs as-is.
            return supabase.upload(filename, file.getBytes(), contentType);
        }

        Files.createDirectories(uploadDir);
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new IOException("Cannot store file outside upload directory");
        }

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/products/" + filename;
    }

    public void deleteIfExists(String relativePath) {
        if (relativePath == null) {
            return;
        }
        String filename = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        if (useSupabase() && relativePath.startsWith(supabase.publicUrlPrefix())) {
            try {
                supabase.delete(filename);
            } catch (RuntimeException e) {
                log.warn("Failed to delete Supabase object: {}", filename, e);
            }
            return;
        }
        Path filePath = uploadDir.resolve(filename).normalize();
        if (filePath.startsWith(uploadDir)) {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Failed to delete file: {}", filePath, e);
            }
        }
    }

    public Path getUploadDir() {
        return uploadDir;
    }
}
