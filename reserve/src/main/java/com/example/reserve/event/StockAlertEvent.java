package com.example.reserve.event;

import org.springframework.context.ApplicationEvent;

public class StockAlertEvent extends ApplicationEvent {
    private final String message;

    public StockAlertEvent(Object source, String message) {
        super(source);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}