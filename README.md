# ⚖️ MySQL vs Redis Performance Benchmark

![Java](https://img.shields.io/badge/Java-11-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-2.7-6DB33F?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-6.4-DC382D?logo=redis&logoColor=white)

> **관련 논문:** [MySQL과 Redis의 데이터 처리 성능 비교 평가 (JICS, 2024)]
> **저자:** 방혁, 김서현, 전상훈

RDBMS(MySQL)와 NoSQL(Redis)의 실제 CRUD 처리 성능을 비교/분석한 벤치마크 프로젝트입니다. 동일한 애플리케이션 로직 하에서 저장소 구조(Disk vs Memory)의 차이가 실제 Latency에 미치는 영향을 검증했습니다.

## 👨‍💻Why This Project?

캐시를 도입하면 얼마나 빨라지는지 궁금해서 시작했습니다. 단순히 Redis가 빠르다가 아니라, 얼마나 빠른지 직접 환경을 구축하고 수치로 측정하고자 했습니다. 이를 통해 상황에 맞게 DB 선택을 하고자 했습니다.


## ⚡실험 결과 요약

10,000건의 데이터 처리 실험 결과, Redis는 MySQL 대비 압도적인 성능 우위를 보였습니다.

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

> **Result:** 특히 Delete 연산에서 12배 이상의 성능 격차가 발생했습니다. 이는 MySQL의 트랜잭션 로그 및 인덱스 재정렬 비용이 주요 원인임을 확인했습니다.


## 📋 목차
1. [프로젝트 개요](#1-프로젝트-개요)
2. [핵심 설계](#2-핵심-설계)
3. [실험 환경](#3-실험-환경)
4. [프로젝트 구조](#4-프로젝트-구조)
5. [실행 방법](#5-실행-방법)
6. [결론 및 고찰](#6-결론-및-고찰)



## 1. 프로젝트 개요

### 비교 대상 구조 (Disk vs Memory)

| MySQL (RDBMS) | Redis (NoSQL) |
| :---: | :---: |
| ![MySQL Architecture](./images/mysql_architecture.png) | ![Redis Architecture](./images/redis_architecture.png) |
| **Disk I/O & B-Tree**<br>디스크 접근 및 인덱스 탐색 비용 발 | **In-Memory & Hash**<br>메모리 직접 접근, Key-Value 조회 (O(1)) |

### 실험 아키텍처
비교의 공정성을 위해 동일한 Spring Boot 애플리케이션 내에서 Profile만 변경하여 테스트했습니다.

![Experimental Architecture](./images/architecture_overview.png)

> - 실행 옵션에 따라 JdbcTemplate / RedisTemplate 선택
> - 공통 비즈니스 로직은 유지하고 하부 저장소 접근 기술만 변경하여 순수 성능 차이 측정



## 2. 핵심 설계

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



## 6. 결론 및 고찰

### 연구의 한계
본 실험은 로컬 환경과 단일 스레드 기반으로 수행되어 다음과 같은 제약 사항을 가집니다.
- 실제 클라우드 환경의 네트워크 왕복 시간(RTT) 미반영
- Redis의 단일 스레드 아키텍처에서 대규모 동시 접속 시 발생할 수 있는 병목 현상 미검증
- 평균 응답 시간 위주의 측정으로, Tail Latency에 대한 분석 부족

### Key Insights
실험을 통해 도출한 결론은 다음과 같습니다.

1. 단순 조회 시 7.8배, 삭제 시 12배의 성능 격차를 확인함으로써, Read-Heavy 데이터에 대한 Redis 캐싱 전략이 비용 대비 높은 효율을 낸다는 것을 확인했습니다.

2. 동일한 로직이라도 하드웨어 상태와 초기화 변수에 따라 결과가 달라질 수 있음을 인지했습니다. 따라서 Template Method 패턴으로 로직을 통제하고 환경을 격리하는 것이 신뢰도에서 중요하다는 것을 확인했습니다.

3. 이번 실험에서는 평균값만 봤지만, 실제 운영 환경에서는 평균 응답 속도보다 튀는 값이 사용자 경험에 더 치명적임을 깨달았습니다. 향후 프로젝트에서는 P95/P99 Latency 지표 관리를 필수로 적용할 계획입니다.
