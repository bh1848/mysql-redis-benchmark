# ⚖️ MySQL vs Redis Performance Benchmark

![Java](https://img.shields.io/badge/Java-11-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-2.7-6DB33F?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-6.4-DC382D?logo=redis&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

> **논문 제목:** MySQL과 Redis의 데이터 처리 성능 비교 평가 (Comparative Evaluation of Data Processing Performance between MySQL and Redis)  
> **저자:** 방혁, 김서현, 전상훈 (수원대학교)  
> **게재:** Journal of Internet Computing and Services (JICS), 2024  

RDBMS(MySQL)와 NoSQL(Redis)의 실제 CRUD 처리 성능을 비교/분석한 벤치마크 프로젝트입니다. 동일한 애플리케이션 로직 하에 Disk vs Memory의 차이가 실제 애플리케이션 Latency에 미치는 영향을 검증했습니다.
Redis가 MySQL에 비해 얼마나 빠른지 직접 환경을 구축하고 수치로 측정하여, 상황에 맞게 DB 선택을 하고자 했습니다.


## 📋 목차
1. [프로젝트 소개](#1-프로젝트-소개)
2. [실험 구조](#2-실험-구조)
3. [실험 환경](#3-실험-환경)
5. [실행 방법](#4-실행-방법)
6. [실험 결과](#5-실험-결과)
7. [트러블 슈팅](#6-트러블-슈팅)
8. [한계 및 향후 과제](#7-한계-및-향후-과제)


## 1. 프로젝트 소개

### 배경
현대 웹 애플리케이션은 대규모 트래픽 처리를 위해 캐싱(Caching) 전략이 필수적입니다. 이론적으로 In-Memory DB인 Redis가 Disk 기반의 MySQL보다 빠르다는 것은 널리 알려져 있으나, 실제 Spring Boot 애플리케이션 환경에서 구체적으로 어느 정도의 Latency 차이가 발생하는지에 대한 정량적인 데이터가 필요했습니다. 단순한 조회 속도 차이를 넘어, 실제 비즈니스 로직에 캐싱을 도입했을 때 얻을 수 있는 성능 이점을 수치로 확인하고자 했습니다.

### 목적
1. MySQL과 Redis의 CRUD 연산 속도를 동일 환경에서 측정하여 비교합니다.
2. 직접 측정한 데이터를 기반으로 상황별(Read-heavy vs Write-heavy) 적합한 DB 기술 스택 선정 기준을 마련합니다.
3. 속도(Memory)와 안정성(Disk) 간의 기술적 트레이드오프를 이해하고 분석합니다.

### 실험 아키텍처
![Experimental Architecture](./images/architecture_overview.png)

> - 실행 옵션에 따라 JdbcTemplate / RedisTemplate 선택할 수 있습니다.
> - 공통 비즈니스 로직은 유지하고 하부 저장소 접근 기술만 변경하여 순수 성능 차이를 측정했습니다.

### Disk vs Memory

| MySQL (RDBMS) | Redis (NoSQL) |
| :---: | :---: |
| ![MySQL Architecture](./images/mysql_architecture.png) | ![Redis Architecture](./images/redis_architecture.png) |
| **Disk I/O & B-Tree**<br>디스크 접근 및 인덱스 탐색 비용 발생 | **In-Memory & Hash**<br>메모리 직접 접근, Key-Value 조회 (O(1)) |



## 2. 실험 구조

### 측정 지표
DB 내부의 쿼리 실행 시간만이 아닌, 실제 백엔드 서버의 총 응답 시간을 측정했습니다.
- `Time = Serialization + Network I/O + DB Execution + Deserialization`

### 테스트 워크로드
- JDBC Batch 기능 등의 최적화 기법을 배제하고, 순수하게 1회 호출 당 처리 속도를 비교하기 위해 10,000회의 Single Operation을 반복 수행했습니다.
- 실험 시작 전 Warm-up 과정을 거쳐 Connection Pool(HikariCP) 초기화 비용을 배제했으며, 풀 사이즈를 충분히 확보하여 Connection 대기 시간을 통제했습니다.
- `AbstractBatchExperiment` 클래스를 통해 워밍업, 시간 측정, 로깅 로직을 중앙화하여 구현체 별 측정 오차를 제거했습니다.

### 리소스 격리
- Spring Profile 기능을 활용하여 테스트 대상이 아닌 DB의 Bean 생성을 차단(`autoconfigure.exclude`), 메모리 및 Connection Pool 간섭을 방지했습니다.


## 3. 실험 환경

- 하드웨어: Intel Core i5-1340P, 16GB RAM, Windows 11 (64bit)
- 소프트웨어: Java 11, Spring Boot 2.7.16, MySQL 8.0.33, Redis 6.4.0
- 데이터셋:
  - Synthetic Key-Value: 10,000건의 Integer 기반 더미 데이터
- 파라미터 설정:  
  - Total Operations (N) = 10,000
  - Batch Size (B) = 1,000 (Connection Pool 부하 및 측정 오차 제어를 위한 단위)
  - Metric = Average Latency (ms)

> **Why Localhost?**  
> 네트워크 지연 변수를 최소화하고, 순수하게 Disk B-Tree vs In-Memory Hash의 차이에 집중하기 위해 로컬 환경에서 Loopback 통신으로 수행했습니다.


## 4. 실행 방법

### 벤치마크 실행

- MySQL 벤치마크
~~~bash
# Mac/Linux
./gradlew bootRun --args="--spring.profiles.active=mysql"

# Windows (CMD/PowerShell)
gradlew bootRun --args="--spring.profiles.active=mysql"
~~~

- Redis 벤치마크
~~~bash
# Mac/Linux
./gradlew bootRun --args="--spring.profiles.active=redis"

# Windows (CMD/PowerShell)
gradlew bootRun --args="--spring.profiles.active=redis"
~~~


## 5. 실험 결과

![Performance Result](./images/result_graph.jpg)

> **Note:** 파란색(좌) MySQL / 빨간색(우) Redis

| Operation | MySQL (Disk/B-Tree) | Redis (Memory/Hash) | Speedup | Complexity |
| :---: | :---: | :---: | :---: | :---: |
| Insert | 1.37 ms | 0.23 ms | 5.8배 | O(log N) vs O(1) |
| Select | 1.05 ms | 0.15 ms | 6.6배 | O(log N) vs O(1) |
| Delete | 1.74 ms | 0.14 ms | 12.3배 | O(log N) vs O(1) |
| **Average** | **1.39 ms** | **0.17 ms** | **7.8배** | - |

> **Note:** In-Memory 기반의 Redis는 Disk 기반의 MySQL 대비 평균 7.8배 빠른 Latency를 기록했습니다. 특히 Delete 연산에서 12배 이상의 가장 큰 성능 격차가 발생했습니다.
> - **MySQL:** 트랜잭션 보장을 위한 Undo Log 기록 및 삭제 후 B-Tree 인덱스 재정렬 비용이 발생합니다.
> - **Redis:** Hash 구조상 키를 찾아 메모리 포인터만 해제하는 O(1) 연산이므로 오버헤드가 거의 없습니다.


## 6. 트러블 슈팅

| 문제 상황 | 원인 | 해결 방법 | 기술 블로그 |
| :--- | :--- | :--- | :---: |
| **테스트 데이터 누적** | 독립적 실행 환경(Clean State) 미보장 | `ddl-auto: create` 옵션으로 매회 초기화 | [🔗 Link](링크주소) |
| **시간 측정 정밀도 한계** | `currentTimeMillis`의 해상도 부족 (0ms) | Batch 단위 총 시간 측정 후 역산 방식 적용 | [🔗 Link](링크주소) |
| **동기 실행(Sync) 병목** | Network RTT가 전체 성능을 지배 | RTT를 포함한 'Client Side Latency'로 지표 변경 | [🔗 Link](링크주소) |


## 7. 한계 및 향후 과제

### 한계점
- Redis는 Loose Consistency를 가집니다. 본 실험은 Latency 측정에 집중했으나, 데이터 무결성이 최우선인 환경에서는 RDBMS와의 트레이드오프를 고려해야 함을 상기했습니다.
- `System.currentTimeMillis()`를 사용하여 'ms' 단위의 체감 지연 시간을 측정했습니다. `System.nanoTime()`보다는, I/O가 포함된 애플리케이션 레벨의 경향성을 파악하는 데 집중했습니다.
- 실제 클라우드 환경의 RTT 미반영 및 Redis의 단일 스레드 아키텍처에서 대규모 동시 접속 시 발생할 수 있는 병목 현상은 검증 범위에서 제외되었습니다.


### 향후 연구 과제
- 현재의 MySQL vs Redis 비교를 넘어, MongoDB 등 다양한 NoSQL 유형과의 성능 비교를 통해 데이터 성격에 따른 최적의 DB 선택 전략을 수립할 예정입니다.
- 현재의 Java 11 / Spring Boot 2.7 환경을 Java 17+ 및 Spring Boot 3.x로 마이그레이션하여, `Record` 클래스 등을 활용한 DTO 경량화 및 최신 GC 성능 개선 효과를 추가로 검증할 계획입니다.
- 평균 응답 시간 외에 P95, P99 지표를 추가하여 간헐적인 Jitter 현상을 분석할 예정입니다.
