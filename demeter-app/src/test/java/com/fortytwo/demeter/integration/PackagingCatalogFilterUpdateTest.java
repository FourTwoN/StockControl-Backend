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
class PackagingCatalogFilterUpdateTest {

    private static final String TENANT = "tenant-pkg-fu";

    private static String typeIdA;
    private static String typeIdB;
    private static String materialId;
    private static String colorId;
    private static String catalogIdA;
    private static String catalogIdB;

    @Test
    @Order(1)
    void setup_createTypesAndMaterial() {
        typeIdA = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Crate", "description": "Wooden crate"}
                        """)
                .when()
                .post("/api/v1/packaging/types")
                .then()
                .statusCode(201)
                .extract().path("id");

        typeIdB = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Bag", "description": "Plastic bag"}
                        """)
                .when()
                .post("/api/v1/packaging/types")
                .then()
                .statusCode(201)
                .extract().path("id");

        materialId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Wood", "description": "Pine wood"}
                        """)
                .when()
                .post("/api/v1/packaging/materials")
                .then()
                .statusCode(201)
                .extract().path("id");

        colorId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Brown", "hexCode": "#8B4513"}
                        """)
                .when()
                .post("/api/v1/packaging/colors")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(2)
    void setup_createCatalogs() {
        catalogIdA = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Small Wooden Crate",
                            "typeId": "%s",
                            "materialId": "%s",
                            "colorId": "%s",
                            "capacity": 5,
                            "unit": "kg"
                        }
                        """.formatted(typeIdA, materialId, colorId))
                .when()
                .post("/api/v1/packaging/catalogs")
                .then()
                .statusCode(201)
                .extract().path("id");

        catalogIdB = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Large Plastic Bag",
                            "typeId": "%s",
                            "capacity": 25,
                            "unit": "lbs"
                        }
                        """.formatted(typeIdB))
                .when()
                .post("/api/v1/packaging/catalogs")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    // --- Filter tests ---

    @Test
    @Order(3)
    void filterBySearch_shouldMatchName() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("search", "Wooden")
                .when()
                .get("/api/v1/packaging/catalogs")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].name", equalTo("Small Wooden Crate"));
    }

    @Test
    @Order(4)
    void filterBySearchCaseInsensitive_shouldWork() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("search", "plastic")
                .when()
                .get("/api/v1/packaging/catalogs")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].name", equalTo("Large Plastic Bag"));
    }

    @Test
    @Order(5)
    void filterByTypeId_shouldReturnMatchingOnly() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("typeId", typeIdA)
                .when()
                .get("/api/v1/packaging/catalogs")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].typeName", equalTo("Crate"));
    }

    @Test
    @Order(6)
    void filterByMaterialId_shouldReturnMatchingOnly() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("materialId", materialId)
                .when()
                .get("/api/v1/packaging/catalogs")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].materialName", equalTo("Wood"));
    }

    @Test
    @Order(7)
    void filterByColorId_shouldReturnMatchingOnly() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("colorId", colorId)
                .when()
                .get("/api/v1/packaging/catalogs")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].colorName", equalTo("Brown"));
    }

    @Test
    @Order(8)
    void filterCombined_shouldIntersect() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("typeId", typeIdA)
                .queryParam("materialId", materialId)
                .when()
                .get("/api/v1/packaging/catalogs")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].name", equalTo("Small Wooden Crate"));
    }

    @Test
    @Order(9)
    void filterCombinedNoMatch_shouldReturnEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("typeId", typeIdB)
                .queryParam("materialId", materialId)
                .when()
                .get("/api/v1/packaging/catalogs")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }

    @Test
    @Order(10)
    void noFilters_shouldReturnAll() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/packaging/catalogs")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    // --- PUT update tests ---

    @Test
    @Order(11)
    void updateCatalog_shouldModifyName() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Medium Wooden Crate"}
                        """)
                .when()
                .put("/api/v1/packaging/catalogs/" + catalogIdA)
                .then()
                .statusCode(200)
                .body("name", equalTo("Medium Wooden Crate"))
                .body("typeName", equalTo("Crate"))
                .body("materialName", equalTo("Wood"));
    }

    @Test
    @Order(12)
    void updateCatalog_shouldModifyCapacityAndUnit() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"capacity": 15.5, "unit": "lbs"}
                        """)
                .when()
                .put("/api/v1/packaging/catalogs/" + catalogIdA)
                .then()
                .statusCode(200)
                .body("capacity", equalTo(15.5f))
                .body("unit", equalTo("lbs"));
    }

    @Test
    @Order(13)
    void updateCatalog_shouldChangeType() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"typeId": "%s"}
                        """.formatted(typeIdB))
                .when()
                .put("/api/v1/packaging/catalogs/" + catalogIdA)
                .then()
                .statusCode(200)
                .body("typeName", equalTo("Bag"));
    }

    @Test
    @Order(14)
    void updateCatalog_nonExistentId_shouldReturn404() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Ghost"}
                        """)
                .when()
                .put("/api/v1/packaging/catalogs/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }
}
