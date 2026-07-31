FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

CMD ["java", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]