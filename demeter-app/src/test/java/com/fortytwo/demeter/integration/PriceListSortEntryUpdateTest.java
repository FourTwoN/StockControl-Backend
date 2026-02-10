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
class PriceListSortEntryUpdateTest {

    private static final String TENANT = "tenant-pl-sort";

    private static String productId;
    private static String priceListIdA;
    private static String priceListIdB;
    private static String entryId;

    @Test
    @Order(1)
    void setup_createProduct() {
        productId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "PLS-PROD", "name": "Price Sort Product"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(2)
    void setup_createPriceLists() {
        priceListIdA = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Alpha Prices",
                            "description": "First list",
                            "effectiveDate": "2024-01-01"
                        }
                        """)
                .when()
                .post("/api/v1/price-lists")
                .then()
                .statusCode(201)
                .extract().path("id");

        priceListIdB = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Beta Prices",
                            "description": "Second list",
                            "effectiveDate": "2024-06-01"
                        }
                        """)
                .when()
                .post("/api/v1/price-lists")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    // --- Sort tests ---

    @Test
    @Order(3)
    void sortByNameAsc_shouldReturnAlphaFirst() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("sort", "name")
                .when()
                .get("/api/v1/price-lists")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2))
                .body("content[0].name", equalTo("Alpha Prices"));
    }

    @Test
    @Order(4)
    void sortByNameDesc_shouldReturnBetaFirst() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("sort", "-name")
                .when()
                .get("/api/v1/price-lists")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2))
                .body("content[0].name", equalTo("Beta Prices"));
    }

    @Test
    @Order(5)
    void sortByCreatedAtDefault_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/price-lists")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    @Test
    @Order(6)
    void sortByInvalidField_shouldFallbackToDefault() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("sort", "invalidField")
                .when()
                .get("/api/v1/price-lists")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    // --- Entry update tests ---

    @Test
    @Order(7)
    void setup_addEntry() {
        entryId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "price": 50.00,
                            "currency": "USD",
                            "minQuantity": 1
                        }
                        """.formatted(productId))
                .when()
                .post("/api/v1/price-lists/" + priceListIdA + "/entries")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("price", equalTo(50.0f))
                .extract().path("id");
    }

    @Test
    @Order(8)
    void updateEntry_shouldModifyPrice() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"price": 75.50}
                        """)
                .when()
                .put("/api/v1/price-lists/" + priceListIdA + "/entries/" + entryId)
                .then()
                .statusCode(200)
                .body("price", equalTo(75.5f))
                .body("currency", equalTo("USD"));
    }

    @Test
    @Order(9)
    void updateEntry_shouldModifyCurrency() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"currency": "EUR"}
                        """)
                .when()
                .put("/api/v1/price-lists/" + priceListIdA + "/entries/" + entryId)
                .then()
                .statusCode(200)
                .body("currency", equalTo("EUR"))
                .body("price", equalTo(75.5f));
    }

    @Test
    @Order(10)
    void updateEntry_shouldModifyMinQuantity() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"minQuantity": 10}
                        """)
                .when()
                .put("/api/v1/price-lists/" + priceListIdA + "/entries/" + entryId)
                .then()
                .statusCode(200)
                .body("minQuantity", equalTo(10));
    }

    @Test
    @Order(11)
    void updateEntry_multipleFields_shouldModifyAll() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"price": 120.00, "currency": "GBP", "minQuantity": 5}
                        """)
                .when()
                .put("/api/v1/price-lists/" + priceListIdA + "/entries/" + entryId)
                .then()
                .statusCode(200)
                .body("price", equalTo(120.0f))
                .body("currency", equalTo("GBP"))
                .body("minQuantity", equalTo(5));
    }

    @Test
    @Order(12)
    void updateEntry_nonExistentId_shouldReturn404() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"price": 999}
                        """)
                .when()
                .put("/api/v1/price-lists/" + priceListIdA + "/entries/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }
}
