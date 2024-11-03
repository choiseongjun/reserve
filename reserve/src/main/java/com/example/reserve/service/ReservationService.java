package com.example.reserve.service;


import com.example.reserve.dto.StockAlarmMessage;
import com.example.reserve.entity.Reservation;
import com.example.reserve.entity.Stock;
import com.example.reserve.event.StockAlertPublisher;
import com.example.reserve.repository.ReservationRepository;
import com.example.reserve.repository.StockRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;
//    private final StringRedisTemplate redisTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private final StockAlertPublisher stockAlertPublisher;
    private final RedissonClient redissonClient;
    private final StockCacheService stockCacheService;

    private static final int LOCK_WAIT_TIME = 1;
    private static final int LOCK_LEASE_TIME = 3;
    /*
    * 분산락을 이용한 예약
    * */
//    @Transactional(isolation = Isolation.REPEATABLE_READ)
//    public Reservation createReservation(Long productId, Long userId, int quantity) {
//        String lockKey = "lock:product:" + productId;
//
//        while (true) {
//            Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);
//
//            if (lockAcquired != null && lockAcquired) {
//                try {
//                    // Stock을 가져오기
//                    Stock stock = stockRepository.findByProductId(productId)
//                            .orElseThrow(() -> new IllegalArgumentException("해당 상품의 재고가 없습니다."));
//
//                    // 재고 확인 및 감소
//                    if (stock.canDecrease(quantity)) {
//                        stock.decrease(quantity);
//                        stockRepository.save(stock); // 재고 저장
//
//                        // 재고가 5000개 이하로 떨어지면 알림 발행
//                        if (stock.getQuantity() <= 8000) {
////                            StockAlarmMessage.builder()
////                                    .prodId(productId)
////                                    .remainStock(stock.getQuantity())
////                                    .build();
//                            stockAlertPublisher.sendStockAlarmMessage("Product ID " + productId + "의 재고가 5000개 이하입니다. 현재 재고: " + stock.getQuantity());
//
//                        }
//
//
//                        // 예약 생성
//                        Reservation reservation = new Reservation();
//                        reservation.setUserId(userId);
//                        reservation.setProductId(productId);
//                        reservation.setQuantity(quantity);
//                        reservation.setStatus(Reservation.ReservationStatus.PENDING);
//
//                        // 기존 예약이 없는 경우에만 새로운 예약을 저장
//                        if (reservationRepository.findByProductIdAndUserId(productId, userId).isEmpty()) {
//                            return reservationRepository.save(reservation); // 예약 저장
//                        } else {
//                            throw new IllegalStateException("이미 예약된 상품입니다.");
//                        }
//                    } else {
//                        throw new IllegalStateException("재고가 부족합니다.");
//                    }
//                } finally {
//                    // 락 해제
//                    redisTemplate.delete(lockKey);
//                }
//            } else {
//                try {
//                    Thread.sleep(100); // 100ms 대기
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    throw new RuntimeException("Thread interrupted", e);
//                }
//            }
//        }
//    }
    private final Random random = new Random();

    private Long generateRandomUserId() {
        return (long) (random.nextInt(9_999_999) + 1);
    }

    /*
     * redission 분산락을 이용한 예약
     * */
