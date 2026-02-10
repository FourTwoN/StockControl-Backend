# =============================================
# Stage 1: Build
# =============================================
FROM gradle:9.3-jdk25 AS build

WORKDIR /app

# Copy Gradle files first for better caching
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Copy all module build files
COPY demeter-common/build.gradle.kts demeter-common/
COPY demeter-productos/build.gradle.kts demeter-productos/
COPY demeter-inventario/build.gradle.kts demeter-inventario/
COPY demeter-ventas/build.gradle.kts demeter-ventas/
COPY demeter-costos/build.gradle.kts demeter-costos/
COPY demeter-usuarios/build.gradle.kts demeter-usuarios/
COPY demeter-ubicaciones/build.gradle.kts demeter-ubicaciones/
COPY demeter-empaquetado/build.gradle.kts demeter-empaquetado/
COPY demeter-precios/build.gradle.kts demeter-precios/
COPY demeter-analytics/build.gradle.kts demeter-analytics/
COPY demeter-fotos/build.gradle.kts demeter-fotos/
COPY demeter-chatbot/build.gradle.kts demeter-chatbot/
COPY demeter-app/build.gradle.kts demeter-app/

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY . .

# Build the application (fast-jar for better Cloud Run startup)
RUN gradle :demeter-app:quarkusBuild --no-daemon

# =============================================
# Stage 2: Runtime
# =============================================
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S demeter && adduser -S demeter -G demeter

# Copy the built artifact
COPY --from=build /app/demeter-app/build/quarkus-app/ ./

# Switch to non-root user
USER demeter

EXPOSE 8080

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar quarkus-run.jar"]
