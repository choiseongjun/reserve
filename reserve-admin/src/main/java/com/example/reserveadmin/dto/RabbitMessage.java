package com.example.reserveadmin.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class RabbitMessage {
    private String id;
    private String fName;
    private String lName;
}