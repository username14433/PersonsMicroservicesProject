# ЭТАП 1: сборка (multi-stage, build)
# Базовый образ: gradle:8.14.2-jdk24 - среда с JDK 24 и Gradle
# ARG... + ENV... - пробрасывает креды и URL Nexus (для публикации артефактов)
FROM gradle:8.14.2-jdk24 AS build
ARG NEXUS_URL=http://host.docker.internal:8081/repository/maven-snapshots
ARG NEXUS_USERNAME=admin
ARG NEXUS_PASSWORD=admin

ENV NEXUS_URL=${NEXUS_URL}
ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}

# Копирование проекта: сначала build.gradle.kts, settings.gradle.kts (лучше кэшируются зависимости)
# Затем src и openapi
# Сборка: gradle --no-daemon clean build -x test - собриет JAR (без тестов)
# Публикация: gradle publish --info - заливает собранные артефакты в Nexus
WORKDIR /home/gradle/project

COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src ./src
COPY openapi ./openapi

RUN gradle --no-daemon clean build -x test

# Не работает, потому что Nexus запускается позже и gradle не может к нему подключиться,
# чтобы опубликовать артефакты. Поэтому пришлось в docker compose делать новый сервис persons-api-publisher,
# который ожидает запуска nexus и только потом запускается сам
#RUN gradle publish --info

# ЭТАП 2: рантайм (runtime)
# Базовый образ: eclipse-temurin:24-jre - облегчённый JRE для запуска
# Устанавливает curl для (health-check/отладки),
# чистит apt-кэш
FROM eclipse-temurin:24-jre

# WORKDIR /app
# Копирует из билд-слоя все JAR'ы build/libs/*.jar как app.jar
# EXPOSE 8092 - декларация порта приложения
# Команда запуска: java -jar app.jar
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8091
CMD ["java", "-jar", "app.jar"]