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
class ChatMessageControllerTest {

    private static final String TENANT = "tenant-chat-msg";

    private static String userId;
    private static String sessionId;
    private static String messageId;
    private static String toolExecId;

    @Test
    @Order(1)
    void setup_createUser() {
        userId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "chatmsg@example.com", "name": "Chat Msg User"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(2)
    void setup_createSession() {
        sessionId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "userId": "%s",
                            "title": "Message Test Session"
                        }
                        """.formatted(userId))
                .when()
                .post("/api/v1/chat/sessions")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(3)
    void setup_createMessage() {
        messageId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "role": "ASSISTANT",
                            "content": "Processing..."
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
    @Order(4)
    void getMessage_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/chat/messages/" + messageId)
                .then()
                .statusCode(200)
                .body("id", equalTo(messageId));
    }

    @Test
    @Order(5)
    void listToolExecutions_shouldReturnEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/chat/messages/" + messageId + "/tool-executions")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @Order(6)
    void addToolExecution_shouldReturn201() {
        toolExecId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "toolName": "search",
                            "input": {"query": "test"}
                        }
                        """)
                .when()
                .post("/api/v1/chat/messages/" + messageId + "/tool-executions")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(7)
    void listToolExecutions_shouldReturn() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/chat/messages/" + messageId + "/tool-executions")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(8)
    void getMessage_nonExistent_shouldReturn404() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/chat/messages/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }
}
