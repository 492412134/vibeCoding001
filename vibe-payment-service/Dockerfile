FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/vibe-payment-service-*.jar app.jar

EXPOSE 10991

ENTRYPOINT ["java", "-jar", "app.jar"]
