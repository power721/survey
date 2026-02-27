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
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthApiTest extends BaseApiTest {

    private final String username = "authtest_" + UUID.randomUUID().toString().substring(0, 8);
    private final String password = "Test123456";
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

        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.token", notNullValue())
                .body("data.username", equalTo(username))
                .body("data.nickname", equalTo("测试用户"))
                .body("data.role", equalTo("USER"));

        token = response.jsonPath().getString("data.token");
        assertNotNull(token);
    }

    @Test
    @Order(2)
    void register_duplicateUsername() {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/auth/register")
                .then()
                .statusCode(409)
                .body("success", is(false));
    }

    @Test
    @Order(3)
    void register_missingUsername() {
        Map<String, String> body = new HashMap<>();
        body.put("password", password);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/auth/register")
                .then()
                .statusCode(400)
                .body("success", is(false));
    }

    @Test
    @Order(4)
    void register_shortPassword() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "short_pw_user_" + UUID.randomUUID().toString().substring(0, 6));
        body.put("password", "12");

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/auth/register")
                .then()
                .statusCode(400)
                .body("success", is(false));
    }

    // ==================== Login ====================

    @Test
    @Order(10)
    void login_success() {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/auth/login");

        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.token", notNullValue())
                .body("data.username", equalTo(username));

        token = response.jsonPath().getString("data.token");
    }

    @Test
    @Order(11)
    void login_wrongPassword() {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", "wrong_password");

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("success", is(false));
    }

    @Test
    @Order(12)
    void login_nonExistentUser() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "nonexistent_user_xyz");
        body.put("password", password);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("success", is(false));
    }

    // ==================== Profile ====================

    @Test
    @Order(20)
    void getProfile_success() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/auth/profile")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.username", equalTo(username))
                .body("data.nickname", equalTo("测试用户"))
                .body("data.email", equalTo(username + "@test.com"))
                .body("data.role", equalTo("USER"));
    }

    @Test
    @Order(21)
    void getProfile_noToken() {
        given()
                .get("/api/auth/profile")
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(30)
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
    @Order(31)
    void updateProfile_email() {
        String newEmail = "newemail_" + UUID.randomUUID().toString().substring(0, 6) + "@test.com";
        Map<String, String> body = new HashMap<>();
        body.put("email", newEmail);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/auth/profile")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.email", equalTo(newEmail));
    }

    @Test
    @Order(32)
    void updateProfile_changePassword() {
        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", password);
        body.put("newPassword", "NewPass789");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/auth/profile")
                .then()
                .statusCode(200)
                .body("success", is(true));

        // Verify new password works
        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("username", username);
        loginBody.put("password", "NewPass789");

        given()
                .contentType(ContentType.JSON)
                .body(loginBody)
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("success", is(true));
    }

    @Test
    @Order(33)
    void updateProfile_wrongOldPassword() {
        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", "totally_wrong");
        body.put("newPassword", "AnotherPass123");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/auth/profile")
                .then()
                .statusCode(400)
                .body("success", is(false));
    }
}
