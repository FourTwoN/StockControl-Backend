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
class StockMovementFilterTest {

    private static final String TENANT = "tenant-smf";

    private static String productId;
    private static String batchId;
    private static String entradaMovementId;
    private static String ajusteMovementId;

    @Test
    @Order(1)
    void setup_createProductAndBatch() {
        productId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "SMF-PROD", "name": "Movement Filter Product"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .extract().path("id");

        batchId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "batchCode": "SMF-BATCH",
                            "quantity": 500,
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
    @Order(2)
    void setup_createMovements() {
        entradaMovementId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "movementType": "ENTRADA",
                            "quantity": 100,
                            "unit": "units",
                            "notes": "Initial entry",
                            "batchQuantities": [{"batchId": "%s", "quantity": 100}]
                        }
                        """.formatted(batchId))
                .when()
                .post("/api/v1/stock-movements")
                .then()
                .statusCode(201)
                .extract().path("id");

        ajusteMovementId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "movementType": "AJUSTE",
                            "quantity": 50,
                            "unit": "units",
                            "notes": "Stock adjustment",
                            "batchQuantities": [{"batchId": "%s", "quantity": 50}]
                        }
                        """.formatted(batchId))
                .when()
                .post("/api/v1/stock-movements")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(3)
    void filterByType_shouldReturnMatchingOnly() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("type", "ENTRADA")
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].movementType", equalTo("ENTRADA"));
    }

    @Test
    @Order(4)
    void filterByTypeAjuste_shouldReturnAjusteOnly() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("type", "AJUSTE")
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].movementType", equalTo("AJUSTE"));
    }

    @Test
    @Order(5)
    void filterByDateRange_shouldReturnMovementsInRange() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("startDate", "2020-01-01T00:00:00Z")
                .queryParam("endDate", "2030-12-31T23:59:59Z")
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    @Test
    @Order(6)
    void filterByFutureDateRange_shouldReturnEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("startDate", "2090-01-01T00:00:00Z")
                .queryParam("endDate", "2091-01-01T00:00:00Z")
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }

    @Test
    @Order(7)
    void filterByTypeAndDate_shouldCombineFilters() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("type", "ENTRADA")
                .queryParam("startDate", "2020-01-01T00:00:00Z")
                .queryParam("endDate", "2030-12-31T23:59:59Z")
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].movementType", equalTo("ENTRADA"));
    }

    @Test
    @Order(8)
    void noFilters_shouldReturnAll() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    @Test
    @Order(9)
    void filterByTypeNoMatch_shouldReturnEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("type", "VENTA")
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }
}
