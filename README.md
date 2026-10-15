# Blackout Esports - Backend de torneos

Microservicio CRUD independiente para torneos. Java 21, Spring Boot 3.5, Maven,
Spring Data JPA, PostgreSQL 16, validaciones, Swagger/OpenAPI y Docker.
Sigue la estructura del servicio de jugadores y utiliza puertos y almacenamiento propios.

## Mensajería RabbitMQ

Al crear un torneo, el servicio guarda primero el registro y publica el evento
`tournament.created` en el exchange `blackout.events.exchange`. La clave coincide con
el binding `tournament.#` de `fan.notifications.queue`. Los nombres de exchanges,
colas, bindings y routing keys están centralizados en `application.yml` y se cargan
mediante `RabbitMessagingProperties`; no se utilizan literales dispersos en la lógica de negocio.

La publicación está encapsulada en `TournamentEventPublisher`. Si el broker no está disponible,
el CRUD conserva el torneo y registra el error para no alterar la operación principal. Para
ejecutar pruebas sin RabbitMQ se configura `app.rabbit.enabled=false` en el perfil de pruebas.

Antes de iniciar este Compose debe estar creada la red Docker `blackout-network` y en ejecución
el contenedor `blackout-rabbitmq` del repositorio administrador de RabbitMQ.

## Ramas

- `main`: base del repositorio.
- `develop`: base para integrar cambios revisados.
- `feature-tournaments`: implementación original del CRUD.
- `feature-seguridad-jwt`: autenticación JWT y permisos de Entra/Cognito.

## Ejecutar

Selecciona la rama `feature-seguridad-jwt`. Desde la raíz del repositorio:

```sh
docker compose up --build
```

API: http://localhost:8082/api/tournaments

Swagger: http://localhost:8082/swagger-ui.html

Pulsa **Authorize** e ingresa un Access Token válido antes de probar los endpoints.

PostgreSQL: `localhost:5434`, base `tournaments_db`. Los datos se conservan en un volumen Docker.
El servicio de jugadores puede seguir usando `8081` y `5433`.

Para personalizar los valores, copia `.env.example` a `.env` y ajusta sus variables.
Los valores `postgres/postgres` son exclusivamente valores de desarrollo local.
Para detener los contenedores conservando los datos: `docker compose down`.

Sin Docker: Java 21, Maven 3.9 y PostgreSQL con la base `tournaments_db` creada.
Configura `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` y ejecuta
`mvn spring-boot:run` dentro de `blackout-tournaments/`.
Las variables del archivo `.env` son leídas por Compose; para Maven deben exportarse en la terminal.

## API

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/api/tournaments` | Lista de torneos, 200 |
| GET | `/api/tournaments/{id}` | Torneo por ID, 200 o 404 |
| POST | `/api/tournaments` | Crear torneo, 201 y cabecera Location |
| PUT | `/api/tournaments/{id}` | Actualizar todos los campos, 200 o 404 |
| DELETE | `/api/tournaments/{id}` | Eliminar torneo, 204 o 404 |

Todos los `GET` requieren un Access Token válido de Entra ID o Cognito. `POST`, `PUT` y `DELETE`
requieren un token de Entra con rol de aplicación `Admin` o `Staff`; un fan de Cognito es de solo lectura.

El backend valida firma, emisor, vencimiento y sujeto. Entra además debe incluir la audiencia configurada,
el scope `access_as_user` y el rol necesario para escritura. Cognito debe incluir `token_use=access` y el
`client_id` configurado. Un token de Microsoft Graph no se acepta como token de esta API.

Configura `ENTRA_ISSUER`, `ENTRA_AUDIENCE`, `ENTRA_SCOPE`, `COGNITO_ISSUER` y `COGNITO_CLIENT_ID`
en `.env`; `.env.example` contiene el formato esperado. El frontend administrativo debe solicitar el scope
propio `api://<id-aplicacion-api>/access_as_user`, separado de cualquier scope de Microsoft Graph.

Ejemplo para POST o PUT:

```json
{
  "name": "OpsLeague",
  "game": "Valorant",
  "status": "En curso",
  "date": "EN VIVO",
  "teams": "16 equipos"
}
```

Los cinco campos son obligatorios. `id` es numérico y se genera en el servidor.
Se conservan los nombres y tipos de los campos actuales de `CompetitionContext` en FrontAdmin.
`date` es texto de presentación (por ejemplo `24 SEP 2026` o `EN VIVO`), no una fecha para cálculos.
`teams` es texto de presentación (por ejemplo `16 equipos`), no una relación con equipos.
El cliente debe guardar el nuevo `id` y usarlo en modificaciones y eliminaciones, en lugar del nombre.
No se permiten torneos con el mismo nombre y juego, ignorando mayúsculas y espacios exteriores.
Validaciones: 400. Duplicados: 409. Los errores se devuelven como ProblemDetail con detalles de campos.
La base comienza vacía; no se reinsertan torneos eliminados al reiniciar.

## Pruebas

Dentro de `blackout-tournaments/`:

```sh
mvn clean verify
```

Las pruebas usan H2 en modo PostgreSQL y tokens RSA locales. Cubren CRUD, validación, duplicados,
errores 400/401/403/404, roles, firma, emisor, audiencia, scope, vencimiento, claims de Cognito,
CORS y documentación OpenAPI. El build Docker también ejecuta las pruebas.
H2 no reemplaza la comprobación de despliegue con PostgreSQL real.

## Alcance

Este repositorio contiene el CRUD de torneos. El frontend y el backend de jugadores no se modifican.
La conexión del panel administrativo se realiza enviando `Authorization: Bearer <access_token>`.
CORS admite por defecto los orígenes locales 5173, 3000 y 3001, personalizables con `CORS_ORIGINS`.
API Gateway y el despliegue en AWS siguen siendo pasos de infraestructura posteriores.
