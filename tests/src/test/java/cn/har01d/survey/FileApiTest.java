package cn.har01d.survey;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileApiTest extends BaseApiTest {

    private String token;
    private String uploadedFileName;

    @BeforeAll
    void setup() {
        String username = "fileuser_" + UUID.randomUUID().toString().substring(0, 8);
        token = registerAndGetToken(username, "Test123456");
    }

    // ==================== Upload ====================

    @Test
    @Order(1)
    void upload_success() {
        String fileName = given()
                .header("Authorization", "Bearer " + token)
                .multiPart("file", "test.txt", "Hello World".getBytes(), "text/plain")
                .post("/api/files/upload")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.url", startsWith("/api/files/"))
                .body("data.name", equalTo("test.txt"))
                .extract()
                .jsonPath().getString("data.url");

        // Extract filename from URL for download test
        uploadedFileName = fileName.substring(fileName.lastIndexOf("/") + 1);
    }

    @Test
    @Order(2)
    void upload_unauthorized() {
        given()
                .multiPart("file", "test.txt", "Hello World".getBytes(), "text/plain")
                .post("/api/files/upload")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(3)
    void upload_emptyFile() {
        given()
                .header("Authorization", "Bearer " + token)
                .multiPart("file", "empty.txt", new byte[0], "text/plain")
                .post("/api/files/upload")
                .then()
                .statusCode(400)
                .body("success", is(false));
    }

    // ==================== Download ====================

    @Test
    @Order(10)
    void download_success() {
        given()
                .get("/api/files/" + uploadedFileName)
                .then()
                .statusCode(200)
                .header("Content-Disposition", containsString(uploadedFileName));
    }

    @Test
    @Order(11)
    void download_notFound() {
        given()
                .get("/api/files/nonexistent-file-12345.txt")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(12)
    void download_noAuthRequired() {
        // File download should work without authentication
        given()
                .get("/api/files/" + uploadedFileName)
                .then()
                .statusCode(200);
    }
}
