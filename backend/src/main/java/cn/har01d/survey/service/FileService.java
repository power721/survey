package cn.har01d.survey.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cn.har01d.survey.exception.BusinessException;

@Service
public class FileService {

    private final String uploadDir;
    private final long maxSize;

    public FileService(@Value("${app.upload.dir:uploads}") String uploadDir,
                       @Value("${app.upload.max-size:10485760}") long maxSize) {
        this.uploadDir = uploadDir;
        this.maxSize = maxSize;
    }

    public String upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("file.empty");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException("file.size.exceeded");
        }

        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
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
            Path filePath = Paths.get(uploadDir).toAbsolutePath().resolve(fileName);
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
        return Paths.get(uploadDir).toAbsolutePath().resolve(fileName);
    }
}
