plugins {
    `java-library`
}

dependencies {
    api("io.quarkus:quarkus-hibernate-orm-panache")
    api("io.quarkus:quarkus-jdbc-postgresql")
    api("io.quarkus:quarkus-oidc")
    api("io.quarkus:quarkus-smallrye-jwt")
    api("io.quarkus:quarkus-rest-jackson")
    api("io.quarkus:quarkus-hibernate-validator")
    api("io.quarkus:quarkus-smallrye-openapi")
    api("io.quarkus:quarkus-flyway")

    // Cloud SQL Socket Factory for Cloud Run deployment
    api("com.google.cloud.sql:postgres-socket-factory:1.21.0")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
}
