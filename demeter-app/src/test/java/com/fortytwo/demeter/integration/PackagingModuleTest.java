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
class PackagingModuleTest {

    private static final String TENANT = "tenant-packaging";

    private static String typeId;
    private static String materialId;
    private static String colorId;
    private static String catalogId;

    @Test
    @Order(1)
    void createPackagingType_shouldReturn201() {
        typeId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Box", "description": "Standard box"}
                        """)
                .when()
                .post("/api/v1/packaging/types")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Box"))
                .body("description", equalTo("Standard box"))
                .extract().path("id");
    }

    @Test
    @Order(2)
    void createPackagingMaterial_shouldReturn201() {
        materialId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Cardboard", "description": "Standard cardboard"}
                        """)
                .when()
                .post("/api/v1/packaging/materials")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Cardboard"))
                .body("description", equalTo("Standard cardboard"))
                .extract().path("id");
    }

    @Test
    @Order(3)
    void createPackagingColor_shouldReturn201() {
        colorId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Red", "hexCode": "#FF0000"}
                        """)
                .when()
                .post("/api/v1/packaging/colors")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Red"))
                .body("hexCode", equalTo("#FF0000"))
                .extract().path("id");
    }

    @Test
    @Order(4)
    void createPackagingCatalog_shouldReturn201() {
        catalogId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Standard Red Box",
                            "typeId": "%s",
                            "materialId": "%s",
                            "colorId": "%s",
                            "capacity": 10,
                            "unit": "kg"
                        }
                        """.formatted(typeId, materialId, colorId))
                .when()
                .post("/api/v1/packaging/catalogs")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Standard Red Box"))
                .body("typeName", equalTo("Box"))
                .body("materialName", equalTo("Cardboard"))
                .body("colorName", equalTo("Red"))
                .body("unit", equalTo("kg"))
                .extract().path("id");
    }

    @Test
    @Order(5)
    void listPackagingTypes_shouldReturnPaged() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/packaging/types")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    @Test
    @Order(6)
    void listPackagingMaterials_shouldReturnPaged() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/packaging/materials")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    @Test
    @Order(7)
    void listPackagingColors_shouldReturnPaged() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/packaging/colors")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }
}
