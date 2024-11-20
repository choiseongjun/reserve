package com.example.reserve.event;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReservationMessage {
    private Long userId;
    private Long productId;
    private int quantity;
    private String requestId;
}
