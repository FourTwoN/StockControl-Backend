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
class AnalyticsControllerTest {

    private static final String TENANT = "tenant-analytics";

    private static String productId;
    private static String batchId;
    private static String saleId;
    private static String costId;
    private static String warehouseId;

    @Test
    @Order(1)
    void setup_createProduct() {
        productId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "ANALYTICS-PROD-001", "name": "Analytics Product"}
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
                            "batchCode": "ANALYTICS-BATCH-001",
                            "quantity": 200,
                            "unit": "units"
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
    void setup_createAndCompleteSale() {
        saleId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "customerName": "Analytics Customer",
                            "items": [
                                {
                                    "productId": "%s",
                                    "quantity": 10,
                                    "unitPrice": 25.00
                                }
                            ]
                        }
                        """.formatted(productId))
                .when()
                .post("/api/v1/sales")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v1/sales/" + saleId + "/complete")
                .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"));
    }

    @Test
    @Order(4)
    void setup_createCost() {
        costId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "costType": "PURCHASE",
                            "amount": 15.00,
                            "currency": "USD",
                            "description": "Analytics cost",
                            "effectiveDate": "2024-01-15"
                        }
                        """.formatted(productId))
                .when()
                .post("/api/v1/costs")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(5)
    void setup_createWarehouse() {
        warehouseId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Analytics Warehouse",
                            "address": "123 Analytics St"
                        }
                        """)
                .when()
                .post("/api/v1/warehouses")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(6)
    void stockSummary_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/analytics/stock-summary")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    @Order(7)
    void movements_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("from", "2020-01-01T00:00:00Z")
                .queryParam("to", "2030-01-01T00:00:00Z")
                .when()
                .get("/api/v1/analytics/movements")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    @Order(8)
    void inventoryValuation_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/analytics/inventory-valuation")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    @Order(9)
    void topProducts_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("limit", 5)
                .when()
                .get("/api/v1/analytics/top-products")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    @Order(10)
    void locationOccupancy_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/analytics/location-occupancy")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    @Order(11)
    void dashboard_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/analytics/dashboard")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    @Order(12)
    void movementHistory_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/analytics/movement-history")
                .then()
                .statusCode(200)
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    @Test
    @Order(13)
    void movementHistoryFiltered_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .queryParam("type", "VENTA")
                .when()
                .get("/api/v1/analytics/movement-history")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }
}
