# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ---- Docker CLI stage (just the binary) ----
FROM docker:27-cli AS docker-cli

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
# Copy the Docker CLI binary from the official docker:cli image
COPY --from=docker-cli /usr/local/bin/docker /usr/local/bin/docker
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
