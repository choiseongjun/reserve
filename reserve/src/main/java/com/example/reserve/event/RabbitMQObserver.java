package com.example.reserve.event;

import com.example.reserve.entity.Reservation;
import com.example.reserve.service.ReservationObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


@Component
@Slf4j
public class RabbitMQObserver implements ReservationObserver {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public RabbitMQObserver(RabbitTemplate rabbitTemplate,
                            @Value("${rabbitmq.exchange}") String exchange,
                            @Value("${rabbitmq.routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void onReservationCreated(Reservation reservation) {
        rabbitTemplate.convertAndSend(exchange, routingKey, reservation);
        log.info("RabbitMQ에 메시지를 전송했습니다: {} ",reservation.getId());
    }
}