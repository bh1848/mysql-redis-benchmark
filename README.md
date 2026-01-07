# ⚖️ MySQL vs Redis Performance Benchmark

> **관련 논문:** [MySQL과 Redis의 데이터 처리 성능 비교 평가 (JICS, 2024)]
> **저자:** 방혁, 김서현, 전상훈

RDBMS(MySQL)와 NoSQL(Redis)의 실제 데이터 처리 성능을 비교하기 위해 구현한 벤치마크 프로젝트입니다.
"NoSQL이 빠르다"는 막연한 통념을 넘어, 동일한 로직에서 저장소 구조(Disk vs Memory) 차이가 실제 CRUD 속도에 얼마나 영향을 미치는지 수치로 확인해보고자 했습니다.

## 📋 목차
1. [프로젝트 개요](#1-프로젝트-개요)
2. [설계 의도](#2-설계-의도)
3. [실험 환경](#3-실험-환경)
4. [프로젝트 구조](#4-프로젝트-구조)
5. [실행 방법](#5-실행-방법)
6. [실험 결과](#6-실험-결과)
7. [한계점 및 회고](#7-한계점-및-회고)



## 1. 프로젝트 개요

백엔드 시스템에서 데이터 저장소 선택은 성능에 직결됩니다. 이번 실험에서는 단순 CRUD 작업에서 Redis 도입 시 RDBMS 대비 어느 정도의 성능 이점이 있는지 검증하고, 이를 통해 효율적인 캐싱 전략의 근거를 마련하고자 했습니다.

### MySQL vs Redis 구조 차이
실험의 핵심은 저장 매체와 자료구조의 차이입니다.

| MySQL (RDBMS) | Redis (NoSQL) |
| :---: | :---: |
| ![MySQL Architecture](./images/mysql_architecture.png) | ![Redis Architecture](./images/redis_architecture.png) |
| **Disk I/O & B-Tree**<br>디스크 저장, 인덱스 탐색 비용 발생 | **In-Memory & Hash**<br>메모리 직접 접근, Key-Value 조회 (O(1)) |

### Architecture Overview
비교의 공정성을 위해 동일한 Spring Boot 애플리케이션 내에서 Profile만 변경하여 테스트했습니다.

![Experimental Architecture](./images/architecture_overview.png)

> - 실행 옵션에 따라 JdbcTemplate 혹은 RedisTemplate로 로직이 분기됩니다.
> - 공통 비즈니스 로직은 유지한 채, 하부의 데이터 접근 기술과 저장소 인프라만 교체하여 순수한 성능 차이를 측정했습니다.



## 2. 설계 의도

단순히 API를 몇 번 호출해보는 테스트가 아니라, 실험의 신뢰성을 높이기 위해 코드 레벨에서 변수를 통제했습니다.

### Template Method Pattern
DB마다 측정 코드를 따로 짜면 오차가 생길 수 있습니다. 이를 막기 위해 AbstractBatchExperiment라는 추상 클래스를 만들었습니다.
- run() 메서드에 워밍업, 시간 측정, 로깅 흐름을 박제해두었습니다.
- 하위 클래스는 순수하게 DB에 다녀오는 로직만 구현하도록 강제했습니다.

### 리소스 격리
Redis 성능을 잴 때 MySQL이 몰래 연결을 맺거나 메모리를 쓰면 안 됩니다.
- application-redis.yml에서는 Spring Boot의 autoconfigure.exclude 옵션을 사용해 DataSource, Hibernate 등 RDBMS 관련 Bean 생성을 차단했습니다.



## 3. 실험 환경

- OS: Windows 11 (64bit)
- H/W: Intel Core i5-1340P, 16GB RAM
- Stack: Java 11, Spring Boot 2.7.16, Gradle 8.5
- DB: MySQL 8.0.33, Redis 6.4.0
- Data: Integer Key-Value 데이터 10,000건
- Method: 1,000건 단위 Batch 처리 후 평균 수행 시간(ms) 측정



## 4. 프로젝트 구조

~~~bash
src/main/java/com/benchmark
├── core
│   └── AbstractBatchExperiment.java  # Template Method
├── mysql
│   ├── MysqlBatchExperiment.java     # JDBC 구현체
│   └── application-mysql.yml         # MySQL 설정
├── redis
│   ├── RedisBatchExperiment.java     # RedisTemplate 구현체
│   ├── RedisConfig.java              # 직렬화 설정
│   └── application-redis.yml         # RDBMS 격리 설정
└── BenchmarkApplication.java
~~~

---

## 5. 실행 방법

Gradle Wrapper가 포함되어 있어 별도 설치 없이 바로 실행 가능합니다.

**1. MySQL 벤치마크**
~~~bash
# Mac/Linux
./gradlew bootRun --args="--spring.profiles.active=mysql"

# Windows (CMD/PowerShell)
gradlew bootRun --args="--spring.profiles.active=mysql"
~~~

**2. Redis 벤치마크**
~~~bash
# Mac/Linux
./gradlew bootRun --args="--spring.profiles.active=redis"

# Windows (CMD/PowerShell)
gradlew bootRun --args="--spring.profiles.active=redis"
~~~

> **Note:** 실행하면 콘솔에 `Batch 1 - Insert Avg: ...` 로그가 찍히며 측정 결과가 나옵니다.

---

## 6. 실험 결과

10,000건의 데이터를 처리했을 때의 평균 수행 시간입니다. (낮을수록 빠름)

### 성능 비교 그래프

![Performance Result](./images/result_graph.jpg)

> **Note:** 파란색(좌) MySQL / 빨간색(우) Redis

### 상세 지표

| Operation | MySQL | Redis | Speedup |
| :---: | :---: | :---: | :---: |
| Insert | 1.37 ms | 0.23 ms | 5.8배 |
| Select | 1.05 ms | 0.15 ms | 6.6배 |
| Delete | 1.74 ms | 0.14 ms | 12.3배 |
| **Average** | **1.39 ms** | **0.17 ms** | **7.8배** |

### 결과 분석
- 역시 메모리 기반인 Redis가 압도적으로 빠릅니다. 디스크 I/O 대기 시간이 없다는 점이 가장 컸습니다.
- 삭제 연산에서 격차가 12배로 가장 컸습니다. MySQL은 삭제 시 인덱스 재정렬, 트랜잭션 로그 기록 등 부가 작업이 많기 때문으로 보입니다.



## 7. 한계점 및 회고

### 실험의 한계
- 로컬 통신이라 실제 네트워크 지연(RTT)이 빠져있습니다.
- 트랜잭션이나 복잡한 Join 없이 단순 CRUD만 비교했습니다.
- 평균값만 측정하다 보니, 튀는 값이나 P99 Latency와 같은 최악의 경우를 보지 못했습니다.
- 단일 스레드로 돌린 실험입니다. 싱글 스레드인 Redis에 대량의 동시 접속이 몰릴 때 발생하는 병목은 반영되지 않았습니다.

### 배운 점
이번 프로젝트로 논문을 작성하고 실험하며 느낀 점들입니다.

- 실험 재현성의 중요도를 깨달았습니다. 그래서 하드웨어 스펙이나 초기화 조건을 꼼꼼히 기록하고 변수를 통제하는 데 신경을 많이 썼습니다.

- 조회 속도가 12배까지 차이 나는 걸 직접 보니, 왜 세션이나 캐시는 Redis에 저장하는지 체감했습니다. 데이터 중요도에 따라 MySQL과 Redis를 섞어 쓰는 전략이 필수적임을 알았습니다.

- 결과를 분석하며 "평균은 거짓말을 할 수 있다"는 걸 깨달았습니다. 다음 성능 테스트 때는 평균뿐만 아니라 P99 Latency를 함께 모니터링해야겠다고 느꼈습니다.
