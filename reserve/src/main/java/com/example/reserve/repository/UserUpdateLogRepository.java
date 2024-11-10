package com.example.reserve.repository;

import com.example.reserve.entity.UserUpdateLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserUpdateLogRepository extends JpaRepository<UserUpdateLog, Long> {
    Optional<UserUpdateLog> findTopByUserIdOrderByUpdatedAtDesc(Long userId);
}
