FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp package -DskipTests
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S blackout && adduser -S blackout -G blackout
COPY --from=build /app/target/*.jar app.jar
USER blackout
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
