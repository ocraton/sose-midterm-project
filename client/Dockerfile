FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/client-orchestrator-1.0-SNAPSHOT-jar-with-dependencies.jar ./client.jar
# Non esponiamo porte perche e un client che esegue uno script e poi si ferma
ENTRYPOINT ["java", "-jar", "client.jar"]