# ⚖️MySQL–Redis 데이터 처리 성능 비교 실험

본 레포지토는 학술 논문「MySQL과 Redis의 데이터 처리 성능 비교 평가」(JICS, 2024)에서  
사용한 실험 코드를 재현 및 공개하기 위한 레포지토리이다.

관계형 데이터베이스(MySQL)와 인메모리 기반 NoSQL(Redis)를 대상으로  
동일한 조건에서 CRUD 연산 성능을 측정하여  
DB 구조 차이가 실제 지연시간에 미치는 영향을 분석한다.

## 목차

- 1. [개요](#1-개요)
- 2. [연구 및 실험 배경](#2-연구-및-실험-배경)
- 3. [실험 환경](#3-실험-환경)
- 4. [실험 설계](#4-실험-설계)
- 5. [프로젝트 구조](#5-프로젝트-구조)
- 6. [실행 방법](#6-실행-방법)
- 7. [실험 결과 요약 및 그래프](#7-실험-결과-요약-및-그래프)
- 8. [시사점](#8-시사점)
- 9. [한계 및 배운 점](#9-한계-및-배운-점)
- 10. [논문정보](#10-관련-논문)

## 1. 개요

본 프로젝트는 MySQL과 Redis의 CRUD 연산 성능 차이를 정량적으로 비교한다.

### Architecture Overview

| MySQL (Relational Model) | Redis (Key-Value Model) |
| :---: | :---: |
| ![MySQL Architecture](./images/mysql_architecture.png) | ![Redis Architecture](./images/redis_architecture.png) |
| **관계 중심 구조 (Join 기반 접근)** | **Direct Key–Value 접근 구조** |

MySQL과 Redis는 데이터 접근 방식이 다르다.

- **MySQL**
  - 여러 테이블 간 관계를 기반으로 데이터에 접근
  - 인덱스 탐색, 조인 처리 등으로 인해 구조적 오버헤드 발생
- **Redis**
  - 메모리 상에서 Key를 통해 Value에 즉시 접근
  - 디스크 I/O 및 쿼리 해석 과정이 제거된 단순 구조

이러한 구조적 차이로 CRUD 연산의 지연시간이 다르게 발생한다.

## 2. 연구 및 실험 배경

대규모 트래픽을 처리하는 웹 서비스에서  
데이터 저장소의 선택은 서비스 응답 지연과 직결된다.

특히 다음과 같은 데이터는:

- 로그인 세션
- 캐시 데이터
- 로그성 데이터

복잡한 조인이나 트랜잭션이 필요하지 않기 때문에  
전통적인 RDBMS보다 NoSQL이 더 적합할 수 있다.

본 실험은 다음 질문에서 출발한다.

> 단순 CRUD 작업에서 MySQL과 Redis의 처리 성능 차이는 어느 정도이며,  
> 실제 서비스 설계 시 Redis를 캐시 또는 대체 저장소로 고려할 수 있는 근거가 되는가?

## 3. 실험 환경

### 3.1 하드웨어 및 OS

- OS: Windows 11 (64bit)
- CPU: Intel i5-1340P
- Memory: 16GB RAM

### 3.2 소프트웨어 스택

- Java: 11  
- Spring Boot: 2.7.16  
- Gradle: 8.5 (Wrapper 기준)  
- Redis: 6.4.0  
- MySQL: 8.0.33  

※ Gradle은 실험 결과에 영향을 주는 변수가 아니며,  
빌드 및 실행을 위한 도구로만 사용되었다.

## 4. 실험 설계

### 4.1 공통 조건

- 삽입 / 조회 / 삭제 연산을 각각 10,000회 수행
- 1,000회 단위로 평균 수행 시간(ms) 측정
- 동일한 애플리케이션 로직 사용
- 실험 중 다른 프로세스 최소화

### 4.2 MySQL 실험 방식

- 테이블: `TEST_TABLE(ID, DATA)`
- ID는 Primary Key
- 사용 쿼리:
  - INSERT: `INSERT INTO TEST_TABLE ...`
  - SELECT: `SELECT * FROM TEST_TABLE WHERE ID = ?`
  - DELETE: `DELETE FROM TEST_TABLE WHERE ID = ?`

### 4.3 Redis 실험 방식

- Key-Value 구조 사용
- Key: 문자열 + 순번
- Value: 무작위 정수
- Spring Data Redis (`RedisTemplate`) 기반 CRUD 수행

## 5. 프로젝트 구조

```
mysql-redis-benchmark-main
├── src/main/java/com/benchmark
│   ├── BenchmarkApplication.java
│   ├── core
│   │   └── AbstractBatchExperiment.java
│   ├── mysql
│   │   └── MySQLExperiment.java
│   └── redis
│       └── RedisExperiment.java
├── build.gradle
└── README.md
```

- `core`  
  - 반복 실행, 시간 측정, 로그 출력 등 실험에 공통으로 사용되는 로직을 담당한다.
  - Template Method Pattern을 적용하여 실험 흐름을 고정하였다.

- `mysql`  
  - JDBC 기반으로 MySQL CRUD 실험을 수행하는 구현체이다.
  - SQL 쿼리를 통해 동일한 조건의 INSERT, SELECT, DELETE를 실행한다.

- `redis`  
  - Spring Data Redis의 RedisTemplate을 사용하여 CRUD 실험을 수행한다.
  - Key-Value 접근 방식만을 사용하여 구조적 차이에 따른 성능을 비교한다.

공통 추상 클래스를 통해  
실험 횟수, 측정 방식, 실행 순서를 통일하고  
DB 접근 방식만을 교체함으로써  
데이터베이스 구조 차이만이 결과에 반영되도록 설계하였다.

## 6. 실행 방법

### 6.1 데이터베이스 준비

- MySQL 서버 실행 및 테스트용 데이터베이스 생성
- Redis 서버 실행 (기본 포트 6379)

### 6.2 애플리케이션 실행

```bash
./gradlew bootRun
```

실행 시 MySQL → Redis에 순서로 실험이 수행되며, 각 연산의 평균 처리 시간이 로그로 출력된다.

## 7. 실험 결과 요약 및 그래프

논문에서 도출된 주요 결과는 다음과 같다.

- Redis는 MySQL 대비

  - 삽입(Insert): 약 5.84배 빠름
  - 조회(Select): 약 6.61배 빠름
  - 삭제(Delete): 약 12.33배 빠름
- 전체 평균 처리 속도는 약 7.78배 차이를 보였다.

이는 Redis의 인메모리 구조와  
단순 Key-Value 접근 방식이  
디스크 기반 RDBMS보다 지연시간 측면에서 유리함을 보여준다.  

## 8. 시사점

- 캐시, 세션, 로그성 데이터와 같이  
  응답 속도가 중요한 영역에서는 Redis가 효과적이다.
  
- 모든 데이터를 RDBMS에 저장하기보다  
  데이터 성격에 따라 저장소를 분리하는 아키텍처가 필요하다.
  
- 데이터베이스 선택은 단순한 기술 비교가 아니라  
  서비스 요구사항에 기반한 설계 문제이다.

## 9. 한계 및 배운 점

#### 한계

- 단일 노드 환경에서 실험이 수행되었다.
- 트랜잭션, 조인, 복잡한 쿼리는 고려하지 않았다.
- Redis의 영속성(RDB/AOF) 옵션은 비활성화된 상태이다.
- 분산 환경(Replication, Sharding)은 다루지 않았다.

#### 배운 점

- 성능 비교에서는 조건 통제가 가장 중요하다.
- 데이터베이스 선택은 기술 문제가 아니라 아키텍처 문제이다.
- 빠르다는 이유만으로 기술을 선택해서는 안 된다.

---

## 10. 논문 정보

- 논문명: MySQL과 Redis의 데이터 처리 성능 비교 평가
- 게재지: Journal of Internet Computing and Services (JICS)
- 게재년도: 2024

본 레포지토리는 해당 논문의 실험 재현 및 코드 공개 목적으로 제공된다.
