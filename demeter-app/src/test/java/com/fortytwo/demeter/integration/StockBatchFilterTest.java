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
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class StockBatchFilterTest {

    private static final String TENANT = "tenant-sb-filter";

    private static String productIdA;
    private static String productIdB;
    private static String batchIdA;
    private static String batchIdB;

    @Test
    @Order(1)
    void setup_createProducts() {
        productIdA = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "SBF-PROD-A", "name": "Filter Product A"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .extract().path("id");

        productIdB = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "SBF-PROD-B", "name": "Filter Product B"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(2)
    void setup_createBatches() {
        batchIdA = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "batchCode": "SBF-BATCH-A",
                            "quantity": 100,
                            "unit": "kg"
                        }
                        """.formatted(productIdA))
                .when()
                .post("/api/v1/stock-batches")
                .then()
                .statusCode(201)
                .extract().path("id");

        batchIdB = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "batchCode": "SBF-BATCH-B",
                            "quantity": 50,
                            "unit": "lbs"
                        }
                        """.formatted(productIdB))
                .when()
                .post("/api/v1/stock-batches")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(3)
    void setup_depleteBatchB() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"status": "DEPLETED"}
                        """)
                .when()
                .put("/api/v1/stock-batches/" + batchIdB)
                .then()
                .statusCode(200)
                .body("status", equalTo("DEPLETED"));
    }

    @Test
    @Order(4)
    void filterByProductId_shouldReturnOnlyMatchingBatches() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("productId", productIdA)
                .when()
                .get("/api/v1/stock-batches")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].productId", equalTo(productIdA))
                .body("content[0].batchCode", equalTo("SBF-BATCH-A"));
    }

    @Test
    @Order(5)
    void filterByStatus_shouldReturnOnlyMatchingStatus() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("status", "ACTIVE")
                .when()
                .get("/api/v1/stock-batches")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].status", equalTo("ACTIVE"));
    }

    @Test
    @Order(6)
    void filterByStatusDepleted_shouldReturnDepletedOnly() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("status", "DEPLETED")
                .when()
                .get("/api/v1/stock-batches")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].status", equalTo("DEPLETED"))
                .body("content[0].batchCode", equalTo("SBF-BATCH-B"));
    }

    @Test
    @Order(7)
    void filterByProductIdAndStatus_shouldCombineFilters() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("productId", productIdA)
                .queryParam("status", "ACTIVE")
                .when()
                .get("/api/v1/stock-batches")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].productId", equalTo(productIdA))
                .body("content[0].status", equalTo("ACTIVE"));
    }

    @Test
    @Order(8)
    void filterByProductIdAndWrongStatus_shouldReturnEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("productId", productIdA)
                .queryParam("status", "DEPLETED")
                .when()
                .get("/api/v1/stock-batches")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }

    @Test
    @Order(9)
    void noFilters_shouldReturnAll() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-batches")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }
}
