package com.example.reserveadmin.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StockAlertListener {

    @EventListener
    public void handleStockAlert(StockAlertEvent event) {
        log.info("Received stock alert: " + event.getMessage());
//        System.out.println("Received stock alert: " + event.getMessage());
        // 추가적으로 이메일이나 SMS를 보내는 로직 등을 구현할 수 있습니다.
    }
}