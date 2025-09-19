FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY build/libs/receiver-0.0.1.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]