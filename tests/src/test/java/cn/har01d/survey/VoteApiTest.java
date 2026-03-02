package cn.har01d.survey;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VoteApiTest extends BaseApiTest {

    private String token;
    private String otherToken;

    // Single choice vote
    private Long singlePollId;
    private String singleShareId;
    private Long singleOptionId1;
    private Long singleOptionId2;

    // Multiple choice vote
    private Long multiPollId;
    private String multiShareId;
    private Long multiOptionId1;
    private Long multiOptionId2;
    private Long multiOptionId3;

    // Scored vote
    private Long scoredPollId;
    private String scoredShareId;
    private Long scoredOptionId1;
    private Long scoredOptionId2;

    @BeforeAll
    void setup() {
        token = loginOrRegisterAndGetToken(username, password);
        otherToken = loginOrRegisterAndGetToken(otherUsername, password);
    }

    // ==================== Create Vote Polls ====================

    @Test
    @Order(1)
    void createSingleVote_success() {
        Map<String, Object> body = buildVoteRequest("单选投票", "SINGLE", "ONCE");

        Response response = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/votes");

        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.title", equalTo("单选投票"))
                .body("data.voteType", equalTo("SINGLE"))
                .body("data.status", equalTo("DRAFT"))
                .body("data.shareId", notNullValue())
                .body("data.options", hasSize(3));

        singlePollId = response.jsonPath().getLong("data.id");
        singleShareId = response.jsonPath().getString("data.shareId");
        singleOptionId1 = response.jsonPath().getLong("data.options[0].id");
        singleOptionId2 = response.jsonPath().getLong("data.options[1].id");
    }

    @Test
    @Order(2)
    void createMultipleVote_success() {
        Map<String, Object> body = buildVoteRequest("多选投票", "MULTIPLE", "ONCE");
        body.put("maxOptions", 2);

        Response response = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/votes");

        response.then()
                .statusCode(200)
                .body("data.voteType", equalTo("MULTIPLE"));

        multiPollId = response.jsonPath().getLong("data.id");
        multiShareId = response.jsonPath().getString("data.shareId");
        multiOptionId1 = response.jsonPath().getLong("data.options[0].id");
        multiOptionId2 = response.jsonPath().getLong("data.options[1].id");
        multiOptionId3 = response.jsonPath().getLong("data.options[2].id");
    }

    @Test
    @Order(3)
    void createScoredVote_success() {
        Map<String, Object> body = buildVoteRequest("计分投票", "SCORED", "ONCE");
        body.put("maxTotalVotes", 10);
        body.put("maxVotesPerOption", 5);

        Response response = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/votes");

        response.then()
                .statusCode(200)
                .body("data.voteType", equalTo("SCORED"));

        scoredPollId = response.jsonPath().getLong("data.id");
        scoredShareId = response.jsonPath().getString("data.shareId");
        scoredOptionId1 = response.jsonPath().getLong("data.options[0].id");
        scoredOptionId2 = response.jsonPath().getLong("data.options[1].id");
    }

    @Test
    @Order(4)
    void createVote_unauthorized() {
        Map<String, Object> body = buildVoteRequest("无权限投票", "SINGLE", "ONCE");

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/votes")
                .then()
                .statusCode(403);
    }

    // ==================== Get Vote ====================

    @Test
    @Order(10)
    void getVote_byId_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/votes/" + singlePollId)
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.title", equalTo("单选投票"));
    }

    @Test
    @Order(11)
    void getVote_byId_forbidden() {
        given()
                .header("Authorization", "Bearer " + otherToken)
                .get("/api/votes/" + singlePollId)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(12)
    void getVote_byShareId_draftNotAccessible() {
        given()
                .get("/api/votes/v/" + singleShareId)
                .then()
                .statusCode(400);
    }

    // ==================== Update Vote ====================

    @Test
    @Order(20)
    void updateVote_success() {
        Map<String, Object> body = buildVoteRequest("更新后的投票", "SINGLE", "ONCE");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/votes/" + singlePollId)
                .then()
                .statusCode(200)
                .body("data.title", equalTo("更新后的投票"));
    }

    @Test
    @Order(21)
    void updateVote_forbidden() {
        Map<String, Object> body = buildVoteRequest("非法更新", "SINGLE", "ONCE");

        given()
                .header("Authorization", "Bearer " + otherToken)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/votes/" + singlePollId)
                .then()
                .statusCode(403);
    }

    // ==================== Publish ====================

    @Test
    @Order(30)
    void publishSingleVote() {
        given()
                .header("Authorization", "Bearer " + token)
                .post("/api/votes/" + singlePollId + "/publish")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("PUBLISHED"));
    }

    @Test
    @Order(31)
    void publishMultipleVote() {
        given()
                .header("Authorization", "Bearer " + token)
                .post("/api/votes/" + multiPollId + "/publish")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("PUBLISHED"));
    }

    @Test
    @Order(32)
    void publishScoredVote() {
        given()
                .header("Authorization", "Bearer " + token)
                .post("/api/votes/" + scoredPollId + "/publish")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("PUBLISHED"));
    }

    @Test
    @Order(33)
    void publishVote_forbidden() {
        given()
                .header("Authorization", "Bearer " + otherToken)
                .post("/api/votes/" + singlePollId + "/publish")
                .then()
                .statusCode(403);
    }

    // ==================== Get Published Vote by ShareId ====================

    @Test
    @Order(34)
    void getVote_byShareId_afterPublish() {
        Response response = given()
                .get("/api/votes/v/" + singleShareId);

        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.status", equalTo("PUBLISHED"));

        // Refresh option IDs (may have changed after update)
        singleOptionId1 = response.jsonPath().getLong("data.options[0].id");
        singleOptionId2 = response.jsonPath().getLong("data.options[1].id");
    }

    @Test
    @Order(35)
    void refreshMultiShareId() {
        Response response = given()
                .get("/api/votes/v/" + multiShareId);

        response.then().statusCode(200);

        multiOptionId1 = response.jsonPath().getLong("data.options[0].id");
        multiOptionId2 = response.jsonPath().getLong("data.options[1].id");
        multiOptionId3 = response.jsonPath().getLong("data.options[2].id");
    }

    @Test
    @Order(36)
    void refreshScoredShareId() {
        Response response = given()
                .get("/api/votes/v/" + scoredShareId);

        response.then().statusCode(200);

        scoredOptionId1 = response.jsonPath().getLong("data.options[0].id");
        scoredOptionId2 = response.jsonPath().getLong("data.options[1].id");
    }

    // ==================== Submit Votes ====================

    @Test
    @Order(40)
    void submitSingleVote_success() {
        Map<String, Object> body = new HashMap<>();
        body.put("optionIds", List.of(singleOptionId1));
        body.put("deviceId", "test-device-single-1");

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/votes/v/" + singleShareId + "/submit")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.totalVoteCount", greaterThanOrEqualTo(1))
                .body("data.hasVoted", is(true));
    }

    @Test
    @Order(41)
    void submitSingleVote_multipleOptions_fail() {
        Map<String, Object> body = new HashMap<>();
        body.put("optionIds", List.of(singleOptionId1, singleOptionId2));
        body.put("deviceId", "test-device-single-multi-1");

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/votes/v/" + singleShareId + "/submit")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(42)
    void submitSingleVote_empty_fail() {
        Map<String, Object> body = new HashMap<>();
        body.put("optionIds", List.of());
        body.put("deviceId", "test-device-empty-1");

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/votes/v/" + singleShareId + "/submit")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(50)
    void submitMultipleVote_success() {
        Map<String, Object> body = new HashMap<>();
        body.put("optionIds", List.of(multiOptionId1, multiOptionId2));
        body.put("deviceId", "test-device-multi-1");

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/votes/v/" + multiShareId + "/submit")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.totalVoteCount", greaterThanOrEqualTo(2));
    }

    @Test
    @Order(51)
    void submitMultipleVote_tooManyOptions_fail() {
        // maxOptions is 2, trying to select 3
        Map<String, Object> body = new HashMap<>();
        body.put("optionIds", List.of(multiOptionId1, multiOptionId2, multiOptionId3));
        body.put("deviceId", "test-device-multi-exceed-1");

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/votes/v/" + multiShareId + "/submit")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(60)
    void submitScoredVote_success() {
        Map<String, Object> votes = new HashMap<>();
        votes.put(String.valueOf(scoredOptionId1), 3);
        votes.put(String.valueOf(scoredOptionId2), 5);

        Map<String, Object> body = new HashMap<>();
        body.put("votes", votes);
        body.put("deviceId", "test-device-scored-1");

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/votes/v/" + scoredShareId + "/submit")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.totalVoteCount", greaterThanOrEqualTo(8));
    }

    @Test
    @Order(61)
    void submitScoredVote_exceedPerOption_fail() {
        // maxVotesPerOption is 5, trying 6
        Map<String, Object> votes = new HashMap<>();
        votes.put(String.valueOf(scoredOptionId1), 6);

        Map<String, Object> body = new HashMap<>();
        body.put("votes", votes);
        body.put("deviceId", "test-device-scored-exceed-1");

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/votes/v/" + scoredShareId + "/submit")
                .then()
                .statusCode(400);
    }

    // ==================== My Votes / Public Votes ====================

    @Test
    @Order(70)
    void getMyVotes_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/votes/my")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.totalElements", greaterThanOrEqualTo(3));
    }

    @Test
    @Order(71)
    void getPublicVotes() {
        given()
                .get("/api/votes/public")
                .then()
                .statusCode(200)
                .body("success", is(true));
    }

    // ==================== Close & Delete ====================

    @Test
    @Order(80)
    void closeVote_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .post("/api/votes/" + singlePollId + "/close")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("CLOSED"));
    }

    @Test
    @Order(81)
    void closeVote_forbidden() {
        given()
                .header("Authorization", "Bearer " + otherToken)
                .post("/api/votes/" + multiPollId + "/close")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(90)
    void deleteVote_forbidden() {
        given()
                .header("Authorization", "Bearer " + otherToken)
                .delete("/api/votes/" + singlePollId)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(91)
    void deleteVote_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .delete("/api/votes/" + singlePollId)
                .then()
                .statusCode(200)
                .body("success", is(true));
    }

    @Test
    @Order(92)
    void getVote_afterDelete() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/votes/" + singlePollId)
                .then()
                .statusCode(404);
    }

    // Cleanup remaining polls
    @Test
    @Order(99)
    void deleteRemainingPolls() {
        given()
                .header("Authorization", "Bearer " + token)
                .delete("/api/votes/" + multiPollId)
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .delete("/api/votes/" + scoredPollId)
                .then()
                .statusCode(200);
    }

    // ==================== Helper ====================

    private Map<String, Object> buildVoteRequest(String title, String voteType, String frequency) {
        Map<String, Object> option1 = new HashMap<>();
        option1.put("title", "选项A");
        option1.put("content", "选项A描述");
        option1.put("sortOrder", 0);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("title", "选项B");
        option2.put("content", "选项B描述");
        option2.put("sortOrder", 1);

        Map<String, Object> option3 = new HashMap<>();
        option3.put("title", "选项C");
        option3.put("sortOrder", 2);

        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("description", "测试投票描述");
        body.put("voteType", voteType);
        body.put("frequency", frequency);
        body.put("accessLevel", "PUBLIC");
        body.put("anonymous", true);
        body.put("options", List.of(option1, option2, option3));
        return body;
    }
}
