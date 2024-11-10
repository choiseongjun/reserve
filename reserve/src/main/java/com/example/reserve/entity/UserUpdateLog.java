package com.example.reserve.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class UserUpdateLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String updatedBy;  // 업데이트한 사용자의 이름
    private String updatedAddress;
    private String updatedPhone;
    private Integer updatedAge;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public UserUpdateLog(Long userId, String updatedBy, Integer updatedAge, String updatedAddress, String updatedPhone) {
        this.userId = userId;
        this.updatedBy = updatedBy;
        this.updatedAge = updatedAge;
        this.updatedAddress = updatedAddress;
        this.updatedPhone = updatedPhone;
        this.updatedAt = LocalDateTime.now();
    }


}