# pg-kafka-reference

Yet another employee REST app: departments and employees in PostgreSQL + Liquibase,
with employee termination processed asynchronously through Kafka.

## Prerequisites

- Java 21
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) running (used by `docker compose` and Testcontainers)

## Run

```bash
docker compose up -d        # Postgres :5432 (db sample, user dev / welcome1) and Kafka :9092
./gradlew bootRun           # app on :8080 (Liquibase migrates on startup)
```

Stop: `docker compose down`

To reset the database, see [Resetting the database](#resetting-the-database).

## Test

```bash
./gradlew test              # unit tests
./gradlew integrationTest   # Testcontainers: Postgres + Kafka
./gradlew check             # both
```

## Database setup

On the first `docker compose up` (empty `pgdata` volume), the Postgres image runs
[`docker/postgres/init-db.sh`](docker/postgres/init-db.sh) from `/docker-entrypoint-initdb.d`.
Using the `postgres` superuser (init only), it creates:

- role `dev` / `welcome1` — a regular login user, **not** a superuser
- database `sample`, owned by `dev`

The app and Liquibase connect as `dev`, so all of the tables are owned by `dev`. The script runs only on an
empty volume: after changing it (or the `APP_*` variables in `compose.yaml`),  run `docker compose down -v`.

Admin access: `docker compose exec postgres psql -U postgres -d sample`.

## Database migrations

Liquibase runs on startup from `src/main/resources/db/changelog/db.changelog-master.xml`.

To add a schema change, create `004-<description>.xml` with both blocks and include it in the master.

Never edit a changeset that has already run; add a new one instead.


## Resetting the database

Stop the app first (`Ctrl+C` on `bootRun`). Then pick how much to reset:

**1. Full reset — start completely fresh (default choice)**

```bash
docker compose down -v
docker compose up -d
./gradlew bootRun
```


**2. Clear data, keep the schema**

```bash
docker compose exec postgres psql -U dev -d sample -c "TRUNCATE employees, departments RESTART IDENTITY;"
```


**3. Rebuild the schema from the changelogs, without restarting containers**

```bash
docker compose exec postgres psql -U dev -d sample -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
./gradlew bootRun
```

Drops all tables including Liquibase's tracking tables, so the next startup re-runs every changeset.


Automated tests never use this database: each test run gets throwaway Postgres and Kafka containers 
from Testcontainers, so no reset is needed for `./gradlew check`.


## API

```bash
# Departments
curl -i -X POST localhost:8080/api/departments -H 'Content-Type: application/json' \
  -d '{"name":"Engineering","location":"Toronto"}'
curl localhost:8080/api/departments
curl localhost:8080/api/departments/1
curl localhost:8080/api/departments/1/employees

# Employees
curl -i -X POST localhost:8080/api/employees -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","jobTitle":"Engineer","hireDate":"2024-01-15","salary":85000.00,"departmentId":1}'
curl localhost:8080/api/employees
curl localhost:8080/api/employees/1

# Termination (202 Accepted; applied asynchronously by the Kafka consumer)
curl -i -X POST localhost:8080/api/employees/1/termination -H 'Content-Type: application/json' \
  -d '{"reason":"Restructuring","effectiveDate":"2026-10-31"}'
curl localhost:8080/api/employees/1     # status becomes TERMINATED
```

## Kafka

Topics: `employee-terminations` 
Dead-letter topic `employee-terminations.DLT`.

Failed message retry:  3 times and then moved to the DLT

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic employee-terminations --from-beginning --property print.key=true

docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic employee-terminations.DLT --from-beginning --property print.key=true
```
