# MySQL–Redis Benchmark Suite
Batch-Based Storage Performance Evaluation Framework (Spring Boot)

This repository provides a reproducible benchmarking framework for comparing MySQL (disk-backed relational database) and Redis (in-memory key–value store) under identical experimental conditions.  
The framework executes fixed-size batch operations and measures per-operation latency, batch-level averages, and workload behavior.  
All results are printed through logs (no CSV or result files are generated).

---

## Directory Structure

src/
└── main/
├── java/com/benchmark/
│   ├── core/
│   │   └── AbstractBatchExperiment.java
│   ├── mysql/
│   │   └── MysqlBatchExperiment.java
│   └── redis/
│       ├── RedisBatchExperiment.java
│       └── RedisConfig.java
│
└── resources/
├── application.yml
├── application-mysql.yml
└── application-redis.yml

---

## System Requirements

- Java 11
- Spring Boot 2.7.x
- MySQL 8.x
- Redis 7.x
- Gradle 7+
- Tested on Windows 11 + WSL2 (Linux/macOS supported)

---

## Running Experiments

### MySQL Benchmark

./gradlew bootRun --args="--spring.profiles.active=mysql"

### Redis Benchmark

./gradlew bootRun --args="--spring.profiles.active=redis"

Spring profiles determine which backend executes.

---

## Experiment Workflow

Each run evaluates a fixed-size workload:

- Total Operations (COUNT): 10,000
- Batch Size (BATCH_SIZE): 1,000
- Total Batches: 10

For each batch:

1. Generate deterministic keys
2. Execute 1,000 operations
3. Measure per-operation latency
4. Compute batch-level averages
5. Log results

### MySQL operations

- INSERT INTO test_data (...)
- SELECT * FROM test_data WHERE id = ?
- DELETE FROM test_data WHERE id = ?

### Redis operations

- SET key value
- GET key
- DEL key

The experiment structure is identical for both systems to ensure fair comparison.

---

## Configuration

### application-mysql.yml

spring:
datasource:
url: jdbc:mysql://localhost:3306/test
username: root
password:
jpa:
hibernate:
ddl-auto: create
open-in-view: false

Required MySQL setup:

CREATE DATABASE test;

---

### application-redis.yml

spring:
redis:
host: localhost
port: 6379

autoconfigure:
exclude:
- org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
- org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
- org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration

Redis requires only a running local server:

redis-server

---

## Output (Logs Only)

No result files are generated. Output includes:

- Per-operation latency
- Batch-level averages
- Completion summary

---

## Reproducibility Notes

- MySQL schema auto-generated (ddl-auto: create)
- Redis persistence disabled (pure in-memory)
- Deterministic key generation for repeatable results
- Identical batch execution flow
- Logs printed in English

---

## License

MIT License
