FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre
LABEL name="zoo-app" version="0.0.1-SNAPSHOT"
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
VOLUME ["/app/db", "/app/uploads"]
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]