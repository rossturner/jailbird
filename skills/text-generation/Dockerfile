FROM openjdk:21-jdk-slim

# Install necessary tools
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy source code
COPY src/ src/

# Copy libs (needed for building)
COPY ../../libs/ ../libs/
COPY ../../proto/ ../proto/

# Make gradlew executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew build -x test

# Expose ports
EXPOSE 50008 50005

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:50008/actuator/health || exit 1

# Run the application
CMD ["./gradlew", "bootRun"]