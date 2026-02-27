package cn.har01d.survey.tests;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileControllerTest extends BaseIntegrationTest {

    private String token;
    private String uploadedFileName;

    @BeforeAll
    void setup() throws Exception {
        token = registerAndLogin("fileuser", "password123");
    }

    @Test
    @Order(1)
    void upload_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes()
        );

        MvcResult result = mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        String url = json.get("data").get("url").asText();
        assertNotNull(url);
        assertEquals("test.txt", json.get("data").get("name").asText());

        // Extract filename from URL for download test
        uploadedFileName = url.substring(url.lastIndexOf("/") + 1);
    }

    @Test
    @Order(2)
    void upload_unauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes()
        );

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void download_success() throws Exception {
        mockMvc.perform(get("/api/files/" + uploadedFileName))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    void download_notFound() throws Exception {
        mockMvc.perform(get("/api/files/nonexistent.txt"))
                .andExpect(status().isNotFound());
    }
}
