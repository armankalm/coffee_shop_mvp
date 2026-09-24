# Production image for Render: Spring Boot API + the built React app in one container.
# Render's free plan gives one web service per image, and serving the SPA from the
# same origin removes CORS and cross-site cookie issues (Google sign-in state cookie).

# ---------------------------------------------------------------------
# Stage 1: frontend
# ---------------------------------------------------------------------
FROM node:22-alpine AS frontend-build

WORKDIR /build

# Manifests first so the npm ci layer is cached until package-lock.json changes.
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./

# Vite inlines VITE_* at build time. An empty origin makes the app call /api on
# its own host, which is exactly where the backend serves it.
ENV VITE_API_ORIGIN=""
RUN npm run build

# ---------------------------------------------------------------------
# Stage 2: backend (the SPA is packed into the jar as classpath:/static)
# ---------------------------------------------------------------------
# backend/mvnw only delegates to a Maven install on the dev machine, so the image
# brings its own Maven.
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-build

WORKDIR /build

COPY backend/pom.xml ./
RUN mvn dependency:go-offline -q

COPY backend/src/ src/
COPY --from=frontend-build /build/dist/ src/main/resources/static/
RUN mvn clean package -DskipTests -q

# ---------------------------------------------------------------------
# Stage 3: runtime
# ---------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=backend-build /build/target/*.jar app.jar

RUN mkdir -p logs && chown -R appuser:appgroup /app

USER appuser

# Render free instances have 512MB RAM
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
