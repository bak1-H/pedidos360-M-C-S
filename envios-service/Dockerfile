FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY bff/pom.xml bff/pom.xml
COPY usuarios-service/pom.xml usuarios-service/pom.xml
COPY pedidos-service/pom.xml pedidos-service/pom.xml
COPY envios-service/pom.xml envios-service/pom.xml
RUN chmod +x mvnw && ./mvnw -pl envios-service -q dependency:go-offline
COPY envios-service/src envios-service/src
RUN ./mvnw -pl envios-service -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/envios-service/target/envios-service-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
