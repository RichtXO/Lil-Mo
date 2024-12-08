FROM maven:3.9-eclipse-temurin-23-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

FROM eclipse-temurin:23-jre-alpine
COPY --from=build /app/target/*jar-with-dependencies.jar /Lil-Mo.jar
ENTRYPOINT ["java","-jar","Lil-Mo.jar"]