package com.example.reserve;


import com.example.reserve.entity.Stock;
import com.example.reserve.repository.ReservationRepository;
import com.example.reserve.repository.StockRepository;
import com.example.reserve.service.ReservationServiceV3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ReservationServiceTestV2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationServiceTestV2.class);

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationServiceV3 reservationServiceV3;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // Given: 초기 상품 및 재고 설정
        reservationRepository.deleteAll(); // 기존 예약 데이터 삭제

//        // 1000개 생성
//        if (stockRepository.findByProductId(1L).isEmpty()) {
//            Stock stock = new Stock();
//            stock.setProductId(1L);
//            stock.setQuantity(1000); // 초기 재고 설정
//            stockRepository.save(stock);
//        }
        // 5000개 생성
        if (stockRepository.findByProductId(1L).isEmpty()) {
            Stock stock = new Stock();
            stock.setProductId(1L);
            stock.setQuantity(100000); // 초기 재고 설정
            stockRepository.save(stock);
        }
    }
//    /*정합성 고려하고 test pass*/
//    @Test
//    @DisplayName("5020명이 동시에 5000개를 예약한다")
//    void shouldHandleConcurrentReservationsFor5020People() throws InterruptedException {
//        // Given
//        reservationServiceV2.initialize(); // 초기 상태 설정
//
//        int numberOfThreads = 5020;
//        CountDownLatch latch = new CountDownLatch(numberOfThreads);
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failCount = new AtomicInteger(0);
//
//        ExecutorService executorService = Executors.newFixedThreadPool(64);
//
//        // When
//        IntStream.range(0, numberOfThreads).forEach(i -> {
//            executorService.submit(() -> {
//                try {
//                    reservationServiceV2.createReservation(1L, 1);
//                    successCount.incrementAndGet();
//                } catch (Exception e) {
//                    failCount.incrementAndGet();
//                } finally {
//                    latch.countDown();
//                }
//            });
//        });
//
//        latch.await(2, TimeUnit.MINUTES);
//        executorService.shutdown();
//        executorService.awaitTermination(10, TimeUnit.SECONDS);
//
//        // 최종 정합성 체크를 위한 대기
//        Thread.sleep(2000);
//
//        // Then
//        Stock finalStock = stockRepository.findByProductId(1L).orElseThrow();
//        String redisStock = redisTemplate.opsForValue().get("stock:1");
//        String reservationCount = redisTemplate.opsForValue().get("reservation:count");
//        long actualReservations = reservationRepository.count();
//
//        assertEquals(0, finalStock.getQuantity(), "DB 재고는 0이다");
//        assertEquals("0", redisStock, "Redis 재고는 0이다");
//        assertEquals("5000", reservationCount, "예약 수는 5000이다");
//        assertEquals(5000, actualReservations, "실제 예약 수는 5000이어야 한다");
//        assertEquals(5000, successCount.get(), "성공한 예약 수는 5000이어야 한다");
//        assertEquals(20, failCount.get(), "실패한 예약 수는 20이어야 한다");
//    }

    /*test pass response time about 2m20s*/
//    @Test
//    @DisplayName("10100명이 동시에 10000개를 예약한다")
//    void shouldHandleConcurrentReservationsFor5020People() throws InterruptedException {
//        // Given
//        reservationServiceV2.initialize(); // 초기 상태 설정
//
//        int numberOfThreads = 10100;
//        CountDownLatch latch = new CountDownLatch(numberOfThreads);
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failCount = new AtomicInteger(0);
//
//        ExecutorService executorService = Executors.newFixedThreadPool(64);
//
//        // When
//        IntStream.range(0, numberOfThreads).forEach(i -> {
//            executorService.submit(() -> {
//                try {
//                    reservationServiceV2.createReservation(1L, 1);
//                    successCount.incrementAndGet();
//                } catch (Exception e) {
//                    failCount.incrementAndGet();
//                } finally {
//                    latch.countDown();
//                }
//            });
//        });
//
//        latch.await(2, TimeUnit.MINUTES);
//        executorService.shutdown();
//        executorService.awaitTermination(10, TimeUnit.SECONDS);
//
//        // 최종 정합성 체크를 위한 대기
//        Thread.sleep(2000);
//
//        // Then
//        Stock finalStock = stockRepository.findByProductId(1L).orElseThrow();
//        String redisStock = redisTemplate.opsForValue().get("stock:1");
//        String reservationCount = redisTemplate.opsForValue().get("reservation:count");
//        long actualReservations = reservationRepository.count();
//
//        assertEquals(0, finalStock.getQuantity(), "DB 재고는 0이다");
//        assertEquals("0", redisStock, "Redis 재고는 0이다");
//        assertEquals("10000", reservationCount, "예약 수는 5000이다");
//        assertEquals(10000, actualReservations, "실제 예약 수는 5000이어야 한다");
//        assertEquals(10000, successCount.get(), "성공한 예약 수는 5000이어야 한다");
//        assertEquals(100, failCount.get(), "실패한 예약 수는 100이어야 한다");
//    }
    @Test
    @DisplayName("100100명이 동시에 100000개를 예약한다")
    void shouldHandleConcurrentReservationsFor100100People() throws InterruptedException {
        // Given
        reservationServiceV3.initialize(); // 초기 상태 설정

        int numberOfThreads = 100100;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(13);

        // When
        IntStream.range(0, numberOfThreads).forEach(i -> {
            executorService.submit(() -> {
                try {
                    reservationServiceV3.createReservation(1L, 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        });

        latch.await(2, TimeUnit.MINUTES);
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // 최종 정합성 체크를 위한 대기
        Thread.sleep(2000);

        // Then
        Stock finalStock = stockRepository.findByProductId(1L).orElseThrow();
        String redisStock = redisTemplate.opsForValue().get("stock:1");
        String reservationCount = redisTemplate.opsForValue().get("reservation:count");
        long actualReservations = reservationRepository.count();

        assertEquals(0, finalStock.getQuantity(), "DB 재고는 0이다");
        assertEquals("0", redisStock, "Redis 재고는 0이다");
        assertEquals("100000", reservationCount, "예약 수는 100000이다");
        assertEquals(100100, actualReservations, "실제 예약 수는 100100이어야 한다");
        assertEquals(100000, successCount.get(), "성공한 예약 수는 100000이어야 한다");
        assertEquals(100, failCount.get(), "실패한 예약 수는 100이어야 한다");
    }
}