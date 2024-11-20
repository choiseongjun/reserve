package com.example.reserve;

import com.example.reserve.event.ReservationPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class RabbitMQIntegrationTest {

    @Autowired
    private ReservationPublisher reservationPublisher;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    public void testEnqueueAndConsumeReservation() throws InterruptedException {
        // Given
        Long userId = 12345L;
        Long productId = 1L;
        int quantity = 5;

        // When
        reservationPublisher.publishReservationRequest(userId, productId, quantity);

        // Simulate some delay for Consumer to process the message
        Thread.sleep(5000);

        // Then
        // 대기열 처리 후 결과를 Redis 또는 로그에서 확인
        // 예: Redis에 저장된 처리 결과 검증
        String queueKey = "reservation:queue";
        Long queueSize = redisTemplate.opsForList().size(queueKey);

        System.out.println("Queue size after processing: " + queueSize);
        assertTrue(queueSize == 0 || queueSize == null);
    }
}
