FROM eclipse-temurin:17-jre
ARG JAR_FILE=build/libs/trade-store-0.1.0.jar
COPY ${JAR_FILE} /app/trade-store.jar
ENTRYPOINT ["java", "-jar", "/app/trade-store.jar", "--spring.profiles.active=batch"]
