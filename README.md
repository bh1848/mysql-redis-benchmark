# MySQL vs Redis 성능 비교 벤치마크

> **"Disk 기반 RDBMS와 In-Memory NoSQL의 실제 응답 속도 차이를 분석한 성능 비교 연구"**

<p align="left">
  <img src="https://img.shields.io/badge/Java-11-007396?style=flat-square&logo=java&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Boot-2.7-6DB33F?style=flat-square&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white"/>
  <img src="https://img.shields.io/badge/Redis-7.4-DC382D?style=flat-square&logo=redis&logoColor=white"/>
</p>

## 논문 정보
* **제목**: MySQL과 Redis의 데이터 처리 성능 비교 평가
* **학술지**: JICS 2024 게재 (KCI)
* **저자**: 방혁, 김서현, 전상훈

<br/>

## 1. 프로젝트 요약
**실제 Spring Boot 애플리케이션 환경에서 메모리와 디스크 사이에서 발생하는 지연 시간(Latency)을 측정**했습니다. 같은 비즈니스 로직에서 MySQL과 Redis가 어느 정도 성능 차이를 보이는지 분석하여, 상황에 맞는 DB를 고르는 기준을 세우려 했습니다.

<br/>

## 2. 실험 구조
![Experimental Architecture](./images/architecture_overview.png)

* **같은 환경에서 테스트**: 공통 로직은 그대로 두고 Spring Profile을 써서 저장소 접근 방식(JDBC vs RedisTemplate)만 바꿔가며 순수 성능 차이를 쟀습니다.
* **측정 기준**: DB 내부 실행 시간뿐만 아니라 데이터 변환, 네트워크 왔다 갔다 하는 시간(RTT)을 모두 포함한 **전체 응답 시간**을 기준으로 삼았습니다.

<br/>

## 3. 실험 결과 (10,000건 단건 연산 기준)
메모리 기반의 Redis가 디스크 기반의 MySQL보다 **평균 7.8배 빠른 속도**를 보였습니다.

![Performance Result](./images/result_graph.jpg)

| 연산 | MySQL (Disk) | Redis (Memory) | 개선율 |
| :---: | :---: | :---: | :---: |
| 추가 (Insert) | 1.37 ms | 0.23 ms | 5.8배 |
| 조회 (Select) | 1.05 ms | 0.15 ms | 6.6배 |
| 삭제 (Delete) | 1.74 ms | 0.14 ms | **12.3배** |
| **평균** | **1.39 ms** | **0.17 ms** | **7.8배** |

* **MySQL**: 데이터 안전을 위한 로그 기록과 인덱스 재정렬 과정에서 시간이 걸립니다.
* **Redis**: 해시 구조를 이용해 메모리에서 바로 처리하므로 추가적인 작업 부하가 거의 없습니다.

<br/>

## 4. 트러블슈팅

### 1. JPA `ddl-auto`를 활용한 테스트 데이터 격리
- **문제**: 반복 테스트 시 잔존 데이터로 인해 중복 키 에러(`Duplicate entry`)가 발생하고, 데이터 누적에 따른 인덱스 오버헤드로 측정 결과의 일관성 결여.
- **해결**: JPA의 **`ddl-auto: create`** 전략을 채택하여 앱 컨텍스트 로드 시마다 스키마를 초기화함으로써 테스트 환경을 완전히 격리함.
- **결과**: 외부 변수를 차단한 상태에서 독립적인 테스트 환경을 구축하고, 인덱스 상태에 따른 측정 오차 제거.
- **Blog**: [결정적 ID 생성에 따른 PK 충돌 해결 및 ddl-auto 기반 환경 초기화](https://velog.io/@bh1848/JPA-ddl-auto%EB%A5%BC-%EC%9D%B4%EC%9A%A9%ED%95%9C-%EC%8A%A4%ED%82%A4%EB%A7%88-%EC%B4%88%EA%B8%B0%ED%99%94)

### 2. 배치(Batch) 측정을 통한 시스템 타이머 해상도 한계 극복
- **문제**: Redis의 초고속 연산 처리가 OS 시스템 타이머의 정밀도 한계를 넘어서면서 단건 조회 시간이 **0ms**로 뭉뚱그려 측정되는 현상 확인.
- **해결**: 1,000건 단위의 배치 연산 총 소요 시간을 측정한 뒤 이를 평균값으로 나누어 역산하는 **산술 평균 방식** 도입.
- **결과**: 0ms로 측정되던 Redis의 실제 평균 속도(**0.17ms**)를 정밀하게 도출하여 데이터 신뢰성 확보.
- **Blog**: [System.currentTimeMillis() 해상도 한계 극복을 위한 통계적 보정 및 Redis Latency 산출](https://velog.io/@bh1848/currentTimeMillis%EC%9D%98-%ED%95%B4%EC%83%81%EB%8F%84-%ED%95%9C%EA%B3%84%EC%99%80-%EC%82%B0%EC%88%A0-%ED%8F%89%EA%B7%A0-%EC%B8%A1%EC%A0%95)

### 3. 네트워크 RTT 기반의 시스템 병목 구간 규명
- **문제**: 고사양 서버 환경임에도 실제 처리량(Throughput)이 이론적 기대치에 미치지 못하는 클라이언트 측 병목 현상 발생.
- **해결**: 측정 범위를 DB 내부 엔진 처리 시간에서 직렬화 및 네트워크 왕복 시간(**RTT**)을 포함한 **Client-Side Latency**로 재정의하여 병목 원인 분석.
- **결과**: 동기식 환경에서 전체 성능 임계치는 DB 엔진 성능보다 네트워크 IO 비용에 의해 결정됨을 지표로 입증.
- **Blog**: [Stop-and-Wait 프로토콜에 따른 Redis Throughput 저하 및 Client Side Latency 분석](https://velog.io/@bh1848/%EB%84%A4%ED%8A%B8%EC%9B%8C%ED%81%AC-RTT%EC%97%90-%EB%94%B0%EB%A5%B8-Redis-%EC%B2%98%EB%A6%AC%EB%9F%89-%EB%B3%91%EB%AA%A9-%EB%B6%84%EC%84%9D)

<br/>

## 5. 회고록
저장 매체에 따라서 실제 서비스의 사용자 경험에 어떤 영향을 주는지 확인할 수 있었습니다.  
특히 네트워크 RTT가 전체 지연 시간의 상당 부분을 차지한다는 것을 알았고  
이후 D-HASH 프로젝트에서 네트워크 홉을 추가하는 프록시 방식 대신, 지연 시간을 최소화할 수 있는 클라이언트 사이드 라우팅 방식을 선택하게 되었습니다.  
