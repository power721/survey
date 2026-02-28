package cn.har01d.survey.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cn.har01d.survey.exception.BusinessException;

@Service
public class FileService {

    private final String uploadDir;
    private final long defaultMaxSize;
    private final Set<String> defaultAllowedExtensions;
    private final SystemConfigService configService;

    public FileService(@Value("${app.upload.dir:uploads}") String uploadDir,
                       @Value("${app.upload.max-size:10485760}") long maxSize,
                       @Value("${app.upload.allowed-extensions:.jpg,.jpeg,.png,.gif,.bmp,.webp,.svg,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.csv,.zip,.rar,.7z,.mp3,.mp4,.wav,.avi,.mov}") String allowedExtensions,
                       SystemConfigService configService) {
        this.uploadDir = uploadDir;
        this.defaultMaxSize = maxSize;
        this.defaultAllowedExtensions = Set.of(allowedExtensions.toLowerCase().split(","));
        this.configService = configService;
    }

    private long getMaxSize() {
        String val = configService.get(SystemConfigService.UPLOAD_MAX_SIZE);
        if (!val.isEmpty()) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return defaultMaxSize;
    }

    private Set<String> getAllowedExtensions() {
        String val = configService.get(SystemConfigService.UPLOAD_ALLOWED_EXTENSIONS);
        if (!val.isEmpty()) {
            return Set.of(val.toLowerCase().split(","));
        }
        return defaultAllowedExtensions;
    }

    public String upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("file.empty");
        }
        if (file.getSize() > getMaxSize()) {
            throw new BusinessException("file.size.exceeded");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (!ext.isEmpty() && !getAllowedExtensions().contains(ext)) {
            throw new BusinessException("file.type.not.allowed");
        }

        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String fileName = UUID.randomUUID() + ext;

            Path target = dir.resolve(fileName);
            file.transferTo(target.toFile());

            return "/api/files/" + fileName;
        } catch (IOException e) {
            throw new BusinessException("file.upload.failed");
        }
    }

    public void delete(String fileName) {
        try {
            Path filePath = safePath(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new BusinessException("file.delete.failed");
        }
    }

    public List<String> listOldFiles(int hours) {
        List<String> fileNames = new ArrayList<>();
        Path dir = Paths.get(uploadDir).toAbsolutePath();
        if (!Files.exists(dir)) {
            return fileNames;
        }
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toInstant().isBefore(cutoff);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(p -> fileNames.add(p.getFileName().toString()));
        } catch (IOException e) {
            // ignore
        }
        return fileNames;
    }

    public Path getFilePath(String fileName) {
        return safePath(fileName);
    }

    private Path safePath(String fileName) {
        if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new BusinessException("file.invalid.name");
        }
        Path dir = Paths.get(uploadDir).toAbsolutePath();
        Path filePath = dir.resolve(fileName).normalize();
        if (!filePath.startsWith(dir)) {
            throw new BusinessException("file.invalid.name");
        }
        return filePath;
    }
}
