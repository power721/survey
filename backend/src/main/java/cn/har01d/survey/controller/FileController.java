package cn.har01d.survey.controller;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cn.har01d.survey.config.RateLimiter;
import cn.har01d.survey.dto.ApiResponse;
import cn.har01d.survey.exception.BusinessException;
import cn.har01d.survey.service.FileService;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final RateLimiter rateLimiter;

    public FileController(FileService fileService, RateLimiter rateLimiter) {
        this.fileService = fileService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(@RequestParam("file") MultipartFile file,
                                                                   HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!rateLimiter.isAllowed("upload:" + ip, 5, 60_000)) {
            throw new BusinessException("file.rate.limit", HttpStatus.TOO_MANY_REQUESTS);
        }
        String url = fileService.upload(file);
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        return ResponseEntity.ok(ApiResponse.ok("File uploaded", Map.of("url", url, "name", originalName)));
    }

    @DeleteMapping("/{fileName}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String fileName) {
        fileService.delete(fileName);
        return ResponseEntity.ok(ApiResponse.ok("File deleted", null));
    }

    @PostMapping("/{fileName}/delete")
    public ResponseEntity<ApiResponse<Void>> deleteByPost(@PathVariable String fileName,
                                                          HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!rateLimiter.isAllowed("delete:" + ip, 10, 60_000)) {
            throw new BusinessException("file.rate.limit", HttpStatus.TOO_MANY_REQUESTS);
        }
        fileService.delete(fileName);
        return ResponseEntity.ok(ApiResponse.ok("File deleted", null));
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> download(@PathVariable String fileName) throws MalformedURLException {
        Path filePath = fileService.getFilePath(fileName);
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_") + "\"")
                .body(resource);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
