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
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class PackagingCrudValidationTest {

    private static final String TENANT = "tenant-pkg-valid";

    private static String typeId;
    private static String materialId;
    private static String colorId;

    @Test
    @Order(1)
    void setup_createType() {
        typeId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Bag"}
                        """)
                .when()
                .post("/api/v1/packaging/types")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(2)
    void createType_missingName_shouldReturn400() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {}
                        """)
                .when()
                .post("/api/v1/packaging/types")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(3)
    void getType_nonExistent_shouldReturn404() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/packaging/types/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    void updateType_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Updated Bag"}
                        """)
                .when()
                .put("/api/v1/packaging/types/" + typeId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Bag"));
    }

    @Test
    @Order(5)
    void setup_createMaterial() {
        materialId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Plastic"}
                        """)
                .when()
                .post("/api/v1/packaging/materials")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(6)
    void updateMaterial_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Updated Plastic"}
                        """)
                .when()
                .put("/api/v1/packaging/materials/" + materialId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Plastic"));
    }

    @Test
    @Order(7)
    void setup_createColor() {
        colorId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Blue", "hexCode": "#0000FF"}
                        """)
                .when()
                .post("/api/v1/packaging/colors")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(8)
    void updateColor_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Updated Blue", "hexCode": "#0000EE"}
                        """)
                .when()
                .put("/api/v1/packaging/colors/" + colorId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Blue"))
                .body("hexCode", equalTo("#0000EE"));
    }
}
