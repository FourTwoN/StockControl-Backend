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
class UserControllerTest {

    private static final String TENANT = "tenant-user";

    private static String createdUserId;

    @Test
    @Order(1)
    void createUser_shouldReturn201() {
        createdUserId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "test@example.com", "name": "Test User", "role": "WORKER"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("email", equalTo("test@example.com"))
                .body("name", equalTo("Test User"))
                .body("role", equalTo("WORKER"))
                .body("active", equalTo(true))
                .extract().path("id");
    }

    @Test
    @Order(2)
    void getUser_shouldReturnCreated() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/users/" + createdUserId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdUserId))
                .body("email", equalTo("test@example.com"))
                .body("name", equalTo("Test User"))
                .body("createdAt", notNullValue());
    }

    @Test
    @Order(3)
    void listUsers_shouldReturnPaged() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/users")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20))
                .body("totalElements", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(4)
    void updateUser_shouldModify() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Updated User", "role": "SUPERVISOR"}
                        """)
                .when()
                .put("/api/v1/users/" + createdUserId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated User"))
                .body("role", equalTo("SUPERVISOR"));
    }

    @Test
    @Order(5)
    void getMe_shouldReturnValidResponse() {
        // /me endpoint uses JWT externalId to find or create user.
        // With @TestSecurity user "test-user", this should return 200
        // (findOrCreateFromExternalId will create if not found).
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/users/me")
                .then()
                .statusCode(200)
                .body("id", notNullValue());
    }

    @Test
    @Order(6)
    void deleteUser_shouldReturn204() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .delete("/api/v1/users/" + createdUserId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(7)
    void createUser_missingRequired_shouldReturn400() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"role": "ADMIN"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(8)
    void getUser_nonExistent_shouldReturn404() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/users/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }
}
