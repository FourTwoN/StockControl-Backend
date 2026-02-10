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
class PhotoSessionControllerTest {

    private static final String TENANT = "tenant-photo";

    private static String sessionId;

    @Test
    @Order(1)
    void createSession_shouldReturn201() {
        sessionId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/v1/photo-sessions")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("status", equalTo("PENDING"))
                .extract().path("id");
    }

    @Test
    @Order(2)
    void getSession_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/photo-sessions/" + sessionId)
                .then()
                .statusCode(200)
                .body("id", equalTo(sessionId))
                .body("status", equalTo("PENDING"));
    }

    @Test
    @Order(3)
    void getSessionStatus_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/photo-sessions/" + sessionId + "/status")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    @Order(4)
    void listSessions_shouldReturnPaged() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/photo-sessions")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    @Test
    @Order(5)
    void getSessionImages_shouldReturnEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/photo-sessions/" + sessionId + "/images")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @Order(6)
    void getSessionEstimations_shouldReturnEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/photo-sessions/" + sessionId + "/estimations")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @Order(7)
    void deleteSession_shouldReturn204() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .delete("/api/v1/photo-sessions/" + sessionId)
                .then()
                .statusCode(204);

        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/photo-sessions/" + sessionId)
                .then()
                .statusCode(404);
    }
}
