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
class FamilyControllerTest {

    private static final String TENANT = "tenant-family";

    private static String createdFamilyId;

    @Test
    @Order(1)
    void createFamily_shouldReturn201() {
        createdFamilyId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Test Family", "description": "Family desc"}
                        """)
                .when()
                .post("/api/v1/families")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Test Family"))
                .body("description", equalTo("Family desc"))
                .extract().path("id");
    }

    @Test
    @Order(2)
    void getFamily_shouldReturnCreated() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/families/" + createdFamilyId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdFamilyId))
                .body("name", equalTo("Test Family"))
                .body("description", equalTo("Family desc"))
                .body("createdAt", notNullValue());
    }

    @Test
    @Order(3)
    void listFamilies_shouldReturnPaged() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/families")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20))
                .body("totalElements", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(4)
    void updateFamily_shouldModify() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Updated Family", "description": "Updated desc"}
                        """)
                .when()
                .put("/api/v1/families/" + createdFamilyId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Family"))
                .body("description", equalTo("Updated desc"));
    }

    @Test
    @Order(5)
    void deleteFamily_shouldReturn204() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .delete("/api/v1/families/" + createdFamilyId)
                .then()
                .statusCode(204);

        // Verify it is gone
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/families/" + createdFamilyId)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(6)
    void createFamily_missingName_shouldReturn400() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"description": "No name"}
                        """)
                .when()
                .post("/api/v1/families")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(7)
    void getFamily_nonExistent_shouldReturn404() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/families/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }
}
