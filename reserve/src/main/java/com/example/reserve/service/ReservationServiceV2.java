package com.example.reserve.service;



import com.example.reserve.entity.Reservation;
import com.example.reserve.entity.Stock;
import com.example.reserve.event.RabbitMQObserver;
import com.example.reserve.event.StockAlertPublisher;
import com.example.reserve.repository.ReservationRepository;
import com.example.reserve.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceV2 {
    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;
    private final StockAlertPublisher stockAlertPublisher;
    private final RabbitMQObserver rabbitMQObserver;


    private static final String STOCK_KEY = "stock:1";
    private static final String RESERVATION_COUNT_KEY = "reservation:count";
    private static final String PROCESS_KEY = "processing:1";
    private static final int TOTAL_QUANTITY = 100000;
    private final List<ReservationObserver> observers = new ArrayList<>();

    @PostConstruct
    public void init() {
        addObserver(rabbitMQObserver);
    }
    public void addObserver(ReservationObserver observer) {
        observers.add(observer);
    }
    private void notifyObservers(Reservation reservation) {
        for (ReservationObserver observer : observers) {
            observer.onReservationCreated(reservation);
        }
    }
//    public void initialize() {
//        RLock lock = redissonClient.getLock("init:lock");
//        try {
//            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
//                try {
//                    // Redis 초기화
//                    Stock stock = stockRepository.findByProductId(1L)
//                            .orElseThrow(() -> new IllegalArgumentException("재고 없음"));
//
//                    redisTemplate.delete(STOCK_KEY);
//                    redisTemplate.delete(RESERVATION_COUNT_KEY);
//                    redisTemplate.delete(PROCESS_KEY);
//
//                    redisTemplate.opsForValue().set(STOCK_KEY, String.valueOf(TOTAL_QUANTITY));
//                    redisTemplate.opsForValue().set(RESERVATION_COUNT_KEY, "0");
//
//                    // DB 초기화
//                    stock.setQuantity(TOTAL_QUANTITY);
//                    stockRepository.save(stock);
//
//                    // 기존 예약 정보 삭제
//                    reservationRepository.deleteAll();
//                } finally {
//                    lock.unlock();
//                }
//            }
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new RuntimeException("초기화 실패", e);
//        }
//    }

    @Transactional
    public Reservation createReservation(Long productId, int quantity) {
        String lockKey = "lock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(1, 3, TimeUnit.SECONDS)) {
                throw new RuntimeException("락 획득 실패");
            }

            try {
                // 1. 현재 처리 중인 요청 수 확인
                Long processing = redisTemplate.opsForValue().increment(PROCESS_KEY);
                if (processing > TOTAL_QUANTITY) {
                    redisTemplate.opsForValue().decrement(PROCESS_KEY);
                    throw new IllegalStateException("재고 소진");
                }

                // 2. Redis 재고 확인 및 감소
                String currentStockStr = redisTemplate.opsForValue().get(STOCK_KEY);
                if (currentStockStr == null) {
                    throw new IllegalStateException("재고 정보 없음");
                }

                int currentStock = Integer.parseInt(currentStockStr);
                if (currentStock < quantity) {
                    redisTemplate.opsForValue().decrement(PROCESS_KEY);
                    throw new IllegalStateException("재고 부족");
                }

                // 3. DB 재고 확인 및 감소
                Stock stock = stockRepository.findByProductIdWithPessimisticLock(productId)
                        .orElseThrow(() -> new IllegalArgumentException("재고 없음"));

                if (stock.getQuantity() < quantity) {
                    redisTemplate.opsForValue().decrement(PROCESS_KEY);
                    throw new IllegalStateException("DB 재고 부족");
                }

                // 4. 재고 감소
                stock.decrease(quantity);
                Stock savedStock = stockRepository.saveAndFlush(stock);

                // 5. Redis 재고 업데이트
                redisTemplate.opsForValue().decrement(STOCK_KEY, quantity);

                // 6. 예약 생성
                Reservation reservation = Reservation.builder()
                        .productId(productId)
                        .userId(generateRandomUserId())
                        .quantity(quantity)
                        .build();

                Reservation savedReservation = reservationRepository.saveAndFlush(reservation);

                // 7. 예약 카운트 증가
                redisTemplate.opsForValue().increment(RESERVATION_COUNT_KEY);

                // 8. 처리 완료
                redisTemplate.opsForValue().decrement(PROCESS_KEY);

                // 9. 검증
                verifyConsistency(savedStock);

                // 10.알림 처리
                notifyObservers(reservation);


                return savedReservation;
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted", e);
        }
    }

    private void verifyConsistency(Stock stock) {
        String redisStock = redisTemplate.opsForValue().get(STOCK_KEY);
        String reservationCount = redisTemplate.opsForValue().get(RESERVATION_COUNT_KEY);

        if (redisStock != null && reservationCount != null) {
            int redisStockInt = Integer.parseInt(redisStock);
            int reservationCountInt = Integer.parseInt(reservationCount);

            if (redisStockInt != stock.getQuantity() ||
                    (TOTAL_QUANTITY - reservationCountInt) != stock.getQuantity()) {
                log.error("데이터 정합성 불일치 detected! DB Stock: {}, Redis Stock: {}, Reservation Count: {}",
                        stock.getQuantity(), redisStockInt, reservationCountInt);

                redisTemplate.opsForValue().set(STOCK_KEY, String.valueOf(stock.getQuantity()));
                redisTemplate.opsForValue().set(RESERVATION_COUNT_KEY,
                        String.valueOf(TOTAL_QUANTITY - stock.getQuantity()));
            }
        }
    }

    private Long generateRandomUserId() {
        return (long) (new Random().nextInt(999_999_999) + 1);
    }

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void checkAndRepairConsistency() {
        RLock lock = redissonClient.getLock("consistency:lock");
        try {
            if (lock.tryLock(1, 3, TimeUnit.SECONDS)) {
                try {
                    Stock stock = stockRepository.findByProductId(1L)
                            .orElseThrow(() -> new IllegalArgumentException("재고 없음"));
                    verifyConsistency(stock);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Consistency check interrupted", e);
        }
    }
}