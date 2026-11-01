# docker compose build : une image par service, SERVICE=drone-service | order-service | slice-service
FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /build

# Dépendances résolues avant les sources, pour garder cette couche en cache.
COPY pom.xml .
COPY platform/pom.xml platform/
COPY drone-service/pom.xml drone-service/
COPY order-service/pom.xml order-service/
COPY slice-service/pom.xml slice-service/
RUN mvn -B -q -pl ${SERVICE} -am dependency:go-offline

COPY platform/src platform/src
COPY ${SERVICE}/src ${SERVICE}/src
RUN mvn -B -q -pl ${SERVICE} -am clean package -DskipTests

FROM eclipse-temurin:21-jre
ARG SERVICE
WORKDIR /app

# curl : healthcheck de docker-compose
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/${SERVICE}/target/${SERVICE}.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
