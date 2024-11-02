package com.example.reserveadmin.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class StockAlarmMessage {
    private Long prodId;
    private Integer remainStock;

}