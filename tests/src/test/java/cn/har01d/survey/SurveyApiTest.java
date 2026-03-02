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
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SurveyApiTest extends BaseApiTest {

    private String token;
    private String otherToken;
    private Long surveyId;
    private String shareId;
    private Long questionId;
    private Long optionId1;
    private Long optionId2;

    @BeforeAll
    void setup() {
        token = loginOrRegisterAndGetToken(username, password);
        otherToken = loginOrRegisterAndGetToken(otherUsername, password);
    }

    // ==================== Create Survey ====================

    @Test
    @Order(1)
    void createSurvey_success() {
        Map<String, Object> body = buildSurveyRequest("测试问卷", "PUBLIC");

        Response response = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/surveys");

        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.title", equalTo("测试问卷"))
                .body("data.status", equalTo("DRAFT"))
                .body("data.shareId", notNullValue())
                .body("data.questions", hasSize(2));

        surveyId = response.jsonPath().getLong("data.id");
        shareId = response.jsonPath().getString("data.shareId");
        questionId = response.jsonPath().getLong("data.questions[0].id");
        optionId1 = response.jsonPath().getLong("data.questions[0].options[0].id");
        optionId2 = response.jsonPath().getLong("data.questions[0].options[1].id");
        assertNotNull(surveyId);
    }

    @Test
    @Order(2)
    void createSurvey_unauthorized() {
        Map<String, Object> body = buildSurveyRequest("无权限问卷", "PUBLIC");

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/surveys")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(3)
    void createSurvey_missingTitle() {
        Map<String, Object> body = buildSurveyRequest("", "PUBLIC");
        body.put("title", "");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/surveys")
                .then()
                .statusCode(400);
    }

    // ==================== Get Survey ====================

    @Test
    @Order(10)
    void getSurvey_byId_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/surveys/" + surveyId)
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.id", equalTo(surveyId.intValue()))
                .body("data.title", equalTo("测试问卷"));
    }

    @Test
    @Order(11)
    void getSurvey_byId_forbidden() {
        given()
                .header("Authorization", "Bearer " + otherToken)
                .get("/api/surveys/" + surveyId)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(12)
    void getSurvey_byId_notFound() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/surveys/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(13)
    void getSurvey_byShareId_draftNotAccessible() {
        // Survey is still DRAFT, so shareId access should fail
        given()
                .get("/api/surveys/s/" + shareId)
                .then()
                .statusCode(400);
    }

    // ==================== Update Survey ====================

    @Test
    @Order(20)
    void updateSurvey_success() {
        Map<String, Object> body = buildSurveyRequest("更新后的问卷", "PUBLIC");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/surveys/" + surveyId)
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.title", equalTo("更新后的问卷"));
    }

    @Test
    @Order(21)
    void updateSurvey_forbidden() {
        Map<String, Object> body = buildSurveyRequest("非法更新", "PUBLIC");

        given()
                .header("Authorization", "Bearer " + otherToken)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/surveys/" + surveyId)
                .then()
                .statusCode(403);
    }

    // ==================== Publish / Close ====================

    @Test
    @Order(30)
    void publishSurvey_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .post("/api/surveys/" + surveyId + "/publish")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("PUBLISHED"));
    }

    @Test
    @Order(31)
    void getSurvey_byShareId_afterPublish() {
        Response response = given()
                .get("/api/surveys/s/" + shareId);

        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.title", equalTo("更新后的问卷"))
                .body("data.status", equalTo("PUBLISHED"));

        // Refresh question and option IDs from published survey
        questionId = response.jsonPath().getLong("data.questions[0].id");
        optionId1 = response.jsonPath().getLong("data.questions[0].options[0].id");
        optionId2 = response.jsonPath().getLong("data.questions[0].options[1].id");
    }

    // ==================== Submit Survey ====================

    @Test
    @Order(40)
    void submitSurvey_success() {
        Map<String, Object> answer1 = new HashMap<>();
        answer1.put("questionId", questionId);
        answer1.put("selectedOptionId", optionId1);

        Map<String, Object> body = new HashMap<>();
        body.put("answers", List.of(answer1));

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/surveys/s/" + shareId + "/submit")
                .then()
                .statusCode(200)
                .body("success", is(true));
    }

    @Test
    @Order(41)
    void submitSurvey_emptyAnswers() {
        Map<String, Object> body = new HashMap<>();
        body.put("answers", List.of());

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/surveys/s/" + shareId + "/submit")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(42)
    void submitSurvey_secondSubmission() {
        // Submit a second time with different option
        // Survey may not allow multiple submissions by default
        Map<String, Object> answer1 = new HashMap<>();
        answer1.put("questionId", questionId);
        answer1.put("selectedOptionId", optionId2);

        Map<String, Object> body = new HashMap<>();
        body.put("answers", List.of(answer1));

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/surveys/s/" + shareId + "/submit")
                .then()
                .statusCode(400)  // Survey doesn't allow duplicate submissions
                .body("success", is(false));
    }

    // ==================== Responses & Stats ====================

    @Test
    @Order(50)
    void getResponses_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/surveys/" + surveyId + "/responses")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.totalElements", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(51)
    void getResponses_forbidden() {
        given()
                .header("Authorization", "Bearer " + otherToken)
                .get("/api/surveys/" + surveyId + "/responses")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(52)
    void getStats_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/surveys/" + surveyId + "/stats")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.totalResponses", greaterThanOrEqualTo(1))
                .body("data.questionStats", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(53)
    void exportExcel_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/surveys/" + surveyId + "/export")
                .then()
                .statusCode(200)
                .contentType(containsString("spreadsheetml"));
    }

    // ==================== My Surveys / Public Surveys ====================

    @Test
    @Order(60)
    void getMySurveys_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/surveys/my")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.totalElements", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(61)
    void getMySurveys_withKeyword() {
        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", "更新后")
                .get("/api/surveys/my")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.totalElements", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(62)
    void getPublicSurveys() {
        given()
                .get("/api/surveys/public")
                .then()
                .statusCode(200)
                .body("success", is(true));
    }

    @Test
    @Order(63)
    void getTemplates() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/surveys/templates")
                .then()
                .statusCode(200)
                .body("success", is(true));
    }

    // ==================== Close & Delete ====================

    @Test
    @Order(70)
    void closeSurvey_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .post("/api/surveys/" + surveyId + "/close")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("CLOSED"));
    }

    @Test
    @Order(71)
    void submitSurvey_afterClose() {
        Map<String, Object> answer1 = new HashMap<>();
        answer1.put("questionId", questionId);
        answer1.put("selectedOptionId", optionId1);

        Map<String, Object> body = new HashMap<>();
        body.put("answers", List.of(answer1));

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/surveys/s/" + shareId + "/submit")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(80)
    void deleteSurvey_forbidden() {
        given()
                .header("Authorization", "Bearer " + otherToken)
                .delete("/api/surveys/" + surveyId)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(81)
    void deleteSurvey_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .delete("/api/surveys/" + surveyId)
                .then()
                .statusCode(anyOf(is(200), is(204)))  // Accept both 200 and 204
                .body("success", is(true));
    }

    @Test
    @Order(82)
    void getSurvey_afterDelete() {
        // After deletion, survey might be soft-deleted (status = DELETED) or return 404
        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/surveys/" + surveyId)
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    // ==================== Helper ====================

    private Map<String, Object> buildSurveyRequest(String title, String accessLevel) {
        Map<String, Object> option1 = new HashMap<>();
        option1.put("content", "选项A");
        option1.put("sortOrder", 0);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("content", "选项B");
        option2.put("sortOrder", 1);

        Map<String, Object> question1 = new HashMap<>();
        question1.put("type", "SINGLE_CHOICE");
        question1.put("title", "单选题");
        question1.put("required", true);
        question1.put("sortOrder", 0);
        question1.put("options", List.of(option1, option2));

        Map<String, Object> question2 = new HashMap<>();
        question2.put("type", "TEXT");
        question2.put("title", "填空题");
        question2.put("required", false);
        question2.put("sortOrder", 1);
        question2.put("options", List.of());

        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("description", "测试描述");
        body.put("accessLevel", accessLevel);
        body.put("anonymous", true);
        body.put("template", false);
        body.put("questions", List.of(question1, question2));
        return body;
    }
}
