FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# mvnw is committed without the exec bit (Windows checkout)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src/ src/
RUN ./mvnw clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN mkdir -p logs && chown -R appuser:appgroup /app

USER appuser

# Render free instances have 512MB RAM
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
