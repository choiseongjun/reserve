package com.example.reserve;

import com.example.reserve.entity.User;
import com.example.reserve.entity.UserInfo;
import com.example.reserve.entity.UserUpdateLog;
import com.example.reserve.repository.UserInfoRepository;
import com.example.reserve.repository.UserRepository;
import com.example.reserve.service.UserServiceV1;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserServiceTestV1 {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationServiceTest.class);
    @Autowired
    private UserServiceV1 userService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserInfoRepository userInfoRepository;
    @Test
    @DisplayName("다수의 유저가 동시에 유저정보를 접근했을때 최종변경 값을 불러온다")
    void testConcurrentUpdate() throws InterruptedException {
        // 초기 데이터 생성
        User user = new User();
        user.setName("성준");
        user.setAge(30);
        userRepository.save(user);

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setAddress("서울");
        userInfo.setPhone("010-1234-5678");
        userInfoRepository.save(userInfo);

        // 첫 번째 사용자가 업데이트
        Thread t1 = new Thread(() -> {
            userService.updateUserAndInfo(user.getId(), "User1", 31, "부산", "010-1111-1111");
        });

        // 두 번째 사용자가 업데이트
        Thread t2 = new Thread(() -> {
            userService.updateUserAndInfo(user.getId(), "User2", 32, "대구", "010-2222-2222");
        });

        // 세 번째 사용자가 조회
        Thread t3 = new Thread(() -> {
            User latestUser = userService.getUser(user.getId());
            UserInfo latestInfo = userService.getUserInfo(user.getId());
            System.out.println("최신 유저: " + latestUser.getName() + ", 주소: " + latestInfo.getAddress());
        });

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        User updatedUser = userService.getUser(user.getId());
        UserInfo updatedInfo = userService.getUserInfo(user.getId());

        assertEquals("User2", updatedUser.getName());
        assertEquals("대구", updatedInfo.getAddress());;

    }
    @Test
    @DisplayName("다수의 유저가 동시에 유저 정보를 접근했을 때 최종 변경 값을 불러온다 - 1000명 동시 접근")
    void testConcurrentUpdateWithMultipleUsers() throws InterruptedException {
        User user = new User();
        user.setName("성준");
        user.setAge(30);
        userRepository.save(user);

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setAddress("서울");
        userInfo.setPhone("010-1234-5678");
        userInfoRepository.save(userInfo);

        int threadCount = 1000;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                String newName = "User" + index;
                String newAddress = "도시" + index;
                String newPhone = "010-" + String.format("%04d-%04d", index, index);
                userService.updateUserAndInfo(user.getId(), newName, 30 + index, newAddress, newPhone);
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        UserUpdateLog latestLog = userService.getLatestUpdateLog(user.getId());
        LOGGER.info("최종 업데이트 로그 - 유저 이름: {}, 나이: {}, 주소: {}, 전화번호: {}, 수정 시간: {}",
                latestLog.getUpdatedBy(), latestLog.getUpdatedAge(),
                latestLog.getUpdatedAddress(), latestLog.getUpdatedPhone(),
                latestLog.getUpdatedAt());
    }
}
