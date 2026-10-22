FROM gradle:8.14-jdk21 AS build

WORKDIR /app
COPY . .
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-jar", "/app/app.jar"]
