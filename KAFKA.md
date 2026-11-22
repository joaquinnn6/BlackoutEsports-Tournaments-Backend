# Partidos en vivo — entrega 3

## Implementación

El frontend feature-entrega-3-kafka incluye Academy y una vista #partidos. Admin o Staff crea un partido asociado a un torneo existente, lo inicia, asigna cada ronda a A o B, anula la última ronda con una confirmación y finaliza cuando existe un ganador. La anulación no pide nombre ni envía un campo de autor. Los visitantes consultan el marcador público y las victorias por equipo cada dos segundos. La página de administración lee el estado confirmado por la base de datos. La página pública lee la proyección procesada por Kafka, por lo que refleja el retraso del consumidor.

El paquete live del microservicio Torneos implementa modelo, repositorios, comandos, outbox y dos consumidores. En esta versión los consumidores se ejecutan dentro del proceso Torneos y usan su PostgreSQL. kafka-admin es otro proceso Spring Boot, con su propio pom.xml, Dockerfile y estructura src/main. Se encuentra en este mismo repositorio.

## Comandos HTTP

- GET /api/matches — proyección pública del marcador.
- GET /api/matches/statistics — resultados por partido de la segunda proyección. La web agrega victorias por nombre de equipo.
- GET /api/matches/manage — estado de escritura, sólo Admin/Staff.
- POST /api/matches — crea partido con tournamentId, teamA, teamB y game.
- POST /api/matches/{id}/commands — action START, A, B, REVERT o FINISH y expectedVersion.

SecurityConfig valida JWT y roles antes de escribir. expectedVersion compara el estado que vio el admin con el actual bajo bloqueo de fila. Una petición repetida con versión antigua recibe 409. Los estados son SCHEDULED, LIVE y FINISHED. Una ronda suma un punto. Sólo se anula la última vigente y sólo durante LIVE. Un empate impide finalizar.

## Evento y persistencia

MatchEvent es un DTO con eventId, schemaVersion=1, eventType, matchId, matchVersion, tournamentId, nombres, videojuego, estado, rondas vigentes y occurredAt. Los tipos son CREATED, STARTED, ROUND_WON, ROUND_REVERTED y FINISHED. Cada evento lleva una instantánea completa del partido después de aplicar el cambio. Eso permite reconstruir el marcador incluso si se recupera un evento antiguo después de procesar una versión posterior.

MatchService guarda el cambio y una fila en match_outbox en la misma transacción. KafkaOutboxPublisher toma una fila pendiente ordenada por id, publica con clave matchId y espera confirmación antes de marcarla publicada. Ante un fallo la transacción conserva el pendiente. La outbox publicada conserva el historial de cambios, incluyendo las anulaciones. Se debe definir su archivado si crece el uso.

## Tópicos y consumidores

| Tópico | Particiones | Réplicas | Consumidor |
|---|---|---|---|
| blackout.matches.events | 3 | 3 | blackout-scoreboard |
| blackout.matches.scoreboard | 3 | 3 | blackout-match-statistics |

Ambos usan cleanup.policy=delete y retention.ms=604800000 (7 días). Cada uno tiene un tópico .DLT de las mismas dimensiones para mensajes fallidos. KafkaConfiguration crea los cuatro tópicos. Los dos primeros transportan el DTO tipado serializado como JSON. El segundo conserva la instantánea del marcador con su versión y rondas, de la que se obtienen los puntajes. Los tópicos .DLT conservan el payload original y cabeceras de error.

La clave matchId mantiene los eventos del partido en la misma partición con un número de particiones estable. El productor configura acks=all y enable.idempotence=true. Cada tópico exige min.insync.replicas=2. Hay tres brokers y tres ZooKeeper en docker-compose.kafka.yml, usando imágenes Confluent 7.7.13 compatibles con ZooKeeper. Los procesos comparten el host cuando se usa este Compose. La pérdida de ese host afecta al clúster completo.

