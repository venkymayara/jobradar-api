FROM node:24-bookworm-slim AS frontend
ARG UI_REVISION=688a95b49712089d59a3df1f0fec9ddee66a768b
WORKDIR /ui
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL --retry 3 "https://codeload.github.com/venkymayara/jobradar-ui/tar.gz/${UI_REVISION}" \
       | tar -xz --strip-components=1
RUN npm ci && npm run build \
    && mv dist/JR_UI/browser/index.csr.html dist/JR_UI/browser/index.html

FROM maven:3.9-eclipse-temurin-25 AS backend
WORKDIR /build
COPY pom.xml .
COPY src ./src
COPY --from=frontend /ui/dist/JR_UI/browser/ ./src/main/resources/static/
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-noble
WORKDIR /app
COPY --from=backend /build/target/jr-api-0.0.1-SNAPSHOT.jar ./jobradar.jar
USER 10001
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=55.0 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/jobradar.jar"]
