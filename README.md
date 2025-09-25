# MySQL과 Redis의 데이터 처리 성능 비교 평가 연구

## 프로젝트 개요 및 연구 목표

[cite_start]본 프로젝트는 **"MySQL과 Redis의 데이터 처리 성능 비교 평가"** 라는 제목으로 한국 인터넷 정보학회지([cite: 1][cite_start])에 게재된 논문([cite: 3])의 핵심 연구 내용을 구현한 **데이터베이스 성능 벤치마킹 실험 프로젝트**입니다.

[cite_start]디지털 변화와 온라인 활동 급증에 따라 중요성이 커진 대규모 데이터 처리 및 유지보수 환경에서([cite: 14, 23][cite_start]), 데이터 저장 및 관리의 효율성을 높이기 위해 RDBMS와 NoSQL의 대표 주자를 비교 분석했습니다([cite: 15]).

### 연구 목표

1.  [cite_start]**대표 DB 성능 비교:** RDBMS의 대표 예인 **MySQL**과 NoSQL의 대표 예인 **Redis**를 사용하여([cite: 16][cite_start]), 데이터 처리 기능(삽입, 조회, 삭제)의 수행 시간을 정밀하게 측정하고 평가합니다([cite: 16, 25]).
2.  [cite_start]**구체적 성능 지표 도출:** 10,000회 실행을 1,000회 단위의 배치로 나누어 총 10회 평균 수행 시간을 측정함으로써([cite: 156, 160][cite_start]), 대규모 데이터 처리 환경에서 두 DB 간의 **객관적인 성능 격차**를 도출합니다([cite: 17]).
3.  [cite_start]**데이터베이스 선택 기준 제시:** 실험 결과를 바탕으로 Redis와 같은 NoSQL 데이터베이스가 대규모 데이터 처리 환경에서 **뛰어난 성능**을 제공함을 입증하고([cite: 18, 27][cite_start]), 기업 및 온라인 서비스 제공자에게 효율적인 데이터 관리 솔루션 선택의 참고 자료를 제공합니다([cite: 19, 29]).

---

## 🛠️ 기술 스택 및 실험 환경

| 분류 | 기술 스택 | 주요 역할 및 특징 |
| :--- | :--- | :--- |
| **프레임워크** | **Java**, **Spring Boot** | [cite_start]안정적인 벤치마킹 로직 실행 환경 구축([cite: 149]) |
| **RDBMS 대상** | **MySQL (JDBC)** | 디스크 기반 DB. [cite_start]`JdbcTemplate`을 사용하여 SQL 성능 측정([cite: 143, 150]) |
| **NoSQL 대상** | **Redis (Key-Value)** | 인메모리 DB. [cite_start]`RedisTemplate`을 사용하여 고속 커맨드 성능 측정([cite: 125, 150]) |
| **실험 환경** | Windows 11, 13th Gen Intel Core i5, 16.0 GB RAM | [cite_start]측정 프로세스 외 다른 실행을 최소화하여 정확한 측정 환경 조성([cite: 151, 181, 182]) |

---

## 실험 설계 및 구현 상세

### 1. 실험 부하 조건 및 측정 방법

* [cite_start]**총 작업 수:** 각 기능(삽입, 조회, 삭제)별 **10,000회** 실행([cite: 156, 185]).
* [cite_start]**측정 배치:** 1,000회 실행마다 **평균 수행 시간**을 측정하여 총 10번 반복 기록([cite: 160, 185]).
* [cite_start]**측정 데이터:** 무작위로 생성된 **정수형 데이터**를 사용([cite: 156, 162]).
* [cite_start]**로깅:** 각 함수의 성능 평가에 소요된 시간을 측정 후 로그 형태로 기록([cite: 152, 161, 176]).

### [cite_start]2. 구현 쿼리 및 커맨드 ([cite: 166, 171, 188, 191])

| 기능 | MySQL 쿼리 (JdbcTemplate) | Redis 커맨드 (RedisTemplate) | 핵심 전략 |
| :--- | :--- | :--- | :--- |
| **삽입 (INSERT)** | `INSERT INTO TEST_TABLE (ID, DATA) VALUES (?, ?)` | `redisTemplate.opsForValue().set(key, keyIndex)` | [cite_start]MySQL과 비슷하게 두 가지 데이터 저장([cite: 189]) |
| **조회 (SELECT)** | `SELECT * FROM TEST_TABLE WHERE ID = ?` | `redisTemplate.opsForValue().get(key)` | [cite_start]**기본 키(ID)** 또는 **키(Key)**를 사용하여 불필요한 디스크 I/O를 줄임([cite: 169]) |
| **삭제 (DELETE)** | `DELETE FROM TEST_TABLE WHERE ID = ?` | `redisTemplate.delete(key)` | [cite_start]기본 키(ID) 또는 키를 사용하여 데이터 삭제([cite: 170]) |

---

## 연구 결과 및 기술적 시사점

[cite_start]실험 결과, **Redis**는 MySQL에 비해 데이터 처리 속도 면에서 **상당한 우위**를 보였습니다([cite: 222]).

| 기능 | MySQL 평균 시간 (ms) | Redis 평균 시간 (ms) | Redis vs MySQL (속도 차이) |
| :--- | :--- | :--- | :--- |
| **삽입** | [cite_start]1.3763 ms [cite: 197, 204] | [cite_start]0.2357 ms [cite: 208, 213] | [cite_start]**약 5.84배 빠름** [cite: 17, 217, 221] |
| **조회** | [cite_start]1.0550 ms [cite: 197, 204] | [cite_start]0.1595 ms [cite: 208, 213] | [cite_start]**약 6.61배 빠름** [cite: 17, 217, 221] |
| **삭제** | [cite_start]1.7473 ms [cite: 197, 204] | [cite_start]0.1417 ms [cite: 208, 213] | [cite_start]**약 12.33배 빠름** [cite: 17, 218, 221] |
| **전체 평균** | [cite_start]1.3929 ms [cite: 229] | [cite_start]0.1790 ms [cite: 234] | [cite_start]**약 7.78배 빠름** [cite: 221] |

### 결론 및 시사점

1.  [cite_start]**Redis의 성능 우위 입증:** Redis는 인메모리(In-memory) 데이터베이스의 특징을 바탕으로([cite: 125, 245][cite_start]), 데이터 삽입, 조회, 삭제 **모든 기능에서** MySQL 대비 월등히 빠른 처리 속도를 보였습니다([cite: 220, 244]).
2.  **데이터베이스 선택 가이드라인:**
    * [cite_start]**Redis:** 고속 데이터 접근, 대용량 데이터 처리, 실시간 데이터 처리([cite: 146][cite_start]), 특히 대규모 데이터 처리 및 유지보수([cite: 18, 27])가 필요한 환경에 적합합니다.
    * [cite_start]**MySQL:** 데이터의 일관성 및 무결성이 중요하고([cite: 42][cite_start]), 복잡한 쿼리 및 트랜잭션 처리가 필요한 환경([cite: 146, 52])에 적합합니다.
3.  [cite_start]**향후 연구 계획:** Redis의 성능을 더욱 향상시킬 수 있는 방안을 탐구하고, 다양한 데이터베이스 유형과의 성능 비교를 통해 최적의 데이터 관리 전략을 제안할 계획입니다([cite: 250, 251]).

---
