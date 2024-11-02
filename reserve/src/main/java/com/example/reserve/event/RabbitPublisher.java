package com.example.reserve.event;

import com.example.reserve.dto.RabbitMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange topicExchange;

    public void sendMessage(RabbitMessage message){
        rabbitTemplate.convertAndSend(topicExchange.getName(), "default.key", message);
    }
    public void sendSubMessage(RabbitMessage message){
        rabbitTemplate.convertAndSend(topicExchange.getName(), "subQueue1.key", message);
    }
    public void sendStockAlarmMessage(String message){
        rabbitTemplate.convertAndSend(topicExchange.getName(), "subQueue1.key", message);
    }
}