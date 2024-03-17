FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package

FROM eclipse-temurin:21.0.2_13-jdk-alpine
COPY --from=build /app/target/*jar-with-dependencies.jar /Lil-Mo.jar
ENTRYPOINT ["java","-jar","Lil-Mo.jar"]