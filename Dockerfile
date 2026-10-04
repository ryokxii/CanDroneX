# Étape 1 : compilation
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dépendances résolues avant les sources, pour garder cette couche en cache.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Étape 2 : exécution (JRE seule)
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl : healthcheck de docker-compose
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/target/candronex.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
