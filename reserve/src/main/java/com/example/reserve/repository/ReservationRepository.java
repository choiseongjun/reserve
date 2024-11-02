package com.example.reserve.repository;

import com.example.reserve.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByProductIdAndUserId(Long productId, Long userId); // 특정 productId와 userId로 예약 검색

}