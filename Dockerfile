FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/lib/ lib/
COPY target/ubelluris-*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]