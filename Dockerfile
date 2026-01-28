FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/ubelluris-1.0-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]