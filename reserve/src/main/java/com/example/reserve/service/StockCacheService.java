package com.example.reserve.service;

import com.example.reserve.entity.Stock;
import com.example.reserve.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockCacheService {
    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final long STOCK_CACHE_TTL = 60 * 60; // 1시간

    private final RedisTemplate<String, String> redisTemplate;
    private final StockRepository stockRepository;
    private final RedissonClient redissonClient;

    private String getStockKey(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    // 재고 조회 (캐시 우선)
    public int getCurrentStock(Long productId) {
        String stockKey = getStockKey(productId);
        String cachedStock = redisTemplate.opsForValue().get(stockKey);

        if (cachedStock != null) {
            return Integer.parseInt(cachedStock);
        }

        // 캐시 미스시 DB에서 조회하고 캐시 갱신
        return syncStockCache(productId);
    }

    // DB의 재고를 캐시에 동기화
    private int syncStockCache(Long productId) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        String stockKey = getStockKey(productId);
        redisTemplate.opsForValue().set(stockKey,
                String.valueOf(stock.getQuantity()),
                STOCK_CACHE_TTL,
                TimeUnit.SECONDS);

        return stock.getQuantity();
    }

    // 재고 감소 처리
    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        String stockKey = getStockKey(productId);
        String lockKey = "lock:" + stockKey;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new RuntimeException("재고 처리 중 오류가 발생했습니다.");
            }

            try {
                // Redis 캐시에서 현재 재고 확인
                int currentStock = getCurrentStock(productId);
                if (currentStock < quantity) {
                    throw new IllegalStateException("재고가 부족합니다.");
                }

                // DB 재고 감소
                Stock stock = stockRepository.findByProductIdWithPessimisticLock(productId)
                        .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

                stock.decrease(quantity);
                stockRepository.saveAndFlush(stock);

                // 캐시 업데이트
                redisTemplate.opsForValue().set(stockKey,
                        String.valueOf(stock.getQuantity()),
                        STOCK_CACHE_TTL,
                        TimeUnit.SECONDS);

            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재고 처리 중 인터럽트가 발생했습니다.", e);
        }
    }
}