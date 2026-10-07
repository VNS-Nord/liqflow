# LiqFlow

> [!IMPORTANT]
> **Project status: work in progress, not production-ready.** The core API is functional.

LiqFlow is a backend inventory-management API for tracking products and current stock levels across multiple locations. It also models transfer orders that move stock between a source and a target location through an explicit lifecycle.

## 30-second summary

| Question | Answer |
|----------|--------|
| What problem does it address? | Keeping product availability consistent when the same product is stocked in several warehouses, hubs, or stores. |
| What is the main workflow? | Create products and locations, maintain per-location inventory, create a transfer order, and move stock when the order is completed. |
| What is implemented? | REST endpoints, domain rules, PostgreSQL persistence, Flyway migrations, validation, error handling, locking, and automated tests. |
| How is it structured? | As a monolithic Spring Boot application organized package-by-feature: `product`, `location`, `inventory`, and `transfer`, each owning its controller, service, entity, repository, mapper, and DTOs, plus a shared `common/exception` package. |

## Implemented functionality

### Business capabilities

- Create, list, update, and delete products.
- Create, list, update, and delete locations.
- Track physical, reserved, available, and low-threshold quantities for each product at each location.
- Add or deduct physical stock.
- Find inventory by location or by product and location.
- Find inventory whose available quantity is at or below its configured minimum.
- Create transfer orders and add or remove items while an order is a draft.
- Submit, mark in transit, complete, or cancel transfer orders.
- Reserve source stock when an order is submitted, and release it again if the order is cancelled.
- Move every item in a transfer atomically when the order is completed.

### Technical highlights

- Java 21 records and constructor injection at HTTP boundaries.
- Spring MVC controllers with Jakarta Bean Validation.
- Transactional service methods for inventory and transfer operations.
- Domain entities that own inventory invariants and transfer-state rules.
- JPA optimistic locking through entity versions, plus pessimistic locks acquired in a fixed order during transfer completion.
- Flyway-managed schema changes with Hibernate schema validation.
- Database constraints for unique SKUs, location codes, transfer-order numbers, and product/location inventory pairs.
- Centralized handling of validation, domain, not-found, duplicate-data, and persistence-conflict errors.
- Mockito service tests, MockMvc web tests, and PostgreSQL integration tests.

## Architecture

LiqFlow is a monolithic Spring Boot application organized package-by-feature. Each business capability keeps its controller, service, entity, repository, mapper, and request/response DTOs in one package, so a feature can be read without jumping between layer packages. Only cross-cutting error handling is shared, under `common/exception`.

Request flow within a feature:

```text
HTTP client
    ↓
Feature controller (e.g. `inventory/InventoryController`)
    ↓
Transactional feature service (e.g. `inventory/InventoryService`)
    ↓
Feature entity and Spring Data repository
    ↓
PostgreSQL
```

Flyway prepares the PostgreSQL schema when the application starts.

Key code locations:

- [`product/`](src/main/java/com/vnsnord/liqflow/product) — products: controller, service, entity, repository, mapper, and request/response contracts.
- [`location/`](src/main/java/com/vnsnord/liqflow/location) — locations and the [`LocationType`](src/main/java/com/vnsnord/liqflow/location/LocationType.java) enum, in the same controller/service/entity/repository/DTO shape.
- [`inventory/`](src/main/java/com/vnsnord/liqflow/inventory) — per-location stock, including the low-stock query and the stock movements; domain behavior lives in [`Inventory.java`](src/main/java/com/vnsnord/liqflow/inventory/Inventory.java).
- [`transfer/`](src/main/java/com/vnsnord/liqflow/transfer) — transfer orders, items, and reservations; transfer completion is coordinated in [`TransferOrderService.java`](src/main/java/com/vnsnord/liqflow/transfer/TransferOrderService.java), with domain rules in [`TransferOrder.java`](src/main/java/com/vnsnord/liqflow/transfer/TransferOrder.java) and [`TransferOrderReservation.java`](src/main/java/com/vnsnord/liqflow/transfer/TransferOrderReservation.java).
- [`common/exception/`](src/main/java/com/vnsnord/liqflow/common/exception) — typed errors, API error responses, and the global exception handler shared by every feature.
- [`db/migration/`](src/main/resources/db/migration) — schema creation and demo data; the initial constraints are in [`V1__init_schema.sql`](src/main/resources/db/migration/V1__init_schema.sql).

