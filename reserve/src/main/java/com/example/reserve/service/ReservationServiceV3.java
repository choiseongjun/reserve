package com.example.reserve.service;


import com.example.reserve.constants.ErrorCodes;
import com.example.reserve.constants.ReservationConstants;
import com.example.reserve.entity.Reservation;
import com.example.reserve.entity.Stock;
import com.example.reserve.event.RabbitMQObserver;
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
import org.springframework.transaction.annotation.Propagation;
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


    public int getQueuePositionByUserId(Long userId) {
        String queueKey = "reservation:queue";

        // 대기열 전체 조회
        List<String> queueItems = redisTemplate.opsForList().range(queueKey, 0, -1);
        if (queueItems == null || queueItems.isEmpty()) {
            return -1; // 대기열이 비어있음
        }

        // 유저 ID 기반 위치 확인
        int position = 0;
        for (String item : queueItems) {
            if (item.startsWith(userId + ":")) { // 유저 ID로 시작하는 항목 확인
                return position + 1; // 0부터 시작하므로 1을 더함
            }
            position++;
        }

        return -1; // 해당 유저를 찾지 못함
    }

    @Transactional
    public Reservation createReservation(Long userId, Long productId, int quantity) {
        String lockKey = ReservationConstants.LOCK_KEY_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!acquireLock(lock)) {
                // 락 획득 실패 -> 대기열에 추가
                addToQueue(userId, productId, quantity);
                throw new RuntimeException(ErrorCodes.FAILED_TO_ACQUIRE_LOCK);
            }

            // 락 획득 성공 -> 예약 처리
            return processReservation(userId, productId, quantity);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ErrorCodes.THREAD_INTERRUPTED, e);
        } finally {
            releaseLock(lock);
        }
    }

    private Long generateRandomUserId() {
        return (long) (new Random().nextInt(999_999_999) + 1);
    }

    private boolean acquireLock(RLock lock) throws InterruptedException {
        return lock.tryLock(1, 3, TimeUnit.SECONDS);
    }

    private void releaseLock(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private Reservation processReservation(Long userId, Long productId, int quantity) {
        try {
            checkProcessingLimit();
            int currentStock = getAndCheckRedisStock(quantity);
            log.info("Redis Stock Before DB Update: {}", currentStock);

            Stock stock = getAndCheckDBStock(productId, currentStock);
            log.info("DB Stock Before Update: {}", stock.getQuantity());

            updateStock(stock, quantity);
            log.info("DB Stock After Update: {}", stock.getQuantity());

            Reservation reservation = saveReservation(userId, productId, quantity);

            updateRedisStock(quantity);
            log.info("Redis Stock After Update: {}", redisTemplate.opsForValue().get("stock:1"));

            verifyConsistency(stock);

//            notifyObservers(reservation);

            return reservation;
        } finally {
            redisTemplate.opsForValue().decrement(ReservationConstants.PROCESS_KEY);
        }
    }

    private void checkProcessingLimit() {
        Long processing = redisTemplate.opsForValue().increment(ReservationConstants.PROCESS_KEY);
        if (processing != null) {
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

        log.info("quantity= {}",quantity);
        log.info("stock quantity= {}",stock.getQuantity());
        if (stock.getQuantity() < quantity) {
            throw new IllegalStateException(ErrorCodes.INSUFFICIENT_DB_STOCK);
        }

        return stock;
    }

    private void updateStock(Stock stock, int quantity) {
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }

    protected Reservation saveReservation(Long userId, Long productId, int quantity) {
        Reservation reservation = Reservation.builder()
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .build();
        Reservation savedReservation = reservationRepository.saveAndFlush(reservation); // 강제로 플러시
        log.info("Reservation saved: id={}, userId={}, productId={}, quantity={}", savedReservation.getId(), userId, productId, quantity);
        return savedReservation;
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
                log.error("Data consistency mismatch detected! DB Stock: {}, Redis Stock: {}, Reservation Count: {}",
                        stock.getQuantity(), redisStockInt, reservationCountInt);

                redisTemplate.opsForValue().set(ReservationConstants.STOCK_KEY, String.valueOf(stock.getQuantity()));
                redisTemplate.opsForValue().set(ReservationConstants.RESERVATION_COUNT_KEY,
                        String.valueOf(ReservationConstants.TOTAL_QUANTITY - stock.getQuantity()));
            }
        }
    }

    // 대기열에 추가
    private void addToQueue(Long userId, Long productId, int quantity) {
        String queueKey = "reservation:queue";
        String queueItem = userId + ":" + productId + ":" + quantity;

        redisTemplate.opsForList().rightPush(queueKey, queueItem); // 대기열에 추가
        log.info("Request added to queue. User ID: {}, Product ID: {}, Quantity: {}", userId, productId, quantity);
    }

    // 대기열 처리
    @Scheduled(fixedRate = 100) // 0.1초마다 실행
    @Transactional
    public void processQueue() {
        String queueKey = "reservation:queue";

        for (int i = 0; i < 10; i++) { // 한 번에 최대 10개 처리
            String queueItem = redisTemplate.opsForList().leftPop(queueKey);
            if (queueItem == null) {
                break; // 대기열이 비어있으면 종료
            }

            String[] parts = queueItem.split(":");
            Long userId = Long.parseLong(parts[0]);
            Long productId = Long.parseLong(parts[1]);
            int quantity = Integer.parseInt(parts[2]);

            try {
                log.info("Processing queued request. User ID: {}, Product ID: {}, Quantity: {}", userId, productId, quantity);
                processReservation(userId, productId, quantity);
            } catch (Exception e) {
                log.error("Failed to process queued request. User ID: {}, Product ID: {}, Quantity: {}", userId, productId, quantity, e);
                addToQueue(userId, productId, quantity); // 실패 시 다시 대기열에 추가
            }
        }
    }
    public int getQueuePosition(Long userId, Long productId, int quantity) {
        String queueKey = "reservation:queue";

        // 대기열 전체 조회
        List<String> queueItems = redisTemplate.opsForList().range(queueKey, 0, -1);
        if (queueItems == null) {
            return -1; // 대기열이 비어있음
        }

        // 요청 항목 생성
        String targetItem = userId + ":" + productId + ":" + quantity;

        // 요청 위치 검색
        int position = 0;
        for (String item : queueItems) {
            if (item.equals(targetItem)) {
                return position + 1; // 대기열은 0부터 시작하므로 1을 더해 반환
            }
            position++;
        }

        return -1; // 요청을 찾지 못함
    }
}
