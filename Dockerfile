FROM bellsoft/liberica-openjre-alpine:21.0.13.0.3-5
WORKDIR /app
COPY build/libs/ubelluris-*-all.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]