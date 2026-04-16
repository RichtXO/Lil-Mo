FROM maven:3.9-eclipse-temurin-23-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package

FROM eclipse-temurin:23-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/target/*jar-with-dependencies.jar /Lil-Mo.jar
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar","Lil-Mo.jar"]