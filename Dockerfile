# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /home/app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Package stage
FROM eclipse-temurin:17-jre
WORKDIR /usr/local/lib
COPY --from=build /home/app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
