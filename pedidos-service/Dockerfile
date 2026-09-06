FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY bff/pom.xml bff/pom.xml
COPY usuarios-service/pom.xml usuarios-service/pom.xml
COPY pedidos-service/pom.xml pedidos-service/pom.xml
COPY envios-service/pom.xml envios-service/pom.xml
RUN chmod +x mvnw && ./mvnw -pl pedidos-service -q dependency:go-offline
COPY pedidos-service/src pedidos-service/src
RUN ./mvnw -pl pedidos-service -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/pedidos-service/target/pedidos-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
