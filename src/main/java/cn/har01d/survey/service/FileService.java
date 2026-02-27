package cn.har01d.survey.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

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
            throw new BusinessException("File is empty");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException("File size exceeds limit");
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
            throw new BusinessException("File upload failed: " + e.getMessage());
        }
    }

    public Path getFilePath(String fileName) {
        return Paths.get(uploadDir).toAbsolutePath().resolve(fileName);
    }
}