//    @Transactional(isolation = Isolation.SERIALIZABLE)  // 격리 수준 상향 조정
//    public Reservation createReservation(Long productId,  int quantity) {
//        String lockKey = "lock:product:" + productId;
//        RLock lock = redissonClient.getLock(lockKey);
//        Long randomUserId = generateRandomUserId();
//        Long userId = randomUserId;
//
//        try {
//            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
//                throw new RuntimeException("락을 얻지 못했습니다. 다시 시도하십시오.");
//            }
//
//            try {
//                // 비관적 락을 사용하여 재고 조회
//                Stock stock = stockRepository.findByProductIdWithPessimisticLock(productId)
//                        .orElseThrow(() -> new IllegalArgumentException("해당 상품의 재고가 없습니다."));
//
//                // 동시에 같은 사용자가 중복 예약하는 것을 방지
//                if (reservationRepository.findByProductIdAndUserId(productId, userId).isPresent()) {
//                    throw new IllegalStateException("이미 예약된 상품입니다.");
//                }
//
//                // 재고 확인 및 감소
//                if (!stock.canDecrease(quantity)) {
//                    throw new IllegalStateException("재고가 부족합니다.");
//                }
//
//                stock.decrease(quantity);
//                // 명시적으로 재고 저장
//                stockRepository.saveAndFlush(stock);
//
//                // 재고 알림 발송
//                if (stock.getQuantity() <= 8000) {
//                    stockAlertPublisher.sendStockAlarmMessage(
//                            String.format("Product ID %d의 재고가 8000개 이하입니다. 현재 재고: %d",
//                                    productId, stock.getQuantity())
//                    );
//                }
//
//                // 예약 생성 및 저장
//                Reservation reservation = Reservation.builder()
//                        .userId(userId)
//                        .productId(productId)
//                        .quantity(quantity)
////                        .status(ReservationStatus.PENDING)
//                        .build();
//
//                return reservationRepository.saveAndFlush(reservation);
//
//            } finally {
//                if (lock.isHeldByCurrentThread()) {
//                    lock.unlock();
//                }
//            }
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new RuntimeException("Thread interrupted", e);
//        }
//    }
    //데이터 불일치 정합성 문제 처리량은 올랐으나 정합성이 문제됨
    @Transactional(isolation = Isolation.READ_COMMITTED)  // 격리 수준 낮춤
    public Reservation createReservation(Long productId, int quantity) {
        String lockKey = "lock:product:" + productId;
        RLock lock = redissonClient.getLock(lockKey);
        Long userId = generateRandomUserId();

        try {
            // 락 획득 대기 시간 축소
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                throw new RuntimeException("락 획득 실패");
            }

            try {
                // 캐시된 재고 확인
                String stockKey = "stock:" + productId;
                String currentStockStr = redisTemplate.opsForValue().get(stockKey);
                int currentStock;

                if (currentStockStr == null) {
                    // 캐시 미스 시 DB에서 조회하고 캐시 설정
                    Stock stock = stockRepository.findByProductId(productId)
                            .orElseThrow(() -> new IllegalArgumentException("재고 없음"));
                    currentStock = stock.getQuantity();
                    redisTemplate.opsForValue().set(stockKey, String.valueOf(currentStock));
                } else {
                    currentStock = Integer.parseInt(currentStockStr);
                }

                // 재고 확인
                if (currentStock < quantity) {
                    throw new IllegalStateException("재고 부족");
                }

                // Redis에서 재고 감소 (atomic 연산)
                Long newStock = redisTemplate.opsForValue().increment(stockKey, -quantity);

                if (newStock < 0) {
                    // 재고가 음수가 되면 롤백
                    redisTemplate.opsForValue().increment(stockKey, quantity);
                    throw new IllegalStateException("재고 부족");
                }

                // 예약 생성 - 배치 처리를 위해 메모리에 먼저 저장
                Reservation reservation = Reservation.builder()
                        .userId(userId)
                        .productId(productId)
                        .quantity(quantity)
                        .build();

                reservation = reservationRepository.save(reservation);

                // 재고 알림은 비동기로 처리
                if (newStock <= 8000) {
                    CompletableFuture.runAsync(() ->
                            stockAlertPublisher.sendStockAlarmMessage(
                                    String.format("Product ID %d의 재고가 8000개 이하입니다. 현재 재고: %d",
                                            productId, newStock)
                            )
                    );
                }

                // DB 재고 업데이트는 배치로 처리하기 위해 지연
                if (newStock % 100 == 0 || newStock == 0) {
                    Stock stock = stockRepository.findByProductId(productId)
                            .orElseThrow(() -> new IllegalArgumentException("재고 없음"));
                    stock.setQuantity(newStock.intValue());
                    stockRepository.save(stock);
                }

                return reservation;

            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted", e);
        }
    }


}