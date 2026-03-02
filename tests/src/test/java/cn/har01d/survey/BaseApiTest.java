package cn.har01d.survey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class BaseApiTest {

    protected static final String username = "test-user" ;
    protected static final String otherUsername = "other-user";
    protected static final String password = "Test$123456";

    protected static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeAll
    static void setupBase() {
        String baseUrl = System.getenv().getOrDefault("BASE_URL", "http://localhost:8080");
        RestAssured.baseURI = baseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Login and return the JWT token. If login fails, register the user first.
     */
    protected String loginOrRegisterAndGetToken(String username, String password) {
        // Try login first
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/auth/login");

        // If login fails (401), register the user
        if (response.getStatusCode() == 401) {
            response = given()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .post("/api/auth/register");
        }

        String token = response.jsonPath().getString("data.token");
        assertNotNull(token, "Login failed!");
        return token;
    }

    /**
     * Register a new user and return the JWT token.
     */
    protected String registerAndGetToken(String username, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/auth/register");

        return response.jsonPath().getString("data.token");
    }

    /**
     * Login and return the JWT token.
     */
    protected String loginAndGetToken(String username, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/auth/login");

        return response.jsonPath().getString("data.token");
    }

    /**
     * Parse response body as JsonNode.
     */
    protected JsonNode parseJson(Response response) throws Exception {
        return objectMapper.readTree(response.asString());
    }
}
