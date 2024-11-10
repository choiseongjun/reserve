package com.example.reserve.service;

import com.example.reserve.entity.User;
import com.example.reserve.entity.UserInfo;
import com.example.reserve.entity.UserUpdateLog;
import com.example.reserve.repository.UserInfoRepository;
import com.example.reserve.repository.UserRepository;
import com.example.reserve.repository.UserUpdateLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceV1 {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final UserUpdateLogRepository userUpdateLogRepository;

    @Transactional
    public void updateUserAndInfo(Long userId, String newName, Integer newAge, String newAddress, String newPhone) {
        // 비관적 락을 사용하여 User와 UserInfo를 가져옴
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserInfo userInfo = userInfoRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new RuntimeException("UserInfo not found"));

        // User와 UserInfo 동시 업데이트
        user.setName(newName);
        user.setAge(newAge);
        userRepository.save(user);

        userInfo.setAddress(newAddress);
        userInfo.setPhone(newPhone);
        userInfoRepository.save(userInfo);

        UserUpdateLog log = new UserUpdateLog(userId, newName, newAge, newAddress, newPhone);
        userUpdateLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional(readOnly = false) // 읽기 전용 트랜잭션을 false로 설정
    public UserInfo getUserInfo(Long userId) {
        return userInfoRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new RuntimeException("UserInfo not found"));
    }

    @Transactional(readOnly = true)
    public UserUpdateLog getLatestUpdateLog(Long userId) {
        return userUpdateLogRepository.findTopByUserIdOrderByUpdatedAtDesc(userId)
                .orElseThrow(() -> new RuntimeException("No update log found for user"));
    }
}
