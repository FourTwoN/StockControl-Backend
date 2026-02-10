package com.fortytwo.demeter.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class ChatSessionControllerTest {

    private static final String TENANT = "tenant-chat";

    private static String userId;
    private static String sessionId;
    private static String messageId;

    @Test
    @Order(1)
    void setup_createUser() {
        userId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "chat@example.com", "name": "Chat User"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(2)
    void createSession_shouldReturn201() {
        sessionId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "userId": "%s",
                            "title": "Test Chat"
                        }
                        """.formatted(userId))
                .when()
                .post("/api/v1/chat/sessions")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(3)
    void getSession_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/chat/sessions/" + sessionId)
                .then()
                .statusCode(200)
                .body("id", equalTo(sessionId));
    }

    @Test
    @Order(4)
    void listSessions_shouldReturnPaged() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/chat/sessions")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    @Test
    @Order(5)
    void addMessage_shouldReturn201() {
        messageId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "role": "USER",
                            "content": "Hello AI"
                        }
                        """)
                .when()
                .post("/api/v1/chat/sessions/" + sessionId + "/messages")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(6)
    void listMessages_shouldReturn() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/chat/sessions/" + sessionId + "/messages")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(7)
    void closeSession_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v1/chat/sessions/" + sessionId + "/close")
                .then()
                .statusCode(200)
                .body("active", equalTo(false));
    }

    @Test
    @Order(8)
    void deleteSession_shouldReturn204() {
        // Create a new session to delete
        String deleteSessionId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "userId": "%s",
                            "title": "Session to Delete"
                        }
                        """.formatted(userId))
                .when()
                .post("/api/v1/chat/sessions")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .delete("/api/v1/chat/sessions/" + deleteSessionId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(9)
    void createSession_missingUserId_shouldReturn400() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"title": "No user"}
                        """)
                .when()
                .post("/api/v1/chat/sessions")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(10)
    void getSession_nonExistent_shouldReturn404() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/chat/sessions/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }
}
