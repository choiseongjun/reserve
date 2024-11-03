package com.example.reserve.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

@Data
@Entity
@Table(name = "stock", uniqueConstraints = @UniqueConstraint(columnNames = "product_id"))
@DynamicUpdate
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", unique = true, nullable = false)
    private Long productId;

    private Integer quantity;
    @Version // 버전 관리를 위한 어노테이션
    private Long version; // 버전 필드 추가
    public boolean canDecrease(int amount) {
        return this.quantity >= amount;
    }

    public void decrease(int amount) {
        if (canDecrease(amount)) {
            this.quantity -= amount;
        } else {
            throw new IllegalStateException("재고가 부족합니다.");
        }
    }
}