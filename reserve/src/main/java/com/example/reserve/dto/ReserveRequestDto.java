package com.example.reserve.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReserveRequestDto {

    private Long productId;
    private Long userId;
    private int quantity;
}
