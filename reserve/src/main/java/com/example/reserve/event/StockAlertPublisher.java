package com.example.reserve.event;

import com.example.reserve.event.StockAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockAlertPublisher {

//    private final AmqpTemplate amqpTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange topicExchange;
//    @Value("${rabbitmq.exchange}")
//    private String exchange;


//    public void publishStockAlert(String message) {
//        String routingKey = "stock.alert"; // 토픽 라우팅 키
////        amqpTemplate.convertAndSend(exchange, routingKey, message);
////        new StockAlertEvent(message);
//        rabbitPublisher.sendSubMessage(topicExchange.getName(), "default.key",message);
//
//        log.info("Sent alert message to RabbitMQ: " + message);
////        System.out.println("Sent alert message to RabbitMQ: " + message);
//    }
    public void sendStockAlarmMessage(String message){
        rabbitTemplate.convertAndSend(topicExchange.getName(), "default.key", message);
    }
}