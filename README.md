✨ 주요 기능

동시성 재고 관리
분산 락을 통한 동시성 제어

🔧 기술 스택

Java Spring Boot
PostgreSQL
Redis
RabbitMQ
Distributed Lock
Concurrent Programming

🌟 핵심 기술적 도전 과제
동시성 제어 메커니즘

비관적 락 (PostgreSQL)
분산 락 (Redis)
동시 요청 처리 (1010명의 사용자, 1000개 재고)

동시성 처리 전략

CountDownLatch를 이용한 동시 요청 동기화
ExecutorService를 통한 병렬 처리
랜덤 사용자 ID 생성
예약 성공/실패 카운트 추적

🔒 동시성 보장 로직

재고 감소 시 동시성 충돌 방지
정확히 1000개의 재고만 예약 허용
10개의 요청은 자동으로 실패 처리

🔍 테스트 시나리오

1010명의 사용자가 동시에 재고 예약 시도
1000개의 재고에 대한 정확한 예약 보장
실패/성공 케이스 명확한 분리

🎯 주요 검증 포인트

DB 재고 일관성
Redis 재고 동기화
예약 성공/실패 카운트 검증

📡 알림 시스템

RabbitMQ를 통한 예약 완료 알림 구현

📊 성능 지표

99.01% 동시성 요청 처리 성공률
밀리초 단위 트랜잭션 처리

🛠 추가 개선 방향

더 robust한 에러 핸들링
고급 모니터링 시스템 도입
성능 튜닝
Rate Limit
Circit Breaker
