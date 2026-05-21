# STAGE 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package

# STAGE 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/eaas-service-1.0-SNAPSHOT-jar-with-dependencies.jar ./eaas-service.jar

# Creiamo esplicitamente la cartella che ospiterà il volume con il file JSON delle policy
RUN mkdir -p /app/policies

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "eaas-service.jar"]