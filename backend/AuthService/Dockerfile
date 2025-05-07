# Use Java 21 base image
FROM eclipse-temurin:21-jre-alpine

# Set working directory inside container
WORKDIR /app

# Copy your JAR file into the image
COPY target/AuthService-1.0-SNAPSHOT.jar app.jar

# Expose port if needed (e.g., for REST APIs)
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
