FROM openjdk:21-jdk-slim AS builder

# Устанавливаем корневую рабочую директорию для сборки.
WORKDIR /build_root

# Копируем общую библиотеку em-library-common.
COPY em-library-common/ /build_root/em-library-common/

# Копируем Gradle wrapper и файлы конфигурации проекта для em-processor.
COPY em-processor/gradlew /build_root/em-processor/gradlew
COPY em-processor/gradle /build_root/em-processor/gradle
COPY em-processor/build.gradle.kts /build_root/em-processor/build.gradle.kts
COPY em-processor/settings.gradle.kts /build_root/em-processor/settings.gradle.kts
COPY em-processor/gradle.properties /build_root/em-processor/gradle.properties

# Копируем исходный код приложения em-processor.
COPY em-processor/src/ /build_root/em-processor/src/

# Переходим в рабочую директорию сервиса em-processor внутри контейнера.
WORKDIR /build_root/em-processor

# Собираем jar-файл
# Файл settings.gradle.kts с includeBuild("../em-library-common")
# корректно найдет библиотеку по пути /build_root/em-library-common/.
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# Финальный этап формирования образа для запуска приложения
FROM openjdk:21-jdk-slim
WORKDIR /app

# Создаем директорию для логов
RUN mkdir -p /app/logs && chmod 777 /app/logs

# Копируем собранный jar-файл из этапа сборки
COPY --from=builder /build_root/em-processor/build/libs/*.jar app.jar

EXPOSE 8080

# Запуск приложения
CMD ["java", "-jar", "app.jar"]


# ========== Конфигурация при использовании внешнего хранилища для общей либы ==========

# Этап сборки сервиса Processor
#FROM openjdk:21-jdk-slim AS builder

#WORKDIR /app

# Копируем Gradle wrapper и файлы конфигурации проекта
#COPY gradlew .
#COPY gradle gradle
#COPY build.gradle.kts .
#COPY settings.gradle.kts .
#COPY gradle.properties .

# Копируем исходный код приложения
#COPY src src

# Собираем jar-файл приложения
#RUN chmod +x gradlew && ./gradlew bootJar

# Финальный этап формирования образа для запуска приложения
#FROM openjdk:21-jdk-slim
#WORKDIR /app

# Создаем директорию для логов
#RUN mkdir -p /app/logs && chmod 777 /app/logs

# Копируем собранный jar-файл
#COPY --from=builder /app/build/libs/*.jar app.jar

#EXPOSE 8080

# Запуск приложения
#CMD ["java", "-jar", "app.jar"]