# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Create Maven settings inside the container (no external file needed)
RUN mkdir -p /root/.m2 && echo '<settings><mirrors><mirror><id>central-repo</id><name>Maven Central Mirror</name><url>https://repo1.maven.org/maven2</url><mirrorOf>central</mirrorOf></mirror></mirrors></settings>' > /root/.m2/settings.xml

# Copy pom.xml and download dependencies (with retries)
COPY pom.xml .
RUN mvn -Dmaven.wagon.http.retryHandler.count=5 \
        -Dmaven.wagon.httpconnectionManager.ttlSeconds=25 \
        -Dmaven.wagon.http.pool=false \
        dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:21-jdk AS runner
WORKDIR /app
COPY --from=builder /app/target/*.jar ./app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
