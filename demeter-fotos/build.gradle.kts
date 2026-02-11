dependencies {
    implementation(project(":demeter-common"))
    implementation(project(":demeter-productos"))

    // REST client for calling ML Worker
    implementation("io.quarkus:quarkus-rest-client-jackson")

    // Google Cloud Storage for image storage (production)
    implementation("com.google.cloud:google-cloud-storage:2.36.1")
}
