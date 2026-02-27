package cn.har01d.survey.tests;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest extends BaseIntegrationTest {

    private static String token;

    @Test
    @Order(1)
    void register_success() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "authuser", "password", "password123", "nickname", "AuthUser")
        );
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        assertNotNull(json.get("data").get("token").asText());
        assertEquals("authuser", json.get("data").get("username").asText());
    }

    @Test
    @Order(2)
    void register_duplicateUsername() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "authuser", "password", "password123")
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    void register_invalidUsername_tooShort() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "ab", "password", "password123")
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    void register_invalidPassword_tooShort() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "validuser", "password", "12345")
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void login_success() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "authuser", "password", "password123")
        );
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        token = json.get("data").get("token").asText();
        assertNotNull(token);
        assertEquals("authuser", json.get("data").get("username").asText());
    }

    @Test
    @Order(6)
    void login_wrongPassword() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "authuser", "password", "wrongpassword")
        );
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    void login_nonExistentUser() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "nouser", "password", "password123")
        );
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    void getProfile_success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        assertEquals("authuser", json.get("data").get("username").asText());
    }

    @Test
    @Order(9)
    void getProfile_unauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(10)
    void updateProfile_success() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("nickname", "NewNick", "email", "test@example.com")
        );
        MvcResult result = mockMvc.perform(put("/api/auth/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        assertEquals("NewNick", json.get("data").get("nickname").asText());
    }

    @Test
    @Order(11)
    void updateProfile_unauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("nickname", "Hacked"));
        mockMvc.perform(put("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
