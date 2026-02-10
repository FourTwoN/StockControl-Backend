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
class StockMovementControllerTest {

    private static final String TENANT = "tenant-stock-mvmt";

    private static String productId;
    private static String batchId;
    private static String movementId;

    @Test
    @Order(1)
    void setup_createProduct() {
        productId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "MVMT-PROD-001", "name": "Movement Test Product"}
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
                            "batchCode": "MVMT-BATCH-001",
                            "quantity": 200,
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
    void createEntradaMovement_shouldReturn201() {
        movementId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "movementType": "ENTRADA",
                            "quantity": 50,
                            "unit": "kg",
                            "notes": "Initial stock",
                            "batchQuantities": [
                                {
                                    "batchId": "%s",
                                    "quantity": 50
                                }
                            ]
                        }
                        """.formatted(batchId))
                .when()
                .post("/api/v1/stock-movements")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("movementType", equalTo("ENTRADA"))
                .body("quantity", equalTo(50))
                .body("unit", equalTo("kg"))
                .extract().path("id");
    }

    @Test
    @Order(4)
    void getMovement_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-movements/" + movementId)
                .then()
                .statusCode(200)
                .body("id", equalTo(movementId))
                .body("movementType", equalTo("ENTRADA"))
                .body("notes", equalTo("Initial stock"));
    }

    @Test
    @Order(5)
    void listMovements_shouldReturnPaged() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    @Test
    @Order(6)
    void getByType_shouldReturn() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-movements/by-type/ENTRADA")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(7)
    void getByDateRange_shouldReturn() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("from", "2020-01-01T00:00:00Z")
                .queryParam("to", "2030-01-01T00:00:00Z")
                .when()
                .get("/api/v1/stock-movements/by-date-range")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(8)
    void getByReference_shouldReturnEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-movements/by-reference/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @Order(9)
    void createMovement_missingRequired_shouldReturn400() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "notes": "Missing fields"
                        }
                        """)
                .when()
                .post("/api/v1/stock-movements")
                .then()
                .statusCode(400);
    }
}
