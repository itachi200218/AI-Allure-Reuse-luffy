# Use Maven with Java 17 (includes both Java & Maven)
FROM maven:3.9.4-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn -B dependency:resolve dependency:resolve-plugins

# Copy source code
COPY src ./src

# Build the Spring Boot application (skip tests for faster build)
RUN mvn -B clean package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy the built JAR from previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose the dynamic port (Render uses PORT env variable)
EXPOSE 8080
ENV PORT=8080

# Run the Spring Boot JAR
CMD ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]
