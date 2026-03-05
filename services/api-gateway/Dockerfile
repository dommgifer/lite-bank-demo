# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Build the application (skip tests for faster build)
RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests && \
    mv target/*.jar app.jar

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Download OpenTelemetry Java Agent
ADD --chmod=644 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar /otel/javaagent.jar

# Copy JAR from build stage
COPY --from=build /app/app.jar .

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
