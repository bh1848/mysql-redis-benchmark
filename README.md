# MySQL과 Redis의 데이터 처리 성능 비교 평가 연구

## 프로젝트 개요 및 연구 목표

본 프로젝트는 **"MySQL과 Redis의 데이터 처리 성능 비교 평가"** 라는 제목으로 한국 인터넷 정보학회지에 게재된 논문의 핵심 연구 내용을 구현한 **데이터베이스 성능 벤치마킹 실험 프로젝트**입니다.

대규모 데이터 처리 및 유지보수 환경에서, RDBMS의 대표 예인 **MySQL**과 NoSQL의 대표 예인 **Redis**를 사용하여, 데이터 처리 기능(삽입, 조회, 삭제)의 수행 시간을 정밀하게 측정하고 평가했습니다.

### 연구 목표

1.  **대표 DB 성능 비교:** MySQL과 Redis를 활용하여 데이터 처리 함수(삽입, 조회, 삭제)의 수행 시간을 측정하고 평가합니다.
2.  **구체적 성능 지표 도출:** 10,000회 실행을 1,000회 단위의 배치로 나누어 총 10회 평균 수행 시간을 측정함으로써, 대규모 데이터 처리 환경에서 두 DB 간의 객관적인 성능 격차를 도출합니다.
3.  **데이터베이스 선택 기준 제시:** 실험 결과를 바탕으로 Redis와 같은 NoSQL 데이터베이스가 대규모 데이터 처리 환경에서 **뛰어난 성능**을 제공함을 입증합니다.

---

## 기술 스택 및 실험 환경

| 분류 | 기술 스택 | 주요 역할 및 특징 |
| :--- | :--- | :--- |
| **프레임워크** | **Java 11**, **Spring Boot 2.7.x** | 벤치마킹 로직 실행을 위한 안정적인 애플리케이션 환경 구축 |
| **RDBMS 대상** | **MySQL (JDBC)** | 디스크 기반 DB. `JdbcTemplate`을 사용하여 순수 SQL 성능 측정 |
| **NoSQL 대상** | **Redis (Key-Value)** | 인메모리 DB. `RedisTemplate`을 사용하여 고속 커맨드 성능 측정 |
| **ORM/Entity** | **Spring Data JPA** (MySQL 엔티티 정의) | MySQL 테이블(`TestData`)의 구조 정의 및 관리 |
| **실험 환경** | Windows 11, Intel Core i5, 16.0 GB RAM | 측정 프로세스 외 다른 실행을 최소화하여 정확한 측정 환경 조성 |

---

## 실험 설계 및 구현 상세

### 1. 실험 부하 조건 및 측정 방법

* **총 작업 수 (`COUNT`):** 각 기능(삽입, 조회, 삭제)별 **10,000회** 실행.
* **배치 크기 (`BATCH_SIZE`):** 1,000회 실행마다 평균 수행 시간을 측정하여 총 10번 반복 기록.
* **측정 단위:** 개별 SQL 쿼리 또는 Redis 커맨드 실행에 소요된 시간을 `System.currentTimeMillis`로 정밀하게 측정합니다.

### 2. 핵심 코드 구조 및 작동 원리

#### A. Spring Boot 초기화 및 실행

* `TestMysql.java`와 `TestRedis.java`는 모두 `SpringBootApplication`과 **`CommandLineRunner`** 인터페이스를 구현합니다.
* 애플리케이션이 실행되는 즉시 `run` 메서드가 호출되어, **개발자의 개입 없이** 벤치마킹 실험이 자동으로 시작되고 완료됩니다.

#### B. MySQL 벤치마킹 (`TestMysql.java`)

* **JdbcTemplate 사용:** JPA/Hibernate의 ORM 오버헤드를 배제하고 순수한 JDBC 수준의 성능을 측정하기 위해 `JdbcTemplate`을 사용합니다.
* **개별 실행 및 측정:** `performBatchOperations` 내의 `executeUpdateSql` 및 `executeQuerySql` 메서드는 **개별 SQL 쿼리**(`INSERT`, `SELECT`, `DELETE`) 실행 시간을 측정합니다.
    * **INSERT 쿼리:** `INSERT INTO test_data (id, name, key_index) VALUES (?, ?, ?)`
    * **SELECT/DELETE 쿼리:** `id = ?`와 같이 **Primary Key(기본 키)**를 사용하여 불필요한 디스크 I/O를 최소화하고, 효율적인 탐색이 가능하도록 합니다.
* **데이터 모델:** `TestData.java`는 `@Entity`로 선언된 단순 구조로, MySQL 테이블 스키마를 정의합니다.

#### C. Redis 벤치마킹 (`TestRedis.java`)

* **RedisTemplate 사용:** Redis 데이터 처리 표준인 `RedisTemplate`을 사용하여 키-값 기반 작업을 수행합니다.
* **명령어 실행 및 측정:** `executeRedisCommand` 메서드가 `redisTemplate.opsForValue.set`, `get`, `delete` 등 **개별 Redis 명령어** 실행 시간을 측정합니다.
* **직렬화 설정 (`RedisConfig.java`):**
    * **Key:** `StringRedisSerializer`를 사용하여 `"user:1", "user:2"`와 같은 키를 저장합니다.
    * **Value:** `GenericToStringSerializer<Integer>`를 사용하여 값(`keyIndex`)을 문자열로 직렬화하여 저장합니다.

---

## 연구 결과 및 기술적 시사점

실험 결과, **Redis**는 MySQL에 비해 데이터 처리 속도 면에서 **상당한 우위**를 보였습니다.

| 기능 | MySQL 평균 시간 (ms) | Redis 평균 시간 (ms) | Redis vs MySQL (속도 차이) |
| :--- | :--- | :--- | :--- |
| **삽입** | 1.3763 ms | 0.2357 ms | **약 5.84배 빠름**  |
| **조회** | 1.0550 ms | 0.1595 ms | **약 6.61배 빠름**  |
| **삭제** | 1.7473 ms | 0.1417 ms | **약 12.33배 빠름**  |
| **전체 평균** | 1.3929 ms | 0.1790 ms | **약 7.78배 빠름**  |

### 결론 및 시사점

* **Redis의 성능 우위 입증:** Redis는 인메모리(In-memory) 데이터베이스 특징으로 인해, 모든 데이터 처리 작업에서 MySQL 대비 월등한 속도를 보였습니다. 특히 **데이터 삭제** 작업에서 가장 큰 격차(약 12.33배)를 나타냈습니다.
* **활용 분야 제언:**
    * **Redis:** 고속 데이터 접근, 캐싱, 실시간 데이터 처리 등 **지연 시간이 중요한** 시나리오에 필수적입니다.
    * **MySQL:** 데이터 무결성 및 트랜잭션 **ACID 보장**이 필요한 핵심 비즈니스 로직 및 영구적인 데이터 저장에 적합합니다.

---
