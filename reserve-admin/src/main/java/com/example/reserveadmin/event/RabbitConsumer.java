package com.example.reserveadmin.event;

import com.example.reserveadmin.dto.RabbitMessage;
import com.example.reserveadmin.dto.StockAlarmMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class RabbitConsumer {

//    @RabbitListener(queues = "#{queue.name}")
//    public void consume(RabbitMessage message){
//        log.info("[reserve message]:{}", message);
//    }
//    @RabbitListener(queues = "#{queue.name}")
//    public void consume(StockAlarmMessage message){
//        log.info("[reserve message]:{}", message);
//    }
    @RabbitListener(queues = "#{queue.name}")
    public void consume(String message){
        log.info("[reserve message]:{}", message);
    }
    /**
     * PubSub Consumer 1
     */
    @RabbitListener(queues = "#{subQueue1.name}")
    public void consumeSub1(StockAlarmMessage message){
        log.info("[consumeSub1]: {}", message);
    }

    /**
     * PubSub Consumer 2
     */
    @RabbitListener(queues = "#{subQueue2.name}")
    public void consumeSub2(StockAlarmMessage message){
        log.info("[consumeSub2]: {}", message);
    }
}