# ---- build ----
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# tests need Docker (Testcontainers), so they run locally / in CI, not inside the image build
RUN mvn -q -B package -DskipTests

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/target/azhar-council-archive-1.0.0.jar app.jar
USER app
# free hosting tiers give ~512 MB of RAM
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
