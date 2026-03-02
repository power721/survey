package cn.har01d.survey;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthApiTest extends BaseApiTest {

    private String token;

    // ==================== Register ====================

    @Test
    @Order(1)
    void register_success() {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("email", username + "@test.com");
        body.put("nickname", "测试用户");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/auth/register");

        response.then();
    }

    // ==================== Login ====================

    @Test
    @Order(5)
    void login_success() {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/auth/login");

        response
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.token", notNullValue())
                .body("data.username", equalTo(username));

        token = response.jsonPath().getString("data.token");
        assertNotNull(token);
    }

    @Test
    @Order(6)
    void login_wrongPassword() {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", "wrongpassword");

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("success", is(false));
    }

    @Test
    @Order(7)
    void login_nonExistentUser() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "nonexistentuser");
        body.put("password", password);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("success", is(false));
    }

    // ==================== Get Profile ====================

    @Test
    @Order(8)
    void getProfile_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/auth/profile")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.username", equalTo(username));
    }

    @Test
    @Order(9)
    void getProfile_noToken() {
        given()
                .get("/api/auth/profile")
                .then()
                .statusCode(401);
    }

    // ==================== Update Profile ====================

    @Test
    @Order(10)
    void updateProfile_nickname() {
        Map<String, String> body = new HashMap<>();
        body.put("nickname", "新昵称");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/auth/profile")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.nickname", equalTo("新昵称"));
    }

    @Test
    @Order(11)
    void updateProfile_email() {
        Map<String, String> body = new HashMap<>();
        body.put("email", "newEmail@test.com");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/auth/profile")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.email", equalTo("newEmail@test.com"));
    }

    @Test
    @Order(12)
    void updateProfile_revert_email() {
        Map<String, String> body = new HashMap<>();
        body.put("email", username + "@test.com");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/auth/profile")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.email", equalTo(username + "@test.com"));
    }

    @Test
    @Order(13)
    void updateProfile_changePassword() {
        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", password);
        body.put("newPassword", "NewPass123");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/auth/profile")
                .then()
                .statusCode(200)
                .body("success", is(true));
    }

    @Test
    @Order(14)
    void updateProfile_wrongOldPassword() {
        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", "wrongpassword");
        body.put("newPassword", "NewPass123");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/auth/profile")
                .then()
                .statusCode(400)
                .body("success", is(false));
    }

    @Test
    @Order(15)
    void updateProfile_revertPassword() {
        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", "NewPass123");
        body.put("newPassword", password);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/auth/profile")
                .then()
                .statusCode(200)
                .body("success", is(true));
    }
}
