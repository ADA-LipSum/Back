package com.ada.proj.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path baseDir;

    private static final Set<String> IMAGE_EXTS = Set.of("jpg","jpeg","png","gif","webp","bmp");
    private static final Set<String> VIDEO_EXTS = Set.of("mp4","mov","avi","mkv","webm");

    public FileStorageService(@Value("${app.storage.base-dir:uploads}") String baseDir) throws IOException {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
        Files.createDirectories(this.baseDir.resolve("images"));
        Files.createDirectories(this.baseDir.resolve("videos"));
    }

    public StoredFile storeImage(MultipartFile file) throws IOException {
        return store(file, "images", IMAGE_EXTS);
    }

    public StoredFile storeVideo(MultipartFile file) throws IOException {
        return store(file, "videos", VIDEO_EXTS);
    }

    private StoredFile store(MultipartFile file, String folder, Set<String> allowedExts) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }
    String originalName = file.getOriginalFilename();
    if (originalName == null) originalName = "file";
    String original = StringUtils.cleanPath(originalName);
        String ext = getExtension(original).toLowerCase();
        if (!allowedExts.contains(ext)) {
            throw new IllegalArgumentException("Unsupported file extension: ." + ext);
        }
        String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : ("." + ext));
        Path targetDir = baseDir.resolve(folder);
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(storedName);
        Files.copy(file.getInputStream(), target);
        String url = "/files/" + folder + "/" + storedName;
        String contentType = detectContentType(target, file.getContentType());
        return new StoredFile(original, storedName, url, Files.size(target), contentType);
    }

    private String detectContentType(Path path, String fallback) throws IOException {
    String probe = Files.probeContentType(path);
    if (probe != null) return probe;
    if (fallback != null) return fallback;
    return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private static String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i == -1) ? "" : filename.substring(i + 1);
    }

    public record StoredFile(String originalName, String storedName, String url, long size, String contentType) {}
}
