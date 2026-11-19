# Pedidos360

Pedidos360 es un sistema de microservicios Spring Boot (usuarios, pedidos, envíos, notificaciones y los administradores de RabbitMQ y Kafka) detrás de un BFF y un frontend Angular; se levanta completo con `docker compose up --build`, que incluye `docker-compose.kafka.yml` (3 ZooKeeper, 3 brokers y Kafka UI en `localhost:8090`) y conecta los servicios a `kafka1:9092,kafka2:9092,kafka3:9092`. Kafka transporta los eventos de dominio de forma asíncrona: pedidos publica en `pedidos360.pedidos.eventos` y envíos en `pedidos360.envios.tracking`, usando el `pedidoId` como clave para conservar el orden por pedido; cada grupo de consumidores procesa por separado, valida el payload, reintenta con backoff exponencial y envía los mensajes inválidos o agotados al tópico `.DLT` correspondiente.

| Tópico | Particiones | Réplicas | Cleanup / retención | Productor | Grupos consumidores |
| --- | --- | --- | --- | --- | --- |
| `pedidos360.pedidos.eventos` | 3 | 3 | delete / 7 días | pedidos-service | `notificaciones-analitica`, `pedidos-auditoria` |
| `pedidos360.envios.tracking` | 3 | 3 | compact,delete / 3 días | envios-service | `notificaciones-analitica`, `pedidos-tracking` |
| `pedidos360.pedidos.eventos.DLT` | 3 | 3 | delete / 14 días | pedidos-service, notificaciones-service | `notificaciones-analitica-dlt` |
| `pedidos360.envios.tracking.DLT` | 3 | 3 | delete / 14 días | pedidos-service, notificaciones-service | `pedidos-tracking-dlt`, `notificaciones-analitica-dlt` |
