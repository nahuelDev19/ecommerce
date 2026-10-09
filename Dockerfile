# =============================================================================
# Dockerfile para Spring Boot 4.1 (Java 17+) - despliegue en Render
# Build multi-etapa: compila con Maven y ejecuta solo el JRE (imagen final chica).
# =============================================================================

# -----------------------------------------------------------------------------
# ETAPA 1: build
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# 1) Copiamos solo el pom para aprovechar la cache de capas de Docker:
#    mientras el pom no cambie, las dependencias no se vuelven a descargar.
COPY pom.xml ./
RUN mvn -B -Dmaven.test.skip=true dependency:go-offline || true

# 2) Ahora si el codigo fuente.
COPY src ./src

# -Dmaven.test.skip=true: no compila ni ejecuta tests.
# Los tests usan Testcontainers (necesitan Docker) y no deben correr en el build.
RUN mvn -B -Dmaven.test.skip=true clean package \
    && find target -maxdepth 1 -name '*.jar' ! -name '*.original' -exec cp {} /build/app.jar \;

# -----------------------------------------------------------------------------
# ETAPA 2: runtime
# -----------------------------------------------------------------------------
# Si preferis paridad exacta con tu JDK local (17), cambia "21" por "17".
FROM eclipse-temurin:21-jre-jammy AS runtime

# Usuario sin privilegios (Render no necesita root para correr la app).
RUN groupadd --system --gid 1001 spring \
    && useradd --system --uid 1001 --gid spring --create-home spring

WORKDIR /app

COPY --from=build --chown=spring:spring /build/app.jar /app/app.jar

# Carpeta temporal usada por la importacion de Excel (importacion.directorio-temporal).
RUN mkdir -p /tmp/importaciones && chown -R spring:spring /tmp/importaciones

USER spring

# Render inyecta PORT; application.yml ya lo lee con ${PORT:8080}.
EXPOSE 8080

# MaxRAMPercentage: la JVM respeta el limite de memoria del contenedor (Render free = 512 MB).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# exec: java queda como PID 1 y recibe bien el SIGTERM (apagado limpio).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
