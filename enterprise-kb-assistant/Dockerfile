# Build stage: full Maven with JDK, produces the Quarkus runner jar.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests

# Run stage: JRE only, non-root, ONNX models on a mounted volume so a
# rebuild does not re-download 137MB of weights.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 kb && mkdir -p /home/kb/.cache/kb-models && chown -R kb /home/kb
COPY --from=build /workspace/target/quarkus-app/lib/ /app/lib/
COPY --from=build /workspace/target/quarkus-app/*.jar /app/
COPY --from=build /workspace/target/quarkus-app/app/ /app/app/
COPY --from=build /workspace/target/quarkus-app/quarkus/ /app/quarkus/
COPY corpus /app/corpus
COPY eval /app/eval
USER kb
ENV HOME=/home/kb
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]