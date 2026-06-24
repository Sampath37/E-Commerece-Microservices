FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar from the target directory
COPY target/*.jar app.jar

# Expose the service port
EXPOSE 8083

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
