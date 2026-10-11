FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY bff/pom.xml bff/pom.xml
COPY usuarios-service/pom.xml usuarios-service/pom.xml
COPY pedidos-service/pom.xml pedidos-service/pom.xml
COPY envios-service/pom.xml envios-service/pom.xml
COPY notificaciones-service/pom.xml notificaciones-service/pom.xml
COPY rabbit-admin-service/pom.xml rabbit-admin-service/pom.xml
RUN chmod +x mvnw && ./mvnw -pl rabbit-admin-service -q dependency:go-offline
COPY rabbit-admin-service/src rabbit-admin-service/src
RUN ./mvnw -pl rabbit-admin-service -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/rabbit-admin-service/target/rabbit-admin-service-1.0.0.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
