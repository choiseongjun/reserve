package com.example.reserve.repository;

import com.example.reserve.entity.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT s FROM Stock s WHERE s.productId = :productId")
    Optional<Stock> findByProductIdWithLock(@Param("productId") Long productId);

    Optional<Stock> findByProductId(Long productId);

    @Query("select s from Stock s where s.productId = :productId")
    Optional<Stock> findByProductIdWithPessimisticLock(@Param("productId") Long productId);
}