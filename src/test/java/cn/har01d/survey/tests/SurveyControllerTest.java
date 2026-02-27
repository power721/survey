package cn.har01d.survey.tests;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SurveyControllerTest extends BaseIntegrationTest {

    private String token;
    private String otherToken;
    private Long surveyId;
    private String shareId;
    private Long question1Id;
    private Long question2Id;
    private Long q1Option1Id;
    private Long q1Option2Id;

    @BeforeAll
    void setup() throws Exception {
        token = registerAndLogin("surveyuser", "password123");
        otherToken = registerAndLogin("surveyother", "password123");
    }

    // --- createSurvey ---

    @Test
    @Order(1)
    void createSurvey_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Customer Feedback",
                "description", "Please give us your feedback",
                "accessLevel", "PUBLIC",
                "anonymous", true,
                "questions", List.of(
                        Map.of(
                                "type", "SINGLE_CHOICE",
                                "title", "How satisfied are you?",
                                "required", true,
                                "sortOrder", 0,
                                "options", List.of(
                                        Map.of("content", "Very Satisfied", "sortOrder", 0),
                                        Map.of("content", "Satisfied", "sortOrder", 1)
                                )
                        ),
                        Map.of(
                                "type", "TEXT",
                                "title", "Any comments?",
                                "required", false,
                                "sortOrder", 1,
                                "options", List.of()
                        )
                )
        ));

        MvcResult result = mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        JsonNode data = json.get("data");
        surveyId = data.get("id").asLong();
        shareId = data.get("shareId").asText();
        assertNotNull(surveyId);
        assertNotNull(shareId);
        assertEquals("Customer Feedback", data.get("title").asText());
        assertEquals(2, data.get("questions").size());

        question1Id = data.get("questions").get(0).get("id").asLong();
        question2Id = data.get("questions").get(1).get("id").asLong();
        q1Option1Id = data.get("questions").get(0).get("options").get(0).get("id").asLong();
        q1Option2Id = data.get("questions").get(0).get("options").get(1).get("id").asLong();
    }

    @Test
    @Order(2)
    void createSurvey_unauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Test", "questions", List.of()
        ));
        mockMvc.perform(post("/api/surveys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void createSurvey_missingTitle() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "questions", List.of()
        ));
        mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- getSurvey ---

    @Test
    @Order(4)
    void getSurvey_byId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/surveys/" + surveyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertEquals(surveyId.longValue(), json.get("data").get("id").asLong());
    }

    @Test
    @Order(5)
    void getSurvey_notFound() throws Exception {
        mockMvc.perform(get("/api/surveys/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // --- updateSurvey ---

    @Test
    @Order(6)
    void updateSurvey_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Updated Feedback",
                "description", "Updated description",
                "accessLevel", "PUBLIC",
                "anonymous", true,
                "questions", List.of(
                        Map.of(
                                "id", question1Id,
                                "type", "SINGLE_CHOICE",
                                "title", "How satisfied are you? (updated)",
                                "required", true,
                                "sortOrder", 0,
                                "options", List.of(
                                        Map.of("id", q1Option1Id, "content", "Very Satisfied", "sortOrder", 0),
                                        Map.of("id", q1Option2Id, "content", "Satisfied", "sortOrder", 1),
                                        Map.of("content", "Neutral", "sortOrder", 2)
                                )
                        )
                )
        ));

        MvcResult result = mockMvc.perform(put("/api/surveys/" + surveyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        assertEquals("Updated Feedback", json.get("data").get("title").asText());

        // Update IDs for later use
        question1Id = json.get("data").get("questions").get(0).get("id").asLong();
        q1Option1Id = json.get("data").get("questions").get(0).get("options").get(0).get("id").asLong();
        q1Option2Id = json.get("data").get("questions").get(0).get("options").get(1).get("id").asLong();
    }

    @Test
    @Order(7)
    void updateSurvey_accessDenied() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Hacked", "questions", List.of()
        ));
        mockMvc.perform(put("/api/surveys/" + surveyId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // --- publishSurvey ---

    @Test
    @Order(8)
    void publishSurvey_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/surveys/" + surveyId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertEquals("PUBLISHED", json.get("data").get("status").asText());
    }

    @Test
    @Order(9)
    void publishSurvey_accessDenied() throws Exception {
        mockMvc.perform(post("/api/surveys/" + surveyId + "/publish")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    // --- getSurveyByShareId (public) ---

    @Test
    @Order(10)
    void getSurveyByShareId_success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/surveys/s/" + shareId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertEquals(shareId, json.get("data").get("shareId").asText());
    }

    @Test
    @Order(11)
    void getSurveyByShareId_notFound() throws Exception {
        mockMvc.perform(get("/api/surveys/s/nonexistent"))
                .andExpect(status().isNotFound());
    }

    // --- submitSurvey ---

    @Test
    @Order(12)
    void submitSurvey_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "answers", List.of(
                        Map.of("questionId", question1Id, "selectedOptionId", q1Option1Id)
                )
        ));

        MvcResult result = mockMvc.perform(post("/api/surveys/s/" + shareId + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
    }

    @Test
    @Order(13)
    void submitSurvey_emptyAnswers() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "answers", List.of()
        ));
        mockMvc.perform(post("/api/surveys/s/" + shareId + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- getResponses ---

    @Test
    @Order(14)
    void getResponses_success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/surveys/" + surveyId + "/responses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        assertTrue(json.get("data").get("content").size() >= 1);
    }

    @Test
    @Order(15)
    void getResponses_unauthorized() throws Exception {
        mockMvc.perform(get("/api/surveys/" + surveyId + "/responses"))
                .andExpect(status().isForbidden());
    }

    // --- getStatistics ---

    @Test
    @Order(16)
    void getStatistics_success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/surveys/" + surveyId + "/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        assertNotNull(json.get("data").get("totalResponses"));
    }

    // --- getMySurveys ---

    @Test
    @Order(17)
    void getMySurveys_success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/surveys/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        assertTrue(json.get("data").get("content").size() >= 1);
    }

    @Test
    @Order(18)
    void getMySurveys_unauthorized() throws Exception {
        mockMvc.perform(get("/api/surveys/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(19)
    void getMySurveys_withKeyword() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/surveys/my")
                        .param("keyword", "Updated")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
    }

    // --- getPublicSurveys ---

    @Test
    @Order(20)
    void getPublicSurveys_success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/surveys/public"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
    }

    // --- getTemplates ---

    @Test
    @Order(21)
    void getTemplates_success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/surveys/templates")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
    }

    // --- closeSurvey ---

    @Test
    @Order(30)
    void closeSurvey_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/surveys/" + surveyId + "/close")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertEquals("CLOSED", json.get("data").get("status").asText());
    }

    @Test
    @Order(31)
    void closeSurvey_accessDenied() throws Exception {
        mockMvc.perform(post("/api/surveys/" + surveyId + "/close")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    // --- exportResponses ---

    @Test
    @Order(32)
    void exportResponses_success() throws Exception {
        mockMvc.perform(get("/api/surveys/" + surveyId + "/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // --- deleteSurvey ---

    @Test
    @Order(40)
    void deleteSurvey_accessDenied() throws Exception {
        mockMvc.perform(delete("/api/surveys/" + surveyId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(41)
    void deleteSurvey_success() throws Exception {
        // Create a separate survey with no responses for clean deletion
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "To Delete",
                "accessLevel", "PUBLIC",
                "questions", List.of(
                        Map.of("type", "TEXT", "title", "Q1", "sortOrder", 0, "options", List.of())
                )
        ));
        MvcResult createResult = mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        Long deleteId = parseResponse(createResult).get("data").get("id").asLong();

        mockMvc.perform(delete("/api/surveys/" + deleteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verify deleted
        mockMvc.perform(get("/api/surveys/" + deleteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
