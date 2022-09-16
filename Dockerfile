FROM openjdk:17.0.2-slim
COPY build/libs/*.jar app.jar
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar /app.jar"]