## Technology

- Java 21
- Spring Boot 4.1.1
- Spring MVC and Spring Data JPA
- PostgreSQL
- Flyway
- Jakarta Bean Validation
- JUnit, Mockito, MockMvc, and Spring integration tests
- Maven Wrapper

## Running locally

### Prerequisites

- JDK 21
- A PostgreSQL database
- Docker is optional and is only used here to start PostgreSQL

The default configuration expects PostgreSQL at `localhost:5432`, database `liqflow`, with username/password `postgres`/`postgres`. The credentials can be overridden with `DB_USERNAME` and `DB_PASSWORD`.

One way to start PostgreSQL is:

```sh
docker run --rm -d \
  --name liqflow-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=liqflow \
  -p 5432:5432 \
  postgres:18
```

Start the application:

```sh
./mvnw spring-boot:run
```

The API is then available under `http://localhost:8080/api/v1`.

The `dev` profile is the default. It enables SQL logging and Hikari connection-leak detection. Flyway creates the schema and loads demo data into a fresh database.

The application only ever talks to `liqflow`. Tests use a separate database, see [Tests](#tests).

## API overview

All endpoints use the `/api/v1` prefix.

### Products

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/products` | Create a product |
| `GET` | `/products` | List products with pagination |
| `GET` | `/products/{id}` | Get a product by ID |
| `GET` | `/products/sku/{sku}` | Get a product by SKU |
| `PUT` | `/products/{id}` | Update a product; SKU is immutable |
| `DELETE` | `/products/{id}` | Delete a product when it is not referenced |

### Locations

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/locations` | Create a location |
| `GET` | `/locations` | List locations with pagination |
| `GET` | `/locations/{id}` | Get a location by ID |
| `GET` | `/locations/code/{code}` | Get a location by code |
| `PUT` | `/locations/{id}` | Update a location; code and type are immutable |
| `DELETE` | `/locations/{id}` | Delete a location when it is not referenced |

Location types are `CENTRAL_WAREHOUSE`, `REGIONAL_HUB`, and `STORE`.

### Inventory

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/inventories` | Create a product/location inventory record |
| `GET` | `/inventories` | List all inventory records |
| `GET` | `/inventories/low-stock` | List records at or below their available-stock threshold |
| `GET` | `/inventories/location/{locationId}` | List a location's inventory with pagination |
| `GET` | `/inventories/product/{productId}/location/{locationId}` | Get one product at one location |
| `GET` | `/inventories/{id}` | Get an inventory record by ID |
| `POST` | `/inventories/{id}/add-stock` | Add physical stock |
| `POST` | `/inventories/{id}/deduct-stock` | Deduct physical stock |
| `PUT` | `/inventories/{id}/min-threshold` | Change the low-stock threshold |

Inventory supports create and read operations plus explicit stock operations. There is no inventory delete endpoint, and the only field that can be edited directly is the low-stock threshold.

`minThreshold` is editable because it is a configuration value, independent of how much stock happens to be on hand, so retuning it can never put the quantity out of step with the reservations held against it. The physical quantity is deliberately not editable: it may only move through `add-stock`, `deduct-stock`, and transfer orders.

There is deliberately no endpoint to reserve or release stock directly. `reservedQuantity` changes only as a side effect of a transfer order's lifecycle, so it always matches the sum of the `HELD` rows in `transfer_order_reservations`. Allowing it to be edited on its own would let a caller reduce the reserved quantity below what a submitted order has already claimed, and that order would then fail when it reached `IN_TRANSIT`.

### Transfer orders

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/transfer-orders` | Create a draft transfer order |
| `GET` | `/transfer-orders` | List transfer orders with pagination |
| `GET` | `/transfer-orders/{id}` | Get an order and its items |
| `POST` | `/transfer-orders/{id}/items` | Add or merge an item in a draft |
| `DELETE` | `/transfer-orders/{id}/items/{itemId}` | Remove an item from a draft |
| `POST` | `/transfer-orders/{id}/submit` | Submit a non-empty draft and reserve its source stock |
| `POST` | `/transfer-orders/{id}/in-transit` | Mark a submitted order in transit |
| `POST` | `/transfer-orders/{id}/complete` | Move stock and complete the order |
| `POST` | `/transfer-orders/{id}/cancel` | Cancel an order that is not completed |

Paginated endpoints accept zero-based `page`, `size`, and `sort` query parameters. Their defaults are `page=0` and `size=20`; sorting is opt-in, for example `sort=createdAt,desc`. The global inventory and low-stock endpoints return unpaged lists.

## Example workflow

The following example assumes the application is running and that the UUIDs in requests are replaced with IDs returned by the API.

Create a product:

```sh
curl -X POST http://localhost:8080/api/v1/products \
  -H 'Content-Type: application/json' \
  -d '{
    "sku": "SKU-README-001",
    "name": "Demo widget",
    "description": "Used by the README walkthrough",
    "price": 19.90
  }'
```

Create a source and target location:

```sh
curl -X POST http://localhost:8080/api/v1/locations \
  -H 'Content-Type: application/json' \
  -d '{
    "code": "SRC-README-001",
    "name": "Source warehouse",
    "type": "CENTRAL_WAREHOUSE"
  }'

curl -X POST http://localhost:8080/api/v1/locations \
  -H 'Content-Type: application/json' \
  -d '{
    "code": "DST-README-001",
    "name": "Target store",
    "type": "STORE"
  }'
```

Create inventory at both locations before completing the transfer:

```sh
curl -X POST http://localhost:8080/api/v1/inventories \
  -H 'Content-Type: application/json' \
  -d '{
    "locationId": "<source-location-uuid>",
    "productId": "<product-uuid>",
    "initialStock": 20,
    "minThreshold": 10
  }'

curl -X POST http://localhost:8080/api/v1/inventories \
  -H 'Content-Type: application/json' \
  -d '{
    "locationId": "<target-location-uuid>",
    "productId": "<product-uuid>",
    "initialStock": 0,
    "minThreshold": 5
  }'
```

Create a draft order, add an item, and move it through its lifecycle:

```sh
curl -X POST http://localhost:8080/api/v1/transfer-orders \
  -H 'Content-Type: application/json' \
  -d '{
    "orderNumber": "ORDER-README-001",
    "sourceLocationId": "<source-location-uuid>",
    "targetLocationId": "<target-location-uuid>"
  }'

curl -X POST http://localhost:8080/api/v1/transfer-orders/<order-uuid>/items \
  -H 'Content-Type: application/json' \
  -d '{
    "productId": "<product-uuid>",
    "quantity": 15
  }'

curl -X POST http://localhost:8080/api/v1/transfer-orders/<order-uuid>/submit
curl -X POST http://localhost:8080/api/v1/transfer-orders/<order-uuid>/in-transit
curl -X POST http://localhost:8080/api/v1/transfer-orders/<order-uuid>/complete
```

After completion, the source has `5` units and the target has `15` units. Both changes commit in one database transaction.

## Domain rules and consistency

### Inventory

Each inventory record represents one product at one location and stores:

- `quantity`: physical units currently present.
- `reservedQuantity`: units marked as unavailable for another purpose.
- `availableQuantity`: `quantity - reservedQuantity`.
- `minThreshold`: the level used by the low-stock query.

The required inventory invariant is `0 <= reservedQuantity <= quantity`; non-negative stored values alone are not sufficient to guarantee `availableQuantity >= 0`. The current domain operations are written to preserve this invariant from a valid starting state, but the database does not enforce it with `CHECK` constraints.

Stock can be added, deducted, reserved, released, or consumed through explicit operations.

`deductStock` removes physical stock for corrections such as shrinkage or a stock-take. It can only take from the available quantity, so it can never reduce a reservation that a pending order is relying on.

`consumeReservedStock` is the transfer-specific path. It rejects an amount greater than the reserved quantity and only ever removes units that are already reserved, so it cannot reach into stock held on behalf of another order.

### Transfer orders

A transfer follows this state machine:

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    DRAFT --> SUBMITTED: submit
    SUBMITTED --> IN_TRANSIT: mark in transit
    IN_TRANSIT --> COMPLETED: complete
    DRAFT --> CANCELLED: cancel
    SUBMITTED --> CANCELLED: cancel
    IN_TRANSIT --> CANCELLED: cancel
```

- The source and target must be different locations.
- Items can only be added, merged, or removed while an order is `DRAFT`.
- Empty orders cannot be submitted.
- `DRAFT`, `SUBMITTED`, and `IN_TRANSIT` orders can transition to the terminal `CANCELLED` state.
- `COMPLETED` and `CANCELLED` orders cannot transition again.
- Completing an order changes all source and target inventory rows in one transaction.
- Inventory rows touched by a transfer are locked pessimistically, always in the same location-then-product order, so two transfers that share products cannot deadlock against each other. Any remaining contention surfaces as a `409` and is safe to retry.
- Inventory and transfer entities also use optimistic versions to detect conflicting updates.

### Reservations

Submitting an order reserves the source stock it needs, so those units stop being available to any other order. Each line item gets one reservation row recording the product, source location, quantity, and status:

```mermaid
stateDiagram-v2
    [*] --> HELD: submit
    HELD --> CONSUMED: order completed
    HELD --> RELEASED: order cancelled
```

- Submission fails if the source location has no inventory record for an item, or has insufficient available stock.
- Completing a transfer consumes only the quantity that order reserved, and adds the same quantity to the target location. A transfer can therefore never take stock that another order is holding.
- Cancelling a transfer releases any reservation still held, returning the units to the available quantity.
- A reservation settles exactly once, and terminal reservations are kept as history.

Because `reservedQuantity` is only ever changed alongside a reservation row, the two stay in agreement: the reserved quantity on any inventory record equals the sum of its `HELD` reservations. That is what makes a submitted order safe to promise, since no other operation can take the units back out from under it.

This is why submission validates stock rather than only completion: once a unit is reserved for one order, no other transfer can take it, so concurrent orders compete for stock at the moment of submission instead of racing at completion.

Stock corrections cannot undermine this either. `deductStock` refuses to remove units that are reserved, so a shrinkage write-off can never strand a submitted order. If stock genuinely has to be written off and only reserved units remain, the affected order must be cancelled first, which releases the reservation and makes the units available again.

## Tests

Tests run against a dedicated `liqflow_test` database so they never touch your development data. Create it once:

```sh
docker exec liqflow-postgres psql -U postgres -c "CREATE DATABASE liqflow_test"
```

Or, if PostgreSQL runs natively on your machine:

```sql
CREATE DATABASE liqflow_test;
```

Flyway migrates `liqflow_test` automatically on the first test run, so no manual migration step is needed. The connection is configured in [`src/test/resources/application.yaml`](src/test/resources/application.yaml) and can be overridden with `TEST_DB_URL`, `TEST_DB_USERNAME`, and `TEST_DB_PASSWORD` (falling back to `DB_USERNAME`/`DB_PASSWORD`). That file shadows the main `application.yaml` on the test classpath, so it is self-contained.

Run the full suite with:

```sh
./mvnw test
```

The test suite uses:

- Mockito unit tests for service behavior.
- MockMvc tests for controller routing, validation, serialization, and status codes.
- Focused tests for centralized exception handling.
- A [`@SpringBootTest` integration test](src/test/java/com/vnsnord/liqflow/inventory/InventoryFlowIntegrationTest.java) backed by the `liqflow_test` database. Each service call commits on its own, so the tests exercise real transaction boundaries rather than a single rolled-back fixture.
- Integration coverage for the reservation rules: two orders competing for the same stock, cancellation returning a hold to available quantity, and an order completing without consuming another order's reservation.
- A concurrency test that completes two transfers in opposite directions simultaneously, on two real connections, and asserts both commits succeed. Removing the deterministic lock order makes this test fail on the first round with a PostgreSQL `deadlock detected`, so the claim is guarded rather than asserted.

Unit, web, and integration tests sit next to the feature they cover, under [`product/`](src/test/java/com/vnsnord/liqflow/product), [`location/`](src/test/java/com/vnsnord/liqflow/location), [`inventory/`](src/test/java/com/vnsnord/liqflow/inventory), and [`transfer/`](src/test/java/com/vnsnord/liqflow/transfer); shared error-handling tests are in [`exception/`](src/test/java/com/vnsnord/liqflow/exception).

The full suite requires the PostgreSQL instance described in [Running locally](#running-locally), plus the `liqflow_test` database. Tests fail rather than skip when either is missing.

To reset the test database to a clean state, drop and recreate it, then run the suite once to re-apply migrations.

## License

This repository is licensed under the [GNU Affero General Public License v3.0](LICENSE).
