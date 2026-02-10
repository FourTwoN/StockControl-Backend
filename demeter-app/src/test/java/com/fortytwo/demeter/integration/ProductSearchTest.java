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

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class ProductSearchTest {

    private static final String TENANT = "tenant-prod-search";

    @Test
    @Order(1)
    void setup_createProducts() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "SRCH-ALPHA-001", "name": "Alpha Widget"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201);

        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "SRCH-BETA-002", "name": "Beta Gadget"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201);

        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "SRCH-GAMMA-003", "name": "Gamma Widget Plus"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(2)
    void searchByName_shouldFilterResults() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("search", "Widget")
                .when()
                .get("/api/v1/products")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("totalElements", equalTo(2));
    }

    @Test
    @Order(3)
    void searchBySku_shouldFilterResults() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("search", "BETA")
                .when()
                .get("/api/v1/products")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].name", equalTo("Beta Gadget"));
    }

    @Test
    @Order(4)
    void searchCaseInsensitive_shouldWork() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("search", "widget")
                .when()
                .get("/api/v1/products")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2));
    }

    @Test
    @Order(5)
    void searchNoMatch_shouldReturnEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("search", "nonexistent-xyz")
                .when()
                .get("/api/v1/products")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0))
                .body("totalElements", equalTo(0));
    }

    @Test
    @Order(6)
    void searchEmpty_shouldReturnAll() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/products")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(3))
                .body("totalElements", greaterThanOrEqualTo(3));
    }

    @Test
    @Order(7)
    void searchWithPagination_shouldRespectPageSize() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("search", "Widget")
                .queryParam("page", 0)
                .queryParam("size", 1)
                .when()
                .get("/api/v1/products")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("totalElements", equalTo(2))
                .body("page", equalTo(0))
                .body("size", equalTo(1));
    }
}
