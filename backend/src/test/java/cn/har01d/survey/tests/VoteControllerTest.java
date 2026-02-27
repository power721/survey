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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VoteControllerTest extends BaseIntegrationTest {

    private String token;
    private String otherToken;
    private Long pollId;
    private String shareId;
    private Long option1Id;
    private Long option2Id;
    private Long option3Id;

    @BeforeAll
    void setup() throws Exception {
        token = registerAndLogin("voteuser", "password123");
        otherToken = registerAndLogin("voteother", "password123");
    }

    // --- createPoll ---

    @Test
    @Order(1)
    void createPoll_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Favorite Color",
                "description", "Pick your favorite color",
                "voteType", "SINGLE",
                "frequency", "ONCE",
                "accessLevel", "PUBLIC",
                "anonymous", true,
                "options", List.of(
                        Map.of("title", "Red"),
                        Map.of("title", "Blue"),
                        Map.of("title", "Green")
                )
        ));

        MvcResult result = mockMvc.perform(post("/api/votes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        JsonNode data = json.get("data");
        pollId = data.get("id").asLong();
        shareId = data.get("shareId").asText();
        assertNotNull(pollId);
        assertNotNull(shareId);
        assertEquals("Favorite Color", data.get("title").asText());
        assertEquals("SINGLE", data.get("voteType").asText());
        assertEquals(3, data.get("options").size());

        option1Id = data.get("options").get(0).get("id").asLong();
        option2Id = data.get("options").get(1).get("id").asLong();
        option3Id = data.get("options").get(2).get("id").asLong();
    }

    @Test
    @Order(2)
    void createPoll_unauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Test", "options", List.of(Map.of("title", "A"))
        ));
        mockMvc.perform(post("/api/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void createPoll_missingTitle() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "options", List.of(Map.of("title", "A"))
        ));
        mockMvc.perform(post("/api/votes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    void createPoll_emptyOptions() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Test", "options", List.of()
        ));
        mockMvc.perform(post("/api/votes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- getPoll ---

    @Test
    @Order(5)
    void getPoll_byId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/votes/" + pollId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertEquals(pollId.longValue(), json.get("data").get("id").asLong());
    }

    @Test
    @Order(6)
    void getPoll_notFound() throws Exception {
        mockMvc.perform(get("/api/votes/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // --- updatePoll ---

    @Test
    @Order(7)
    void updatePoll_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Updated Color Poll",
                "description", "Updated description",
                "voteType", "SINGLE",
                "frequency", "ONCE",
                "accessLevel", "PUBLIC",
                "anonymous", true,
                "options", List.of(
                        Map.of("id", option1Id, "title", "Red Updated"),
                        Map.of("id", option2Id, "title", "Blue"),
                        Map.of("title", "Yellow")
                )
        ));

        MvcResult result = mockMvc.perform(put("/api/votes/" + pollId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        assertEquals("Updated Color Poll", json.get("data").get("title").asText());
        assertEquals(3, json.get("data").get("options").size());

        // Update option IDs for later use
        option1Id = json.get("data").get("options").get(0).get("id").asLong();
        option2Id = json.get("data").get("options").get(1).get("id").asLong();
        option3Id = json.get("data").get("options").get(2).get("id").asLong();
    }

    @Test
    @Order(8)
    void updatePoll_accessDenied() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Hacked", "options", List.of(Map.of("title", "A"), Map.of("title", "B"))
        ));
        mockMvc.perform(put("/api/votes/" + pollId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // --- publishPoll ---

    @Test
    @Order(9)
    void publishPoll_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/votes/" + pollId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertEquals("PUBLISHED", json.get("data").get("status").asText());
    }

    @Test
    @Order(10)
    void publishPoll_accessDenied() throws Exception {
        mockMvc.perform(post("/api/votes/" + pollId + "/publish")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    // --- getPollByShareId (public) ---

    @Test
    @Order(11)
    void getPollByShareId_success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/votes/v/" + shareId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertEquals(shareId, json.get("data").get("shareId").asText());
    }

    @Test
    @Order(12)
    void getPollByShareId_notFound() throws Exception {
        mockMvc.perform(get("/api/votes/v/nonexistent"))
                .andExpect(status().isNotFound());
    }

    // --- submitVote (SINGLE) ---

    @Test
    @Order(13)
    void submitVote_single_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "optionIds", List.of(option1Id),
                "deviceId", "dev_test1"
        ));

        MvcResult result = mockMvc.perform(post("/api/votes/v/" + shareId + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        assertEquals(1, json.get("data").get("totalVoteCount").asInt());
    }

    @Test
    @Order(14)
    void submitVote_single_multipleOptions() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "optionIds", List.of(option1Id, option2Id),
                "deviceId", "dev_test2"
        ));

        mockMvc.perform(post("/api/votes/v/" + shareId + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- getMyPolls ---

    @Test
    @Order(15)
    void getMyPolls_success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/votes/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
        assertTrue(json.get("data").get("content").size() >= 1);
    }

    @Test
    @Order(16)
    void getMyPolls_unauthorized() throws Exception {
        mockMvc.perform(get("/api/votes/my"))
                .andExpect(status().isForbidden());
    }

    // --- getPublicPolls ---

    @Test
    @Order(17)
    void getPublicPolls_success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/votes/public"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertTrue(json.get("success").asBoolean());
    }

    // --- MULTIPLE vote type ---

    @Test
    @Order(20)
    void createAndSubmit_multipleVote() throws Exception {
        // Create MULTIPLE poll
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Multi Poll",
                "voteType", "MULTIPLE",
                "frequency", "ONCE",
                "accessLevel", "PUBLIC",
                "maxOptions", 2,
                "options", List.of(
                        Map.of("title", "A"),
                        Map.of("title", "B"),
                        Map.of("title", "C")
                )
        ));

        MvcResult createResult = mockMvc.perform(post("/api/votes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode createJson = parseResponse(createResult);
        Long multiPollId = createJson.get("data").get("id").asLong();
        String multiShareId = createJson.get("data").get("shareId").asText();
        Long optA = createJson.get("data").get("options").get(0).get("id").asLong();
        Long optB = createJson.get("data").get("options").get(1).get("id").asLong();
        Long optC = createJson.get("data").get("options").get(2).get("id").asLong();

        // Publish
        mockMvc.perform(post("/api/votes/" + multiPollId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Submit with 2 options (within maxOptions)
        String submitBody = objectMapper.writeValueAsString(Map.of(
                "optionIds", List.of(optA, optB),
                "deviceId", "dev_multi1"
        ));
        MvcResult submitResult = mockMvc.perform(post("/api/votes/v/" + multiShareId + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode submitJson = parseResponse(submitResult);
        assertEquals(2, submitJson.get("data").get("totalVoteCount").asInt());
    }

    @Test
    @Order(21)
    void submitVote_multiple_exceedsMaxOptions() throws Exception {
        // Create MULTIPLE poll with maxOptions=1
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Limited Multi",
                "voteType", "MULTIPLE",
                "frequency", "ONCE",
                "accessLevel", "PUBLIC",
                "maxOptions", 1,
                "options", List.of(
                        Map.of("title", "X"),
                        Map.of("title", "Y")
                )
        ));

        MvcResult createResult = mockMvc.perform(post("/api/votes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode createJson = parseResponse(createResult);
        Long pid = createJson.get("data").get("id").asLong();
        String sid = createJson.get("data").get("shareId").asText();
        Long oX = createJson.get("data").get("options").get(0).get("id").asLong();
        Long oY = createJson.get("data").get("options").get(1).get("id").asLong();

        // Publish
        mockMvc.perform(post("/api/votes/" + pid + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Submit with 2 options (exceeds maxOptions=1)
        String submitBody = objectMapper.writeValueAsString(Map.of(
                "optionIds", List.of(oX, oY),
                "deviceId", "dev_limited1"
        ));
        mockMvc.perform(post("/api/votes/v/" + sid + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isBadRequest());
    }

    // --- SCORED vote type ---

    @Test
    @Order(30)
    void createAndSubmit_scoredVote() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Scored Poll",
                "voteType", "SCORED",
                "frequency", "ONCE",
                "accessLevel", "PUBLIC",
                "maxVotesPerOption", 5,
                "maxTotalVotes", 10,
                "options", List.of(
                        Map.of("title", "S1"),
                        Map.of("title", "S2")
                )
        ));

        MvcResult createResult = mockMvc.perform(post("/api/votes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode createJson = parseResponse(createResult);
        Long scoredPollId = createJson.get("data").get("id").asLong();
        String scoredShareId = createJson.get("data").get("shareId").asText();
        Long sOpt1 = createJson.get("data").get("options").get(0).get("id").asLong();
        Long sOpt2 = createJson.get("data").get("options").get(1).get("id").asLong();
        assertEquals(5, createJson.get("data").get("maxVotesPerOption").asInt());
        assertEquals(10, createJson.get("data").get("maxTotalVotes").asInt());

        // Publish
        mockMvc.perform(post("/api/votes/" + scoredPollId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Submit scored votes
        String submitBody = objectMapper.writeValueAsString(Map.of(
                "votes", Map.of(sOpt1, 3, sOpt2, 2),
                "deviceId", "dev_scored1"
        ));
        MvcResult submitResult = mockMvc.perform(post("/api/votes/v/" + scoredShareId + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode submitJson = parseResponse(submitResult);
        assertEquals(5, submitJson.get("data").get("totalVoteCount").asInt());
    }

    // --- closePoll ---

    @Test
    @Order(40)
    void closePoll_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/votes/" + pollId + "/close")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parseResponse(result);
        assertEquals("CLOSED", json.get("data").get("status").asText());
    }

    @Test
    @Order(41)
    void closePoll_accessDenied() throws Exception {
        mockMvc.perform(post("/api/votes/" + pollId + "/close")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    // --- deletePoll ---

    @Test
    @Order(50)
    void deletePoll_accessDenied() throws Exception {
        mockMvc.perform(delete("/api/votes/" + pollId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(51)
    void deletePoll_success() throws Exception {
        mockMvc.perform(delete("/api/votes/" + pollId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verify deleted
        mockMvc.perform(get("/api/votes/" + pollId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
