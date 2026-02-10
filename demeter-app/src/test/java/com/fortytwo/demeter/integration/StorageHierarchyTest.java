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
class StorageHierarchyTest {

    private static final String TENANT = "tenant-storage";

    private static String binTypeId;
    private static String warehouseId;
    private static String areaId;
    private static String locationId;
    private static String binId;

    @Test
    @Order(1)
    void createBinType_shouldReturn201() {
        binTypeId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Standard",
                            "capacity": 50,
                            "description": "Std bin"
                        }
                        """)
                .when()
                .post("/api/v1/bin-types")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Standard"))
                .body("capacity", equalTo(50))
                .extract().path("id");
    }

    @Test
    @Order(2)
    void createWarehouse_shouldReturn201() {
        warehouseId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Main Warehouse",
                            "address": "123 Street"
                        }
                        """)
                .when()
                .post("/api/v1/warehouses")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Main Warehouse"))
                .body("address", equalTo("123 Street"))
                .extract().path("id");
    }

    @Test
    @Order(3)
    void createArea_shouldReturn201() {
        areaId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Zone A",
                            "description": "First zone"
                        }
                        """)
                .when()
                .post("/api/v1/warehouses/" + warehouseId + "/areas")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Zone A"))
                .body("description", equalTo("First zone"))
                .extract().path("id");
    }

    @Test
    @Order(4)
    void createLocation_shouldReturn201() {
        locationId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Rack 1",
                            "description": "First rack"
                        }
                        """)
                .when()
                .post("/api/v1/areas/" + areaId + "/locations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Rack 1"))
                .body("description", equalTo("First rack"))
                .extract().path("id");
    }

    @Test
    @Order(5)
    void createBin_shouldReturn201() {
        binId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "code": "BIN-001",
                            "binTypeId": "%s"
                        }
                        """.formatted(binTypeId))
                .when()
                .post("/api/v1/locations/" + locationId + "/bins")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("code", equalTo("BIN-001"))
                .extract().path("id");
    }

    @Test
    @Order(6)
    void listWarehouses_shouldReturnPaged() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/warehouses")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20))
                .body("totalElements", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(7)
    void listAreas_shouldReturnList() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/warehouses/" + warehouseId + "/areas")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(8)
    void listLocations_shouldReturnList() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/areas/" + areaId + "/locations")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(9)
    void listBins_shouldReturnList() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/locations/" + locationId + "/bins")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(10)
    void deleteBin_shouldReturn204() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .delete("/api/v1/locations/" + locationId + "/bins/" + binId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(11)
    void deleteWarehouse_shouldReturn204() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .delete("/api/v1/warehouses/" + warehouseId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(12)
    void createWarehouse_missingName_shouldReturn400() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "address": "No name"
                        }
                        """)
                .when()
                .post("/api/v1/warehouses")
                .then()
                .statusCode(400);
    }
}
