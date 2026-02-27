package cn.har01d.survey.service;

import cn.har01d.survey.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileServiceTest {

    private FileService fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileService = new FileService(tempDir.toString(), 10485760L);
    }

    @Test
    void upload_success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("test.pdf");

        String url = fileService.upload(file);

        assertNotNull(url);
        assertTrue(url.startsWith("/api/files/"));
        assertTrue(url.endsWith(".pdf"));
        verify(file).transferTo(any(java.io.File.class));
    }

    @Test
    void upload_emptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> fileService.upload(file));
        assertEquals("File is empty", ex.getMessage());
    }

    @Test
    void upload_exceedsMaxSize() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(20_000_000L);

        BusinessException ex = assertThrows(BusinessException.class, () -> fileService.upload(file));
        assertEquals("File size exceeds limit", ex.getMessage());
    }

    @Test
    void upload_noExtension() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("noext");

        String url = fileService.upload(file);

        assertNotNull(url);
        assertTrue(url.startsWith("/api/files/"));
        assertFalse(url.contains("."));
    }

    @Test
    void upload_nullFilename() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn(null);

        String url = fileService.upload(file);

        assertNotNull(url);
        assertTrue(url.startsWith("/api/files/"));
    }

    @Test
    void getFilePath() {
        Path path = fileService.getFilePath("test.pdf");

        assertEquals(tempDir.resolve("test.pdf"), path);
    }
}
