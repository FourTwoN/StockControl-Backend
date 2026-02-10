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
class CostControllerTest {

    private static final String TENANT = "tenant-cost";

    private static String productId;
    private static String batchId;
    private static String costId;

    @Test
    @Order(1)
    void setup_createProduct() {
        productId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "COST-PROD", "name": "Cost Test Product"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(2)
    void setup_createBatch() {
        batchId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "batchCode": "COST-BATCH",
                            "quantity": 100,
                            "unit": "kg"
                        }
                        """.formatted(productId))
                .when()
                .post("/api/v1/stock-batches")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(3)
    void createCost_shouldReturn201() {
        costId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "batchId": "%s",
                            "costType": "MATERIAL",
                            "amount": 25.50,
                            "currency": "USD",
                            "description": "Raw materials",
                            "effectiveDate": "2024-01-15"
                        }
                        """.formatted(productId, batchId))
                .when()
                .post("/api/v1/costs")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("costType", equalTo("MATERIAL"))
                .body("amount", equalTo(25.5f))
                .body("currency", equalTo("USD"))
                .body("description", equalTo("Raw materials"))
                .extract().path("id");
    }

    @Test
    @Order(4)
    void getCost_shouldReturnCreated() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/costs/" + costId)
                .then()
                .statusCode(200)
                .body("id", equalTo(costId))
                .body("costType", equalTo("MATERIAL"))
                .body("amount", equalTo(25.5f))
                .body("productId", equalTo(productId));
    }

    @Test
    @Order(5)
    void listCosts_shouldReturnPaged() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/costs")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    @Test
    @Order(6)
    void getCostsByProduct_shouldReturn() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/costs/by-product/" + productId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(7)
    void getCostsByBatch_shouldReturn() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/costs/by-batch/" + batchId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(8)
    void updateCost_shouldModify() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "costType": "LABOR",
                            "amount": 30.00,
                            "description": "Updated"
                        }
                        """)
                .when()
                .put("/api/v1/costs/" + costId)
                .then()
                .statusCode(200)
                .body("costType", equalTo("LABOR"))
                .body("amount", equalTo(30))
                .body("description", equalTo("Updated"));
    }

    @Test
    @Order(9)
    void deleteCost_shouldReturn204() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .delete("/api/v1/costs/" + costId)
                .then()
                .statusCode(204);

        // Verify it is gone
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/costs/" + costId)
                .then()
                .statusCode(404);
    }
}
