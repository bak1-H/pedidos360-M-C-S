FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY bff/pom.xml bff/pom.xml
COPY usuarios-service/pom.xml usuarios-service/pom.xml
COPY pedidos-service/pom.xml pedidos-service/pom.xml
COPY envios-service/pom.xml envios-service/pom.xml
RUN chmod +x mvnw && ./mvnw -pl bff -q dependency:go-offline
COPY bff/src bff/src
RUN ./mvnw -pl bff -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/bff/target/bff-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
