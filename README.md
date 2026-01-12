# ⚖️ MySQL vs Redis Performance Benchmark

> **관련 논문:** [MySQL과 Redis의 데이터 처리 성능 비교 평가 (JICS, 2024)]
> **저자:** 방혁, 김서현, 전상훈

RDBMS(MySQL)와 NoSQL(Redis)의 실제 CRUD 처리 성능을 비교/분석한 벤치마크 프로젝트입니다. 동일한 애플리케이션 로직 하에서 저장소 구조(Disk vs Memory)의 차이가 실제 Latency에 미치는 영향을 수치로 검증했습니다.


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

### 비교 대상 구조

| MySQL (RDBMS) | Redis (NoSQL) |
| :---: | :---: |
| ![MySQL Architecture](./images/mysql_architecture.png) | ![Redis Architecture](./images/redis_architecture.png) |
| **Disk I/O & B-Tree**<br>인덱스 탐색 및 디스크 접근 | **In-Memory & Hash**<br>메모리 직접 접근, O(1) |

### 실험 아키텍처
변수 통제를 위해 동일한 Spring Boot 애플리케이션 내에서 Profile로 구현체를 분리했습니다.

![Experimental Architecture](./images/architecture_overview.png)

> - 실행 옵션에 따라 JdbcTemplate / RedisTemplate 선택
> - 공통 비즈니스 로직은 유지하고 하부 저장소 접근 기술만 변경하여 순수 성능 차이 측정



## 2. 설계 의도

단순한 API 호출이 아닌 실험의 재현성과 정확도를 위한 엔지니어링 설계를 적용했습니다.

### Template Method Pattern
- 목적: 측정 로직 중복 제거 및 구현 방식에 따른 오차 방지
- 구현: AbstractBatchExperiment 추상 클래스에 워밍업, 타이머, 로깅 흐름을 고정하고, 하위 클래스는 DB 연산만 구현하도록 강제함

### 리소스 격리
- 목적: Redis 테스트 시 RDBMS 리소스(Connection, Memory) 개입 차단
- 구현: application-redis.yml에서 autoconfigure.exclude를 사용하여 DataSource, Hibernate 등 불필요한 Bean 생성을 원천 차단함



## 3. 실험 환경

- OS: Windows 11 (64bit)
- CPU/RAM: Intel Core i5-1340P, 16GB RAM
- Tech Stack: Java 11, Spring Boot 2.7.16, Gradle 8.5
- DB Version: MySQL 8.0.33, Redis 6.4.0
- Data: Integer Key-Value 데이터 10,000건
- Scenario: 1,000건 단위 Batch 처리 후 평균 수행 시간(ms) 측정



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



## 5. 실행 방법

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



## 6. 실험 결과

10,000건의 데이터 처리 기준 평균 수행 시간 (낮을수록 우수)

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
- Memory의 우위: Disk I/O가 없는 Redis가 평균 약 7.8배 빠른 성능을 기록함
- Delete 성능 격차: 삭제 연산에서 최대 격차(12배) 발생. MySQL의 인덱스 재정렬 및 트랜잭션 로그 비용이 주요 원인으로 분석됨


## 7. 결론 및 고찰

### 연구의 한계
본 실험은 로컬 환경과 단일 스레드 기반으로 수행되어 다음과 같은 제약 사항을 가집니다.
- Network Latency: 실제 클라우드 환경의 네트워크 왕복 시간(RTT) 미반영
- Concurrency: Redis의 단일 스레드 아키텍처에서 대규모 동시 접속 시 발생할 수 있는 병목 현상 미검증
- Single Metric: 평균 응답 시간 위주의 측정으로, P99 등 Tail Latency에 대한 분석 부족

### Key Insights
실험 데이터를 통해 도출한 기술적 결론은 다음과 같습니다.

1. 저장소 분리 전략의 타당성 확보: 단순 조회 시 7.8배, 삭제 시 12배의 성능 격차를 확인함으로써, Read-Heavy 데이터에 대한 Redis 캐싱 전략이 비용 대비 높은 효율을 냄을 입증했습니다.

2. 실험 재현성의 중요성: 동일한 로직이라도 하드웨어 상태와 초기화 변수에 따라 결과가 달라질 수 있음을 확인했습니다. 이에 따라 Template Method 패턴을 통한 측정 로직 표준화와 환경 격리가 벤치마크 신뢰도의 핵심임을 확인했습니다.

3. 통계적 모니터링의 필요성: 평균값이 전체 성능을 대변하지 못함을 인지했습니다. 실제 운영 환경에서는 평균 응답 속도보다 사용자 경험에 치명적인 P99 Latency 지표 관리가 필수적이라는 결론을 얻었습니다.
