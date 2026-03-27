FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/vibe-auth-*.jar app.jar

EXPOSE 11000

ENTRYPOINT ["java", "-jar", "app.jar"]
