# MySQL–Redis 데이터 처리 성능 비교 실험

## 📑 목차

1. 프로젝트 개요  
2. 연구 및 실험 배경  
3. 실험 환경  
4. 실험 설계  
5. 실험 결과 요약  
6. 시사점  
7. 실험의 한계 및 배운 점  
8. 프로젝트 구조  
9. 실행 방법  
10. 논문정보  

## 📍1. 프로젝트 개요

본 저장소는 학술 논문 **「MySQL과 Redis의 데이터 처리 성능 비교 평가」(JICS, 2024)**에서 사용한 **실험 코드를 재현 및 공개**하기 위한 레포지토리이다.

관계형 데이터베이스(MySQL)와 인메모리 기반 NoSQL(Redis)를 대상으로 **삽입·조회·삭제 연산의 처리 지연시간**을 동일한 조건에서 측정하고, 데이터베이스 구조 차이에 따른 성능 특성을 실험적으로 분석한다.

본 프로젝트는 단순 벤치마크 구현이 아니라,

* 동일한 로직
* 동일한 실행 횟수
* 동일한 환경
  에서 **DB 구조 차이만이 성능에 미치는 영향**을 관찰하는 데 목적이 있다.

## 📍2. 연구 및 실험 배경

대규모 트래픽을 처리하는 웹 서비스에서는 데이터 저장소의 성능이 서비스 응답 지연과 직결된다. 특히 로그인 세션, 캐시 데이터, 로그성 데이터와 같이 **복잡한 조인이 필요 없는 데이터**의 경우, 전통적인 RDBMS보다 NoSQL이 더 적합할 수 있다.

본 실험은 다음 질문에서 출발한다.

> "단순 CRUD 작업에서 MySQL과 Redis의 처리 성능 차이는 어느 정도이며,
> 실제 서비스 설계 시 Redis를 캐시 혹은 대체 저장소로 고려할 근거가 되는가?"

## 📍3. 실험 환경

### 3.1 하드웨어 및 OS

* **OS**: Windows 11 (64bit)
* **CPU**: Intel i5-1340P
* **Memory**: 16GB RAM

### 3.2 소프트웨어 스택

* **Language**: Java
* **Framework**: Spring Boot
* **Build Tool**: Gradle
* **Database**:

  * MySQL (Disk-based RDBMS)
  * Redis (In-memory Key-Value Store)

## 📍4. 실험 설계

### 4.1 공통 조건

* 각 데이터베이스에 대해 **삽입 / 조회 / 삭제 연산을 각각 10,000회 수행**
* 1,000회 단위로 평균 수행 시간(ms)을 측정
* 동일한 애플리케이션 로직 사용
* 실험 중 다른 프로세스 최소화

### 4.2 MySQL 실험 방식

* 테이블: `TEST_TABLE(ID, DATA)`
* ID는 Primary Key
* SQL 쿼리

  * INSERT: `INSERT INTO TEST_TABLE ...`
  * SELECT: `SELECT * FROM TEST_TABLE WHERE ID = ?`
  * DELETE: `DELETE FROM TEST_TABLE WHERE ID = ?`

### 4.3 Redis 실험 방식

* Key-Value 구조 사용
* Key: 문자열 + 순번
* Value: 무작위 정수
* RedisTemplate 기반 CRUD 수행

## 📍5. 프로젝트 구조

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

* **core**: 공통 실험 로직 (반복 실행, 시간 측정)
* **mysql**: MySQL 전용 실험 구현
* **redis**: Redis 전용 실험 구현

공통 추상 클래스를 통해 실험 조건을 통일하고, DB 접근 방식만 분리하였다.

## 📍6. 실행 방법

### 6.1 데이터베이스 준비

* MySQL 서버 실행 및 DB 생성
* Redis 서버 실행

### 6.2 애플리케이션 실행

```bash
./gradlew bootRun
```

실행 시 MySQL 및 Redis에 대해 순차적으로 실험이 수행되며, 각 단계의 평균 처리 시간이 로그로 출력된다.

## 📍7. 실험 결과 요약

논문에서 도출된 주요 결과는 다음과 같다.

* Redis는 MySQL 대비

  * **삽입**: 약 5.84배 빠름
  * **조회**: 약 6.61배 빠름
  * **삭제**: 약 12.33배 빠름
* 전체 평균 처리 속도는 약 **7.78배 차이**

이는 Redis의 인메모리 구조와 단순 Key-Value 접근 방식이 디스크 I/O 및 스키마 제약을 가지는 RDBMS보다 유리함을 보여준다.

## 📍8. 백엔드 개발 관점에서의 시사점

- 캐시, 세션, 로그성 데이터와 같이 정합성보다 속도가 중요한 영역에서는 Redis가 효과적
- 모든 데이터를 RDBMS에 저장하기보다 역할 분리 아키텍처가 필요
- 데이터 특성에 따른 저장소 선택이 서비스 성능에 직접적인 영향을 미침

## 📍9. 실험의 한계 및 배운 점

한계점은 다음과 같다.

- 단일 노드 환경에서 수행되었다.
- 트랜잭션, 조인, 복잡한 쿼리는 고려하지 않았다.
- Redis의 영속성(RDB/AOF) 옵션은 비활성화된 상태이다.
- 분산 환경(Replication, Sharding)은 다루지 않았다.

본 실험을 통해 다음과 같은 점을 배웠다.

- 성능 비교에서 가장 중요한 것은 공정한 조건 통제이다.
- 데이터베이스 선택은 기술 문제가 아니라 아키텍처 문제이다.
- 빠르다는 이유만으로 기술을 선택해서는 안 된다.

## 📍10. 관련 논문

* **논문명**: MySQL과 Redis의 데이터 처리 성능 비교 평가
* **게재지**: Journal of Internet Computing and Services (JICS)
* **게재년도**: 2024

본 레포지토리는 해당 논문의 **실험 재현 및 코드 공개 목적**으로 제공된다.
