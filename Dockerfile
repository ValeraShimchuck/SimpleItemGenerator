# Use Gradle 8 with JDK 16
FROM gradle:8.14.3-jdk17

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and properties for consistent builds
COPY gradle/ gradle/
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY gradle.properties gradle.properties

# Copy build files
COPY build.gradle.kts .
COPY settings.gradle .

# Copy source code
COPY src/ src/
COPY api/ api/
COPY config/ config/
COPY examples/ examples/
COPY test-plugin/ test-plugin/

# Make gradlew executable
RUN chmod +x gradlew

# Set the default command to build the project using shadowJar
CMD ["./gradlew", "shadowJar", "--no-daemon"]
