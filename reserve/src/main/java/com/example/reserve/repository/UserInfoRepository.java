package com.example.reserve.repository;

import com.example.reserve.entity.UserInfo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserInfo u WHERE u.userId = :userId")
    Optional<UserInfo> findByUserIdWithLock(@Param("userId") Long userId);
}