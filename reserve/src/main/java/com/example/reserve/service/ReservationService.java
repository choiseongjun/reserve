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
    /*
     * redission 분산락을 이용한 예약
     * */
    @Transactional(isolation = Isolation.SERIALIZABLE)  // 격리 수준 상향 조정
    public Reservation createReservation(Long productId, Long userId, int quantity) {
        String lockKey = "lock:product:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new RuntimeException("락을 얻지 못했습니다. 다시 시도하십시오.");
            }

            try {
                // 비관적 락을 사용하여 재고 조회
                Stock stock = stockRepository.findByProductIdWithPessimisticLock(productId)
                        .orElseThrow(() -> new IllegalArgumentException("해당 상품의 재고가 없습니다."));

                // 동시에 같은 사용자가 중복 예약하는 것을 방지
                if (reservationRepository.findByProductIdAndUserId(productId, userId).isPresent()) {
                    throw new IllegalStateException("이미 예약된 상품입니다.");
                }

                // 재고 확인 및 감소
                if (!stock.canDecrease(quantity)) {
                    throw new IllegalStateException("재고가 부족합니다.");
                }

                stock.decrease(quantity);
                // 명시적으로 재고 저장
                stockRepository.saveAndFlush(stock);

                // 재고 알림 발송
                if (stock.getQuantity() <= 8000) {
                    stockAlertPublisher.sendStockAlarmMessage(
                            String.format("Product ID %d의 재고가 8000개 이하입니다. 현재 재고: %d",
                                    productId, stock.getQuantity())
                    );
                }

                // 예약 생성 및 저장
                Reservation reservation = Reservation.builder()
                        .userId(userId)
                        .productId(productId)
                        .quantity(quantity)
//                        .status(ReservationStatus.PENDING)
                        .build();

                return reservationRepository.saveAndFlush(reservation);

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

//    @Transactional
//    public Reservation createReservation(Long productId, Long userId, int quantity) {
//        // 중복 예약 체크
//        String duplicateKey = "reservation:" + userId + ":" + productId;
//        Boolean isFirstRequest = redisTemplate.opsForValue().setIfAbsent(
//                duplicateKey, "PROCESSING", 60, TimeUnit.SECONDS);
//
//        if (Boolean.FALSE.equals(isFirstRequest)) {
//            throw new IllegalStateException("이미 예약을 진행 중입니다.");
//        }
//
//        try {
//            // 캐시된 재고 확인
//            int currentStock = stockCacheService.getCurrentStock(productId);
//            if (currentStock < quantity) {
//                throw new IllegalStateException("재고가 부족합니다.");
//            }
//
//            // 재고 감소
//            stockCacheService.decreaseStock(productId, quantity);
//
//            // 예약 생성
//            Reservation reservation = Reservation.builder()
//                    .userId(userId)
//                    .productId(productId)
//                    .quantity(quantity)
////                    .status(ReservationStatus.PENDING)
//                    .build();
//
//            // 예약 저장
//            reservation = reservationRepository.save(reservation);
//
//            // 성공 시 중복 예약 방지 키 업데이트
//            redisTemplate.opsForValue().set(duplicateKey, "COMPLETED", 24, TimeUnit.HOURS);
//
//            return reservation;
//
//        } catch (Exception e) {
//            // 실패 시 중복 예약 방지 키 삭제
//            redisTemplate.delete(duplicateKey);
//            throw e;
//        }
//    }


}