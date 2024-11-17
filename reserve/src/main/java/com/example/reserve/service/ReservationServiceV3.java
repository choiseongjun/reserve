package com.example.reserve.service;


import com.example.reserve.constants.ErrorCodes;
import com.example.reserve.constants.ReservationConstants;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceV3 {
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

    @Transactional
    public Reservation createReservation(Long productId, int quantity) {
        String lockKey = ReservationConstants.LOCK_KEY_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!acquireLock(lock)) {
                throw new RuntimeException(ErrorCodes.FAILED_TO_ACQUIRE_LOCK);
            }
            return processReservation(productId, quantity);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ErrorCodes.THREAD_INTERRUPTED, e);
        } finally {
            releaseLock(lock);
        }
    }

    private boolean acquireLock(RLock lock) throws InterruptedException {
        return lock.tryLock(1, 3, TimeUnit.SECONDS);
    }

    private void releaseLock(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private Reservation processReservation(Long productId, int quantity) {
        try {
            checkProcessingLimit();
            int currentStock = getAndCheckRedisStock(quantity);

            Stock stock = getAndCheckDBStock(productId, currentStock);

            updateStock(stock, quantity);
            Reservation reservation = saveReservation(productId, quantity);

            updateRedisStock(quantity);
            verifyConsistency(stock);

            notifyObservers(reservation);

            return reservation;
        } finally {
            redisTemplate.opsForValue().decrement(ReservationConstants.PROCESS_KEY);
        }
    }

    private void checkProcessingLimit() {
        Long processing = redisTemplate.opsForValue().increment(ReservationConstants.PROCESS_KEY);
        if(processing!=null){
            if (processing > ReservationConstants.TOTAL_QUANTITY) {
                redisTemplate.opsForValue().decrement(ReservationConstants.PROCESS_KEY);
                throw new IllegalStateException(ErrorCodes.STOCK_EXHAUSTED);
            }
        }
    }

    private int getAndCheckRedisStock(int quantity) {
        String currentStockStr = redisTemplate.opsForValue().get(ReservationConstants.STOCK_KEY);
        if (currentStockStr == null) {
            throw new IllegalStateException(ErrorCodes.STOCK_INFO_NOT_FOUND);
        }

        int currentStock = Integer.parseInt(currentStockStr);
        if (currentStock < quantity) {
            throw new IllegalStateException(ErrorCodes.INSUFFICIENT_REDIS_STOCK);
        }

        return currentStock;
    }

    private Stock getAndCheckDBStock(Long productId, int quantity) {
        Stock stock = stockRepository.findByProductIdWithPessimisticLock(productId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCodes.STOCK_NOT_FOUND_IN_DB));

        if (stock.getQuantity() < quantity) {
            throw new IllegalStateException(ErrorCodes.INSUFFICIENT_DB_STOCK);
        }

        return stock;
    }

    private void updateStock(Stock stock, int quantity) {
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }

    private Reservation saveReservation(Long productId, int quantity) {
        Reservation reservation = Reservation.builder()
                .productId(productId)
                .userId(generateRandomUserId())
                .quantity(quantity)
                .build();
        return reservationRepository.saveAndFlush(reservation);
    }

    private void updateRedisStock(int quantity) {
        redisTemplate.opsForValue().decrement(ReservationConstants.STOCK_KEY, quantity);
        redisTemplate.opsForValue().increment(ReservationConstants.RESERVATION_COUNT_KEY);
    }

    private void notifyObservers(Reservation reservation) {
        observers.forEach(observer -> observer.onReservationCreated(reservation));
    }

    private void verifyConsistency(Stock stock) {
        String redisStock = redisTemplate.opsForValue().get(ReservationConstants.STOCK_KEY);
        String reservationCount = redisTemplate.opsForValue().get(ReservationConstants.RESERVATION_COUNT_KEY);

        if (redisStock != null && reservationCount != null) {
            int redisStockInt = Integer.parseInt(redisStock);
            int reservationCountInt = Integer.parseInt(reservationCount);

            if (redisStockInt != stock.getQuantity() ||
                    (ReservationConstants.TOTAL_QUANTITY - reservationCountInt) != stock.getQuantity()) {
                // this section not sync redis & rdb
                log.error("Data consistency mismatch detected! DB Stock: {}, Redis Stock: {}, Reservation Count: {}",
                        stock.getQuantity(), redisStockInt, reservationCountInt);

                redisTemplate.opsForValue().set(ReservationConstants.STOCK_KEY, String.valueOf(stock.getQuantity()));
                redisTemplate.opsForValue().set(ReservationConstants.RESERVATION_COUNT_KEY,
                        String.valueOf(ReservationConstants.TOTAL_QUANTITY - stock.getQuantity()));
            }
        }
    }

    private Long generateRandomUserId() {
        return (long) (new Random().nextInt(999_999_999) + 1);
    }

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void checkAndRepairConsistency() {
        RLock lock = redissonClient.getLock(ReservationConstants.CONSISTENCY_LOCK_KEY);
        try {
            if (acquireLock(lock)) {
                try {
                    Stock stock = stockRepository.findByProductId(1L)
                            .orElseThrow(() -> new IllegalArgumentException(ErrorCodes.STOCK_NOT_FOUND_IN_DB));
                    verifyConsistency(stock);
                } finally {
                    releaseLock(lock);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(ErrorCodes.THREAD_INTERRUPTED, e);
        }
    }
}