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
class ImageControllerTest {

    private static final String TENANT = "tenant-image";

    @Test
    @Order(1)
    void getImage_nonExistent_shouldReturn404() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/images/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(2)
    void getDetections_nonExistent_shouldReturn404() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/images/00000000-0000-0000-0000-000000000000/detections")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(3)
    void getClassifications_nonExistent_shouldReturn404() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/images/00000000-0000-0000-0000-000000000000/classifications")
                .then()
                .statusCode(404);
    }
}
