# syntax=docker/dockerfile:1.7

FROM postgres:17-alpine AS build

RUN apk add --no-cache openjdk21-jdk

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /workspace

COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

RUN chmod +x gradlew

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies

COPY detekt.yml ./
COPY src ./src

RUN --mount=type=cache,target=/root/.gradle \
    set -eux; \
    build_pgdata="$(mktemp -d)"; \
    chown postgres:postgres "${build_pgdata}"; \
    gosu postgres initdb \
        --pgdata="${build_pgdata}" \
        --username=postgres \
        --auth-local=trust \
        --auth-host=trust; \
    gosu postgres pg_ctl \
        --pgdata="${build_pgdata}" \
        --options="-c listen_addresses=127.0.0.1" \
        --wait \
        start; \
    trap 'gosu postgres pg_ctl --pgdata="${build_pgdata}" --mode=fast stop' EXIT; \
    gosu postgres createdb certis_build; \
    DB_HOST=127.0.0.1 \
    DB_PORT=5432 \
    DB_NAME=certis_build \
    DB_USER=postgres \
    DB_PASSWORD=build-only \
    DB_SCHEMA=keeper \
        ./gradlew --no-daemon update; \
    DB_HOST=127.0.0.1 \
    DB_PORT=5432 \
    DB_NAME=certis_build \
    DB_USER=postgres \
    DB_PASSWORD=build-only \
    DB_SCHEMA=keeper \
        ./gradlew --no-daemon clean bootJar -x test; \
    gosu postgres pg_ctl \
        --pgdata="${build_pgdata}" \
        --mode=fast \
        --wait \
        stop; \
    trap - EXIT

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S certis \
    && adduser -S -G certis certis

WORKDIR /app

COPY --from=build --chown=certis:certis /workspace/build/libs/*.jar /app/app.jar

USER certis

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
