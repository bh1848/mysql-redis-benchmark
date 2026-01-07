# ⚖️ MySQL–Redis 데이터 처리 성능 비교 실험 (Benchmarking)

본 레포지토리는 학술 논문 「MySQL과 Redis의 데이터 처리 성능 비교 평가」(JICS, 2024)에서 사용된 실험 코드를 재현하고 공개하기 위한 프로젝트입니다. 관계형 데이터베이스(MySQL)와 인메모리 NoSQL(Redis)를 대상으로 동일한 조건에서 CRUD 연산 성능을 측정하여, DB 구조 차이가 실제 지연시간(Latency)에 미치는 영향을 분석합니다.

## 📌 목차
- 1. [개요](#1-개요)
- 2. [연구 및 실험 배경](#2-연구-및-실험-배경)
- 3. [실험 환경](#3-실험-환경)
- 4. [실험 설계](#4-실험-설계)
- 5. [Engineering Focus (설계 핵심)](#5-engineering-focus-설계-핵심)
- 6. [프로젝트 구조](#6-프로젝트-구조)
- 7. [실행 방법](#7-실행-방법)
- 8. [실험 결과 요약 및 그래프](#8-실험-결과-요약-및-그래프)
- 9. [시사점 및 배운 점](#9-시사점-및-배운-점)
- 10. [논문 정보](#10-논문-정보)

## 1. 개요
본 프로젝트는 MySQL과 Redis의 CRUD 연산 성능 차이를 정량적으로 비교합니다.

### Architecture Overview


| MySQL (Relational Model) | Redis (Key-Value Model) |
| :---: | :---: |
| ![MySQL Architecture](./images/mysql_architecture.png) | ![Redis Architecture](./images/redis_architecture.png) |
| **관계 중심 구조 (Join 기반 접근)** | **Direct Key–Value 접근 구조** |

- **MySQL**: 여러 테이블 간 관계 기반 접근, 인덱스 탐색 및 쿼리 해석으로 인한 구조적 오버헤드 발생.
- **Redis**: 메모리 상에서 Key를 통해 Value에 즉시 접근, 디스크 I/O 제거로 인한 단순 구조.

## 2. 연구 및 실험 배경
대규모 트래픽을 처리하는 웹 서비스에서 데이터 저장소의 선택은 서비스 응답 지연과 직결됩니다. 
특히 로그인 세션, 캐시 데이터, 로그성 데이터 등 복잡한 조인이나 트랜잭션이 필요하지 않은 데이터는 전통적인 RDBMS보다 NoSQL이 더 적합할 수 있습니다. 

본 실험은 **"단순 CRUD 작업에서 MySQL과 Redis의 처리 성능 차이는 어느 정도이며, 실제 서비스 설계 시 Redis를 캐시 또는 대체 저장소로 고려할 수 있는 근거가 되는가?"**라는 질문에서 출발했습니다.

## 3. 실험 환경
### 3.1 하드웨어 및 OS
- OS: Windows 11 (64bit)
- CPU: Intel i5-1340P / Memory: 16GB RAM

### 3.2 소프트웨어 스택
- Java: 11 / Spring Boot: 2.7.16
- Redis: 6.4.0 / MySQL: 8.0.33
- Gradle: 8.5 (Wrapper 기준)

## 4. 실험 설계
### 4.1 공통 조건
- 삽입 / 조회 / 삭제 연산을 각각 10,000회 수행
- 1,000회 단위로 평균 수행 시간(ms) 측정 (Batch Processing)
- 동일한 애플리케이션 로직 사용 및 실험 중 다른 프로세스 최소화

### 4.2 MySQL 및 Redis 실험 방식
- **MySQL**: `TEST_TABLE(ID, DATA)` 구조, ID(PK) 기반 `JdbcTemplate` 연산 수행.
- **Redis**: Key-Value 구조 (`user:{id}`), `RedisTemplate` 기반 연산 수행.

## 5. 설계 핵심
실험의 **신뢰성**과 **변수 통제**를 위해 적용한 백엔드 설계 전략입니다.

### 5.1 Template Method Pattern (`AbstractBatchExperiment.java`)
- 실험의 공통 워크플로우(Warm-up, 1,000회 단위 배치 실행, 평균 산출)를 상위 클래스에 고정했습니다.
- 이를 통해 MySQL과 Redis 간의 로직 편차를 제거하고, 오직 DB 엔진의 구조적 차이만이 결과에 반영되도록 설계했습니다.

### 5.2 리소스 분리리 (`application-*.yml`)
- `application-redis.yml` 실행 시 `autoconfigure.exclude` 옵션을 사용하여 JDBC 및 JPA 관련 자동 설정을 명시적으로 차단했습니다.
- Redis 성능 측정 시 불필요한 HikariCP 커넥션 풀 생성 및 메모리 점유를 방지하여 순수 저장소 성능을 격리 측정했습니다.


## 6. 프로젝트 구조

```bash
mysql-redis-benchmark-main
├── src/main/java/com/benchmark
│   ├── core
│   │   └── AbstractBatchExperiment.java (실험 흐름 공통화)
│   ├── mysql
│   │   └── MySQLExperiment.java (JDBC 기반 구현체)
│   ├── redis
│   │   ├── RedisBatchExperiment.java (RedisTemplate 기반 구현체)
│   │   └── RedisConfig.java (직렬화 최적화 설정)
│   └── BenchmarkApplication.java
└── src/main/resources
    ├── application-mysql.yml
    └── application-redis.yml (리소스 격리 설정)
```


## 6. 실행 방법

```bash
# MySQL 실험 실행
./gradlew bootRun --args='--spring.profiles.active=mysql'

# Redis 실험 실행
./gradlew bootRun --args='--spring.profiles.active=redis'
```

실행 시 MySQL → Redis에 순서로 실험이 수행되며, 각 연산의 평균 처리 시간이 로그로 출력된다.


## 7. 실험 결과 요약 및 그래프
논문 데이터와 실제 소스 코드 실행을 통해 도출된 핵심 결과입니다. 모든 수치는 1,000회 단위 배치 측정값의 평균을 기준으로 합니다.

### 📊 주요 성능 데이터
- **평균 처리 속도**: Redis가 MySQL 대비 약 **7.78배** 빠른 응답성을 보임.
- **연산별 상세 비교**:
  - **삽입(Insert)**: Redis가 약 **5.84배** 빠름
  - **조회(Select)**: Redis가 약 **6.61배** 빠름
  - **삭제(Delete)**: Redis가 약 **12.33배** 빠름

이는 Redis의 인메모리 구조와 단순 Key-Value 접근 방식이 디스크 기반 RDBMS의 인덱스 탐색 및 쿼리 해석 과정보다 지연시간 측면에서 압도적으로 유리함을 증명합니다.


## 9. 실험의 한계 및 향후 과제

현 프로젝트는 소스 코드의 목적에 따라 다음과 같은 제약 사항을 가지고 있습니다.

- **인프라 환경**: 단일 노드(Single Node) 환경 측정으로, 실제 운영 환경에서의 **클러스터링 및 네트워크 오버헤드**는 반영되지 않았습니다.
- **기능적 범위**: RDBMS의 강점인 **트랜잭션 보장(ACID) 및 복잡한 Join 연산** 시나리오는 제외된 단순 CRUD 비교입니다.
- **지속성 설정**: Redis의 영속성 옵션(RDB/AOF)이 비활성화된 상태이므로, 실제 데이터 유실 방지 설정을 적용할 경우 성능 격차는 줄어들 수 있습니다.


## 8. 시사점 및 배운 점

본 실험과 설계 과정을 통해 백엔드 엔지니어로서 필요한 기술적 판단 근거와 역량을 체득했습니다.

### 💡 기술적 통찰 (Technical Insights)
- **데이터 성격에 따른 저장소 전략**: 단순 CRUD 지연시간 차이(최대 12배)를 통해, 응답 속도가 생명인 **세션 관리나 캐시**에는 Redis가 필수적임을 수치로 확인했습니다.
- **Polyglot Persistence의 필요성**: 모든 데이터를 RDBMS에 몰아넣기보다, 서비스 요구사항에 맞춰 **Cache Aside** 패턴 등을 적용해 부하를 분산하는 아키텍처 설계의 중요성을 배웠습니다.
- **데이터 기반 의사결정**: "NoSQL이 빠르다"는 막연한 추측 대신, **B-Tree와 Hash Table의 구조적 차이**를 실제 성능 데이터와 연결하여 기술을 선택하는 엔지니어링 마인드를 갖추게 되었습니다.

### 🌱 성장 포인트 (Engineering Growth)
- **실험의 신뢰성 확보**: `AbstractBatchExperiment`를 설계하며 **템플릿 메서드 패턴**을 적용, 로직의 일관성을 유지함으로써 실험의 변수를 통제하는 법을 배웠습니다.
- **리소스 최적화 및 격리**: Spring Profile과 `autoconfigure.exclude`를 활용해 측정 대상 외의 리소스(HikariCP 등)가 결과에 미치는 영향을 차단하는 **실무적인 환경 설정 능력**을 키웠습니다.


## 10. 논문 정보
- **논문명**: MySQL과 Redis의 데이터 처리 성능 비교 평가
- **게재지**: Journal of Internet Computing and Services (JICS)
- **게재년도**: 2024
- **저자**: 방혁, 김서현, 전상훈

본 레포지토리는 위 논문의 실험 재현 및 소스 코드 공개 목적으로 제공됩니다.
