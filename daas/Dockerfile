# STAGE 1: Build (usiamo un'immagine Maven ufficiale con Java 21)
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Impostiamo la cartella di lavoro nel container
WORKDIR /app

# Copiamo prima il pom.xml e scarichiamo le dipendenze (sfrutta la cache di Docker)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiamo il codice sorgente
COPY src ./src

# Compiliamo il progetto e creiamo il fat JAR
RUN mvn clean package

# Copiamo tutte le dipendenze runtime in target/dependency
RUN mvn -DskipTests dependency:copy-dependencies -DincludeScope=runtime

# STAGE 2: Run (usiamo un'immagine più leggera con solo il runtime di Java 21)
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiamo classi compilate e dipendenze runtime dallo STAGE 1
COPY --from=builder /app/target/classes ./classes
COPY --from=builder /app/target/dependency ./dependency

# Esponiamo la porta su cui in ascolto Jetty
EXPOSE 8080

# Comando di avvio del server
ENTRYPOINT ["java", "-cp", "/app/classes:/app/dependency/*", "it.univaq.sose.daas.DaaSApplication"]
