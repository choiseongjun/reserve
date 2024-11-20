package com.example.reserve.event;

import com.example.reserve.service.ReservationServiceV3;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationConsumer {

    private final ReservationServiceV3 reservationService;

    public ReservationConsumer(ReservationServiceV3 reservationService) {
        this.reservationService = reservationService;
    }

    @RabbitListener(queues = "reservation.queue")
    public void consumeReservationRequest(String message) {
        // 메시지 파싱
        String[] parts = message.split(":");
        Long userId = Long.parseLong(parts[0]);
        Long productId = Long.parseLong(parts[1]);
        int quantity = Integer.parseInt(parts[2]);

        try {
            System.out.println("Processing reservation request: " + message);
            reservationService.createReservation( userId, productId, quantity); // 예약 처리
        } catch (Exception e) {
            System.err.println("Failed to process reservation request: " + message);
            // 실패 시 재처리 로직 추가 가능
        }
    }
}