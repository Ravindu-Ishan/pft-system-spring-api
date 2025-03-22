# Use OpenJDK runtime image
FROM eclipse-temurin:23-alpine

# Set the working directory
WORKDIR /app

# Copy the already built JAR file into the container
COPY target/*.jar app.jar

# Expose the application port (should match the one in properties file)
EXPOSE 9090

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
