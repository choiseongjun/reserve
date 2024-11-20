package com.example.reserve.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class ReservationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ReservationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

//    public void publishReservationRequest(Long userId, Long productId, int quantity) {
//        String message = userId + ":" + productId + ":" + quantity;
//        rabbitTemplate.convertAndSend("reservation.exchange", "reservation.routingKey", message);
//        System.out.println("Reservation request added to RabbitMQ: " + message);
//    }
    public String publishReservationRequest(Long userId, Long productId, int quantity) {
        String requestId = UUID.randomUUID().toString(); // 고유 요청 ID 생성

        ReservationMessage message = new ReservationMessage(userId, productId, quantity, requestId);
        rabbitTemplate.convertAndSend("reservation.exchange", "reservation.routingKey", message);
        log.info("Reservation request published: {}", message);

        return requestId; // 생성된 요청 ID를 반환
    }
}