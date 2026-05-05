FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/ubelluris-*-all.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]