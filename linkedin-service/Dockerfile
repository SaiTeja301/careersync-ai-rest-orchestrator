FROM eclipse-temurin:17-jre-alpine
# Install chromium and chromium-driver (needed for Selenium chrome automation)
RUN apk add --no-cache chromium chromium-chromedriver
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
