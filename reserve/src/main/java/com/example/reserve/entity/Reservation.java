package com.example.reserve.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reservation")
@Builder
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "product_id")
    private Long productId;

    private Integer quantity;

    private LocalDateTime reservationTime;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;
    @Version
    private Long version;
    public enum ReservationStatus {
        PENDING, CONFIRMED, CANCELLED
    }
}