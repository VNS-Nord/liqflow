# LiqFlow

A simple REST API for tracking product inventory across locations, stock
movements (reserve / deduct / adjust), and transfer orders between locations.

## Stack

- Java 21
- Spring Boot 4 (Web MVC, Spring Data JPA, Bean Validation)
- PostgreSQL + Flyway (schema managed via `src/main/resources/db/migration`)
- Maven (builds and tests run through the Maven Wrapper, `./mvnw`)

## Prerequisites

A running PostgreSQL instance. The default configuration in
`src/main/resources/application.yaml` connects to `localhost:5432`, database
`liqflow`, user/password `postgres`/`postgres` (overridable via `DB_USERNAME` /
`DB_PASSWORD`). A convenient way to bring one up in Docker:

```sh
docker run -d --name postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=liqflow -p 5432:5432 postgres:18
```

Flyway runs automatically on startup and applies the migrations in
`classpath:db/migration`; it is configured with `baseline-on-migrate` so it can
start from an existing, manually-populated schema.

## Running

```sh
./mvnw spring-boot:run
```

The application runs with the `dev` profile by default
(`spring.profiles.default: dev`), which enables SQL logging and connection
leak detection. Override with `--spring.profiles.active=prod` (or set
`SPRING_PROFILES_ACTIVE`) to disable the debug-only settings. There is no
`prod` profile configuration by default; copy `application-dev.yaml` to
`application-prod.yaml` and remove the debug settings if you need a tuned one.

## Tests

```sh
./mvnw test
```

The unit and service tests are pure Mockito tests and run without any external
dependency. The `@SpringBootTest` integration tests in
`src/test/java/com/vnsnord/liqflow/integration/` exercise the real persistence
layer and require the PostgreSQL instance from the prerequisites above; each
one cleans up the records it creates.

## API

All endpoints live under `/api/v1`.

| Method | Path                              | Description                         |
|--------|-----------------------------------|-------------------------------------|
| POST   | `/products`                       | Create a product                    |
| GET    | `/products`                       | Paginated product list              |
| GET    | `/products/{id}`                  | Product by id                       |
| GET    | `/products/sku/{sku}`             | Product by SKU                      |
| PUT    | `/products/{id}`                  | Update a product                    |
| DELETE | `/products/{id}`                  | Delete a product                    |
| POST   | `/locations`                      | Create a location                   |
| GET    | `/locations`                      | Paginated location list             |
| GET    | `/locations/{id}`                 | Location by id                      |
| GET    | `/locations/code/{code}`          | Location by code                    |
| PUT    | `/locations/{id}`                 | Update a location                   |
| DELETE | `/locations/{id}`                 | Delete a location                   |
| POST   | `/inventory`                      | Create an inventory record          |
| GET    | `/inventory`                      | Paginated inventory list            |
| GET    | `/inventory/low-stock`            | Products below the minimum threshold|
| GET    | `/inventory/{id}`                 | Inventory record by id              |
| PUT    | `/inventory/{id}`                 | Update an inventory record          |
| DELETE | `/inventory/{id}`                 | Delete an inventory record          |
| POST   | `/transfer-orders`                | Create a transfer order (draft)     |
| GET    | `/transfer-orders`                | Paginated transfer-order list       |
| GET    | `/transfer-orders/{id}`           | Transfer order by id                |
| POST   | `/transfer-orders/{id}/items`     | Add an item to a draft order        |
| DELETE | `/transfer-orders/{id}/items/{itemId}` | Remove an item from a draft order |
| POST   | `/transfer-orders/{id}/submit`    | Submit a draft                      |
| POST   | `/transfer-orders/{id}/in-transit`| Mark a submitted order in transit   |
| POST   | `/transfer-orders/{id}/complete`  | Complete and move stock             |
| POST   | `/transfer-orders/{id}/cancel`    | Cancel an order                     |

### Transfer-order lifecycle

A transfer order progresses `DRAFT → SUBMITTED → IN_TRANSIT → COMPLETED` and
can be cancelled from any state except `COMPLETED`. Items can only be added or
removed while the order is in `DRAFT`; submitting an empty order is rejected.
Completing an order deducts the ordered quantities from the source inventory
and adds them to the target inventory in a single transaction.

### Stock invariants

- Available quantity (`quantity − reservedQuantity`) never drops below zero;
  `deductStock` clamps to the available units.
- Physical (`quantity`) and reserved quantities always stay non-negative.
- Transfer completion obtains pessimistic locks on the affected inventory rows
  so concurrent transfers of the same product/location cannot interleave into
  an inconsistent state.

## Domain rules encoded in the model

- `Inventory` exposes reserve/release/deduct/add operations that validate and
  mutate a single record; the mapper no longer has to clamp computed values.
- `TransferOrder` owns its line items and enforces state transitions and the
  same-source/target rule; items can be merged for an already-listed product.
- Uniqueness of product SKUs, location codes, and transfer-order numbers is
  enforced at both the application layer (409 on duplicates) and the database
  (unique constraints, validated on startup by Hibernate `ddl-auto: validate`).

## Configuration

`src/main/resources/application.yaml` holds the base configuration (data
source, JPA, Flyway). Debug-only settings (SQL logging, connection leak
detection) live in `src/main/resources/application-dev.yaml` and are active by
default. The datasource is a Hikari pool with batching enabled for bulk
inserts/updates (`reWriteBatchedInserts`, `batch_size`).
