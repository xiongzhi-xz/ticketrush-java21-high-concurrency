# Runtime stage only — JAR is built locally with JDK 21 before docker compose up
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV JAVA_OPTS="--enable-preview -XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Shanghai"

COPY target/ticketrush-0.0.1-SNAPSHOT.jar /app/ticketrush.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/ticketrush.jar"]