MatchKafka recibe texto y lo convierte con ObjectMapper a MatchEvent. ProjectionService valida el DTO y actualiza match_projections. La proyección SCORE publica a su vez una fila outbox para el segundo tópico en la misma transacción. La proyección STAT permite consultar resultados para estadísticas. Las versiones antiguas o repetidas se omiten. El bloqueo optimista evita sobrescribir un estado con cambios concurrentes.

Los offsets se confirman por registro después del listener. El consumidor usa tres hilos, uno por partición cuando están disponibles. El grupo de marcadores y el de estadísticas tienen offsets independientes. DefaultErrorHandler aplica dos reintentos con pausa de un segundo tras el intento inicial para fallos reintentables. Los fallos considerados fatales por Spring pueden ir directamente a .DLT. DeadLetterPublishingRecoverer exige confirmar la publicación del mensaje fallido. Si falla esa publicación, el error se propaga para conservar la posibilidad de recuperación.

## Administrador Kafka

kafka-admin escucha en 8086, publicado sólo en loopback. Usa AdminClient y valida JWT Entra con rol Admin o Staff. Permite listar y crear tópicos, consultar particiones/líder/réplicas sincronizadas, consultar y modificar retención, eliminar tópicos Blackout, listar grupos y consultar offsets y lag. Crear un tópico requiere nombre blackout.*, de 1 a 12 particiones y de 1 a 3 réplicas. La retención editable va de un minuto a 30 días. La configuración de los tópicos funcionales se declara en código. Incrementar particiones y modificar réplicas no están implementados en este administrador.

Kafka UI se publica en 127.0.0.1:8090 para acceder mediante túnel SSH. Los brokers tienen listeners internos kafka1..3:9092 y listeners loopback 19091..19093 para pruebas ejecutadas en el host. Los mensajes no requieren abrir esos puertos a Internet.

## Puesta en marcha

Desde la carpeta de Torneos, con la red Docker blackout-network existente y las variables Entra del proyecto, ejecutar docker compose -f docker-compose.kafka.yml up -d --build. Esperar a que los brokers estén healthy. En .env de Torneos activar KAFKA_ENABLED=true y reconstruir tournaments-service. El servicio usa los tres nombres internos de broker. El frontend conserva VITE_TOURNAMENTS_API_URL del gateway, con el prefijo de Torneos. Una nueva rama Amplify requiere configurar sus variables y URLs de retorno en ambos proveedores de identidad.

En esta sesión se inspeccionó la EC2 existente y sigue siendo t3.small. La capacidad para desplegar todo el conjunto en ese host debe medirse antes de la puesta en marcha. El Compose deja persistentes los volúmenes. No usar down -v con datos reales.

## Pruebas y presentación

LiveMatchTests recorre creación, inicio, ronda, rechazo de versión repetida, bloqueo de fan, anulación, finalización y procesamiento de eventos repetidos/antiguos. KafkaClusterTests se ejecuta en GitHub Actions con kafka.integration=true y los tres brokers reales levantados por Compose. Publica dos veces un evento y espera que ambas proyecciones terminen con el marcador correcto. Las pruebas del frontend verifican que cancelar la confirmación no envía un comando y que un visitante no ve controles administrativos.

Para demostrar: abrir un navegador admin y otro público, iniciar un partido y sumar rondas. Mostrar los eventos y matchId en Kafka UI y la evolución de offsets/lag. Anular la última ronda con confirmación y observar el marcador. Finalizar y consultar victorias. La prueba de caída de broker y recuperación de consumidores debe ejecutarse en el entorno desplegado antes de presentarla como verificada.

## Pendiente de verificar en AWS

Despliegue en EC2, memoria disponible, conexión a los tópicos desde el servicio desplegado, origen CORS/identidad en Amplify y recorrido real de admin a espectador. Este cambio incorpora Kafka al caso de partidos. El correo real de Academy y los requisitos pendientes del clúster RabbitMQ requieren su propia verificación.
