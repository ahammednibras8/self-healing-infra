FROM openjdk:21-jdk-slim

WORKDIR /app

COPY opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

COPY target/service-a-1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT [ "java", "-javaagent:/app/opentelemetry-javaagent.jar", "-Dserver.port=8080", "-jar", "app.jar" ]