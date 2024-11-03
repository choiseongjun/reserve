package com.example.reserve;


import com.example.reserve.entity.Stock;
import com.example.reserve.repository.ReservationRepository;
import com.example.reserve.repository.StockRepository;
import com.example.reserve.service.ReservationService;
import com.example.reserve.service.ReservationServiceV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ReservationServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationServiceTest.class);

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationServiceV2 reservationServiceV2;

//    @BeforeEach
//    void setUp() {
//        // Given: 초기 상품 및 재고 설정
//        stockRepository.deleteAll();
//        reservationRepository.deleteAll();
//
//        Stock stock = new Stock();
//        stock.setProductId(1L);
//        stock.setQuantity(100); // 초기 재고 설정
//        stockRepository.save(stock);
//    }
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
            stock.setQuantity(5000); // 초기 재고 설정
            stockRepository.save(stock);
        }
    }
//    @Test
//    @DisplayName("102명이 동시에 예약을 한다")
//    void shouldHandleConcurrentReservations() throws InterruptedException {
//        // Given: 1번 상품에 100개 재고 추가
////        Stock stock = new Stock();
////        stock.setProductId(1L);
////        stock.setQuantity(100);
////        stockRepository.save(stock);
//        // Given: 초기 상품 및 재고 설정
//        reservationRepository.deleteAll(); // 기존 예약 데이터 삭제
//
//        // Stock 데이터가 없으면 새로운 Stock을 삽입
//        if (stockRepository.findByProductId(1L).isEmpty()) {
//            Stock stock = new Stock();
//            stock.setProductId(1L);
//            stock.setQuantity(100); // 초기 재고 설정
//            stockRepository.save(stock);
//        }
//        // When: 102명이 동시에 예약 시도
//        int numberOfThreads = 102;
//        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
//
//        IntStream.range(0, numberOfThreads).forEach(i -> {
//            executorService.submit(() -> {
//                try {
//                    reservationService.createReservation(1L,  1); // 각 스레드에서 1개씩 예약
//                } catch (Exception e) {
//                    System.out.println("Reservation " + i + " failed: " + e.getMessage());
//                }
//            });
//        });
//
//        executorService.shutdown();
//        executorService.awaitTermination(1, TimeUnit.MINUTES);
//
//        // Then: 예약 성공 수와 재고 감소 확인
//        long successfulReservations = reservationRepository.count();
//        Stock updatedStock = stockRepository.findByProductId(1L).orElseThrow();
//
//        assertEquals(100, successfulReservations, "총 100개의 예약만 성공해야 합니다.");
//        assertEquals(0, updatedStock.getQuantity(), "재고는 0이어야 합니다.");
//    }

//    @Test
//    @DisplayName("1020명이 동시에 1000개를 예약한다")
//    void shouldHandleConcurrentReservationsFor1020People() throws InterruptedException {
//        // Given: 이미 초기 데이터 설정 완료
//        // Given: 초기 상품 및 재고 설정
//        reservationRepository.deleteAll(); // 기존 예약 데이터 삭제
//
//        // Stock 데이터가 없으면 새로운 Stock을 삽입
//        if (stockRepository.findByProductId(1L).isEmpty()) {
//            Stock stock = new Stock();
//            stock.setProductId(1L);
//            stock.setQuantity(100); // 초기 재고 설정
//            stockRepository.save(stock);
//        }
//        // When: 1020명이 동시에 예약 시도
//        int numberOfThreads = 1020;
//        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
//
//        IntStream.range(0, numberOfThreads).forEach(i -> {
//            executorService.submit(() -> {
//                try {
//                    reservationService.createReservation(1L, 1); // 각 스레드에서 1개씩 예약
//                } catch (Exception e) {
//                    System.out.println("Reservation " + i + " failed: " + e.getMessage());
//                }
//            });
//        });
//
//        executorService.shutdown();
//        executorService.awaitTermination(1, TimeUnit.MINUTES);
//
//        // Then: 예약 성공 수와 재고 감소 확인
//        long successfulReservations = reservationRepository.count();
//        Stock updatedStock = stockRepository.findByProductId(1L).orElseThrow();
//
//        assertEquals(1000, successfulReservations, "총 1000개의 예약만 성공해야 합니다.");
//        assertEquals(0, updatedStock.getQuantity(), "재고는 0이어야 합니다.");
//    }
    @Test
    @DisplayName("5020명이 동시에 5000개를 예약한다")
    void shouldHandleConcurrentReservationsFor5020People() throws InterruptedException {
        // Given: 이미 초기 데이터 설정 완료
        // Given: 초기 상품 및 재고 설정
//        reservationRepository.deleteAll(); // 기존 예약 데이터 삭제
//
//        // Stock 데이터가 없으면 새로운 Stock을 삽입
//        if (stockRepository.findByProductId(1L).isEmpty()) {
//            Stock stock = new Stock();
//            stock.setProductId(1L);
//            stock.setQuantity(100); // 초기 재고 설정
//            stockRepository.save(stock);
//        }
        // When: 5020명이 동시에 예약 시도
        int numberOfThreads = 5020;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        IntStream.range(0, numberOfThreads).forEach(i -> {
            executorService.submit(() -> {
                try {
                    reservationServiceV2.createReservation(1L, 1); // 각 스레드에서 1개씩 예약
                } catch (Exception e) {
                    LOGGER.error("Reservation " + i + " failed: " + e.getMessage());
                }
            });
        });

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        // Then: 예약 성공 수와 재고 감소 확인
        long successfulReservations = reservationRepository.count();
        Stock updatedStock = stockRepository.findByProductId(1L).orElseThrow();

        assertEquals(5000, successfulReservations, "총 5000개의 예약만 성공해야 합니다.");
        assertEquals(0, updatedStock.getQuantity(), "재고는 0이어야 합니다.");
    }
}