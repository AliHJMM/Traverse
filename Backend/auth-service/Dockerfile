FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Least privilege (Plan.md Phase 11): runs as a dedicated non-root user
# instead of the image's default root -- nothing here needs root (no
# privileged port, no host filesystem access).
RUN addgroup -S app && adduser -S app -G app
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
