FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Download Elastic APM Java agent
RUN apk add --no-cache curl
ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/1.46.0/elastic-apm-agent-1.46.0.jar /elastic-apm-agent.jar
# Copy Spring Boot jar
COPY build/libs/*.jar app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
