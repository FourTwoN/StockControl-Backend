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
class PriceListControllerTest {

    private static final String TENANT = "tenant-pricelist";

    private static String productId;
    private static String priceListId;
    private static String entryId;

    @Test
    @Order(1)
    void setup_createProduct() {
        productId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "PL-PROD-001", "name": "Price List Test Product"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(2)
    void createPriceList_shouldReturn201() {
        priceListId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Retail Q1",
                            "description": "Q1 Prices",
                            "effectiveDate": "2024-01-01"
                        }
                        """)
                .when()
                .post("/api/v1/price-lists")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Retail Q1"))
                .body("description", equalTo("Q1 Prices"))
                .extract().path("id");
    }

    @Test
    @Order(3)
    void getPriceList_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/price-lists/" + priceListId)
                .then()
                .statusCode(200)
                .body("id", equalTo(priceListId))
                .body("name", equalTo("Retail Q1"))
                .body("createdAt", notNullValue());
    }

    @Test
    @Order(4)
    void listPriceLists_shouldReturnPaged() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/price-lists")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    @Test
    @Order(5)
    void updatePriceList_shouldModify() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Updated Retail Q1"
                        }
                        """)
                .when()
                .put("/api/v1/price-lists/" + priceListId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Retail Q1"));
    }

    @Test
    @Order(6)
    void activatePriceList_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v1/price-lists/" + priceListId + "/activate")
                .then()
                .statusCode(200)
                .body("active", equalTo(true));
    }

    @Test
    @Order(7)
    void getActivePriceLists_shouldReturn() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/price-lists/active")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(8)
    void addEntry_shouldReturn201() {
        entryId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "price": 99.99,
                            "currency": "USD"
                        }
                        """.formatted(productId))
                .when()
                .post("/api/v1/price-lists/" + priceListId + "/entries")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("price", equalTo(99.99f))
                .extract().path("id");
    }

    @Test
    @Order(9)
    void bulkEntries_shouldReturn201() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        [
                            {
                                "productId": "%s",
                                "price": 49.99
                            }
                        ]
                        """.formatted(productId))
                .when()
                .post("/api/v1/price-lists/" + priceListId + "/entries/bulk")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(10)
    void deleteEntry_shouldReturn204() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .delete("/api/v1/price-lists/" + priceListId + "/entries/" + entryId)
                .then()
                .statusCode(204);
    }
}
