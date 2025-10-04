# ⚙️ MySQL vs Redis Performance Comparison  
**Comparative Evaluation of Data Processing Performance between MySQL and Redis**

---

## 📖 Overview
본 프로젝트는 관계형 데이터베이스(RDBMS)인 **MySQL**과 비관계형 데이터베이스(NoSQL)인 **Redis**의  
데이터 처리 성능을 실험적으로 비교·평가한 연구 코드입니다.  

해당 연구는 **인터넷정보학회논문지 (JICS)** 에 정식 게재되었으며,  
삽입·조회·삭제 성능을 기준으로 두 데이터베이스의 처리 속도를 정량적으로 분석하였습니다.

> 🧾 **논문 정보**  
> *MySQL과 Redis의 데이터 처리 성능 비교 평가*  
> Journal of Internet Computing and Services (JICS), Vol.25, No.3, pp.35–41, 2024  
> DOI: [10.7472/jksii.2024.25.3.35](https://doi.org/10.7472/jksii.2024.25.3.35)

---

## 🧪 Experiment Purpose
대규모 트래픽 환경에서 데이터 처리의 효율성과 확장성을 확보하기 위해  
RDBMS와 NoSQL 간의 구조적 차이가 **성능에 미치는 영향**을 검증합니다.

- **비교 대상**
  - MySQL (디스크 기반, 스키마 정의형 RDBMS)
  - Redis (메모리 기반, Key-Value 구조 NoSQL)
- **비교 항목**
  - 데이터 삽입(Insert)
  - 데이터 조회(Select)
  - 데이터 삭제(Delete)
- **평가 지표**
  - 평균 처리 시간 (ms)
  - 반복 실험 10회, 1,000회 단위 평균 기록

---

## ⚙️ Experiment Setup

| 구분 | 환경 구성 |
|------|------------|
| OS | Windows 11 (64-bit) |
| CPU | Intel Core i5-1340P (13th Gen, 1.9GHz) |
| RAM | 16GB |
| Language | Java 17 |
| Framework | Spring Boot |
| Database | MySQL 8.0, Redis 7.0 |
| Tool | Gradle, JPA |
| 측정 방식 | 10,000회 반복 수행, 1,000회 단위 평균 기록 후 로그 분석 |

---

## 🧩 Implementation Details

### 🔹 MySQL Test
- `INSERT INTO TEST_TABLE (ID, DATA) VALUES (?, ?)`
- `SELECT * FROM TEST_TABLE WHERE ID = ?`
- `DELETE FROM TEST_TABLE WHERE ID = ?`
- `ID`를 기본키로 설정하여 인덱스 기반 탐색 최적화  
- 10,000건 수행 후 평균 삽입: **1.3763ms**, 조회: **1.055ms**, 삭제: **1.7473ms**

### 🔹 Redis Test
- `redisTemplate.opsForValue().set(key, value)`
- `redisTemplate.opsForValue().get(key)`
- `redisTemplate.delete(key)`
- Key-Value 기반 메모리 저장 방식으로 성능 최적화  
- 10,000건 수행 후 평균 삽입: **0.2357ms**, 조회: **0.1595ms**, 삭제: **0.1417ms**

---

## 📊 Results Summary
| 항목 | MySQL 평균(ms) | Redis 평균(ms) | Redis 우위 배율 |
|------|----------------|----------------|----------------|
| Insert | 1.3763 | 0.2357 | **5.84x** |
| Select | 1.055 | 0.1595 | **6.61x** |
| Delete | 1.7473 | 0.1417 | **12.33x** |
| **평균** | — | — | **7.78x 빠름** |

> ✅ Redis는 인메모리 기반 구조 덕분에 모든 항목에서 MySQL 대비 압도적인 처리 성능을 보였습니다.

---

## 🧠 Insights
- **MySQL**은 데이터 일관성·무결성이 필요한 시스템에 적합
- **Redis**는 **대규모 데이터 처리·실시간 서비스·캐싱 환경**에서 최적 성능 제공
- 두 시스템의 **트레이드오프**를 정량적으로 분석해,  **대규모 트래픽 대응 및 캐싱 전략 수립의 기술적 근거**를 제시함
