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
class CategoryControllerTest {

    private static final String TENANT = "tenant-category";

    private static String createdCategoryId;

    @Test
    @Order(1)
    void createCategory_shouldReturn201() {
        createdCategoryId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Test Category", "description": "Desc"}
                        """)
                .when()
                .post("/api/v1/categories")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Test Category"))
                .body("description", equalTo("Desc"))
                .extract().path("id");
    }

    @Test
    @Order(2)
    void getCategory_shouldReturnCreated() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/categories/" + createdCategoryId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdCategoryId))
                .body("name", equalTo("Test Category"))
                .body("description", equalTo("Desc"))
                .body("createdAt", notNullValue());
    }

    @Test
    @Order(3)
    void listCategories_shouldReturnPaged() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/categories")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20))
                .body("totalElements", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(4)
    void updateCategory_shouldModify() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Updated Category", "description": "Updated"}
                        """)
                .when()
                .put("/api/v1/categories/" + createdCategoryId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Category"))
                .body("description", equalTo("Updated"));
    }

    @Test
    @Order(5)
    void deleteCategory_shouldReturn204() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .delete("/api/v1/categories/" + createdCategoryId)
                .then()
                .statusCode(204);

        // Verify it is gone
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/categories/" + createdCategoryId)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(6)
    void createCategory_missingName_shouldReturn400() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"description": "No name"}
                        """)
                .when()
                .post("/api/v1/categories")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(7)
    void getCategory_nonExistent_shouldReturn404() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/categories/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }
}
