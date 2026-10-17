# Fase 1: Compilazione con Maven e Java 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Fase 2: Esecuzione con Java 21
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
# Copia il file .jar generato dalla fase 1
COPY --from=build /app/target/*.jar app.jar
# Espone la porta 8080
EXPOSE 8080
# Avvia l'applicazione
ENTRYPOINT ["java", "-jar", "app.jar"]