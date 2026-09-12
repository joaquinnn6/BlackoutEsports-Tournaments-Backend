# Blackout Esports - Backend de torneos

Microservicio CRUD independiente para torneos. Java 21, Spring Boot 3.5, Maven,
Spring Data JPA, PostgreSQL 16, validaciones, Swagger/OpenAPI y Docker.
Sigue la estructura del servicio de jugadores y utiliza puertos y almacenamiento propios.

## Ramas

- `main`: base del repositorio.
- `develop`: base para integrar cambios revisados.
- `feature-tournaments`: implementación del CRUD y pruebas, pendiente de integración.

## Ejecutar

Selecciona la rama `feature-tournaments`. Desde la raíz del repositorio:

```sh
docker compose up --build
```

API: http://localhost:8082/api/tournaments

Swagger: http://localhost:8082/swagger-ui.html

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

Las pruebas usan H2 en modo PostgreSQL y cubren CRUD, validación, duplicados,
errores 400/404, CORS y documentación OpenAPI. El build Docker también ejecuta las pruebas.
H2 no reemplaza la comprobación de despliegue con PostgreSQL real.

## Alcance

Este repositorio contiene el CRUD de torneos. El frontend y el backend de jugadores no se modifican.
La conexión del panel administrativo queda pendiente. CORS admite por defecto los orígenes locales
5173, 3000 y 3001, personalizables con `CORS_ORIGINS`.

Al igual que el backend de jugadores actual, esta versión todavía no valida Access Tokens de Entra ID:
sus endpoints no exigen autenticación. La validación JWT, roles, BFF, API Gateway y despliegue en EC2
son trabajo pendiente para cumplir el encargo académico; este CRUD por sí solo no completa esa entrega.
