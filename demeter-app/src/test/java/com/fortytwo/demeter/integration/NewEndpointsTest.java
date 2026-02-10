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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class NewEndpointsTest {

    private static final String TENANT = "tenant-new-ep";

    private static String productId;
    private static String batchId;
    private static String costId;
    private static String saleId;

    // --- Setup: create test data ---

    @Test
    @Order(1)
    void setup_createProduct() {
        productId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "NEP-PROD-001", "name": "New Endpoint Product"}
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
                            "batchCode": "NEP-BATCH-001",
                            "quantity": 300,
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
    void setup_createCost() {
        costId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "batchId": "%s",
                            "costType": "PURCHASE",
                            "amount": 12.50,
                            "currency": "USD",
                            "description": "Initial purchase cost",
                            "effectiveDate": "2024-03-15"
                        }
                        """.formatted(productId, batchId))
                .when()
                .post("/api/v1/costs")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create a second cost for trend data
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "costType": "PURCHASE",
                            "amount": 14.00,
                            "currency": "USD",
                            "description": "Updated purchase cost",
                            "effectiveDate": "2024-06-15"
                        }
                        """.formatted(productId))
                .when()
                .post("/api/v1/costs")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(4)
    void setup_createMovement() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "movementType": "ENTRADA",
                            "quantity": 100,
                            "unit": "units",
                            "notes": "Test stock entry",
                            "batchQuantities": [{"batchId": "%s", "quantity": 100}]
                        }
                        """.formatted(batchId))
                .when()
                .post("/api/v1/stock-movements")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(5)
    void setup_createAndCompleteSale() {
        saleId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "customerName": "New EP Customer",
                            "items": [
                                {
                                    "productId": "%s",
                                    "quantity": 5,
                                    "unitPrice": 30.00
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

    // --- Cost Analytics Endpoints (Phase 3) ---

    @Test
    @Order(10)
    void costProducts_shouldReturnAggregatedCosts() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/costs/products")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].productId", notNullValue())
                .body("content[0].productName", notNullValue())
                .body("content[0].averageCost", notNullValue())
                .body("content[0].lastCost", notNullValue())
                .body("content[0].currency", notNullValue())
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    @Test
    @Order(11)
    void costProducts_pagination_shouldWork() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 1)
                .when()
                .get("/api/v1/costs/products")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(1));
    }

    @Test
    @Order(12)
    void costValuation_shouldReturnSummary() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/costs/valuation")
                .then()
                .statusCode(200)
                .body("totalValue", notNullValue())
                .body("totalUnits", greaterThanOrEqualTo(1))
                .body("currency", equalTo("USD"));
    }

    @Test
    @Order(13)
    void costTrends_shouldReturnTrendData() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("productId", productId)
                .when()
                .get("/api/v1/costs/trends")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2))
                .body("[0].date", notNullValue())
                .body("[0].amount", notNullValue())
                .body("[0].costType", equalTo("PURCHASE"));
    }

    @Test
    @Order(14)
    void costTrends_withDateRange_shouldFilter() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("productId", productId)
                .queryParam("from", "2024-01-01")
                .queryParam("to", "2024-04-01")
                .when()
                .get("/api/v1/costs/trends")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].date", equalTo("2024-03-15"));
    }

    @Test
    @Order(15)
    void costTrends_noProduct_shouldReturnEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("productId", "00000000-0000-0000-0000-000000000000")
                .when()
                .get("/api/v1/costs/trends")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    // --- Analytics Endpoints (Phase 4) ---

    @Test
    @Order(20)
    void analyticsKpis_shouldReturnKpiList() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/analytics/kpis")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(5))
                .body("id", hasItem("total_products"))
                .body("id", hasItem("active_batches"))
                .body("id", hasItem("pending_sales"))
                .body("id", hasItem("completed_sales_today"))
                .body("id", hasItem("total_inventory_value"));
    }

    @Test
    @Order(21)
    void analyticsKpis_shouldHaveCorrectStructure() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/analytics/kpis")
                .then()
                .statusCode(200)
                .body("[0].id", notNullValue())
                .body("[0].label", notNullValue())
                .body("[0].value", notNullValue())
                .body("[0].unit", notNullValue());
    }

    @Test
    @Order(22)
    void analyticsStockHistory_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("from", "2020-01-01T00:00:00Z")
                .queryParam("to", "2030-12-31T23:59:59Z")
                .when()
                .get("/api/v1/analytics/stock-history")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    @Order(23)
    void analyticsStockHistory_withData_shouldReturnPoints() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("from", "2020-01-01T00:00:00Z")
                .queryParam("to", "2030-12-31T23:59:59Z")
                .when()
                .get("/api/v1/analytics/stock-history")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].date", notNullValue())
                .body("[0].totalQuantity", notNullValue());
    }

    @Test
    @Order(24)
    void analyticsStockHistory_defaultParams_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/analytics/stock-history")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(25)
    void analyticsSalesSummary_monthly_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("period", "monthly")
                .when()
                .get("/api/v1/analytics/sales-summary")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    @Order(26)
    void analyticsSalesSummary_weekly_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("period", "weekly")
                .when()
                .get("/api/v1/analytics/sales-summary")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    @Order(27)
    void analyticsSalesSummary_default_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/analytics/sales-summary")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(28)
    void analyticsSalesSummary_withData_shouldReturnPeriods() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("period", "monthly")
                .when()
                .get("/api/v1/analytics/sales-summary")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].period", notNullValue())
                .body("[0].totalSales", greaterThanOrEqualTo(1))
                .body("[0].totalRevenue", notNullValue())
                .body("[0].averageOrderValue", notNullValue())
                .body("[0].totalItemsSold", greaterThanOrEqualTo(1));
    }
}
