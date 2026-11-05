# syntax=docker/dockerfile:1
# Imagem da API RotaCerta. Usada pelo Dokploy (deploy a cada push na main) e roda em amd64 ou arm64.
#   docker build -t rotacerta-api .
#   docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod -e POSTGRES_HOST=... rotacerta-api

# 1) build: compila com o Maven Wrapper do projeto (testes rodam antes do commit, não aqui)
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B --no-transfer-progress -DskipTests package \
 && cp target/spring.transportadora-*.jar app.jar

# 2) execução: só o JRE + o jar, com usuário sem privilégio
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 rotacerta
WORKDIR /app
COPY --from=build /app/app.jar app.jar
USER rotacerta
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
