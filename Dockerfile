# Этап сборки сервиса Processor
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# Копируем Gradle wrapper и файлы конфигурации проекта
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Копируем исходный код приложения
COPY src src

# Собираем jar-файл приложения
RUN chmod +x gradlew && ./gradlew bootJar

# Финальный этап формирования образа для запуска приложения
FROM openjdk:21-jdk-slim
WORKDIR /app

# Копируем собранный jar-файл
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8083

# Запуск приложения
CMD ["java", "-jar", "app.jar"]