package com.example.reserve.controller;

import com.example.reserve.dto.DtoGenerator;
import com.example.reserve.entity.User;
import com.example.reserve.entity.UserInfo;
import com.example.reserve.service.UserServiceV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.example.reserve.dto.DtoGenerator.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserServiceV1 userService;
    private final DtoGenerator dtoGenerator;

    @GetMapping("/test/dtogen")
    public ResponseEntity<?> dtoGen() {
        try {
            // 테스트용 가상 데이터 생성
            HashMap<String, Object> resultMap = new HashMap<>();
            resultMap.put("userId", 1L);
            resultMap.put("name", "John Doe");
            resultMap.put("nickname", "johnd");
            resultMap.put("age", 25);
            resultMap.put("email", "john@example.com");
            resultMap.put("isActive", true);
            resultMap.put("score", 95.5);
            resultMap.put("createdAt", LocalDateTime.now());
            resultMap.put("point", 1000);

            String className = "UserDto";

            // DTO 생성
            DtoGenerator.DtoTemplate template = dtoGenerator.analyzeHashMap(className, resultMap);
            dtoGenerator.generateDtoClass(template);
            dtoGenerator.generateDtoDocumentation(template);

            // 생성된 파일 경로 반환
            Map<String, String> filePaths = dtoGenerator.getGeneratedFilePaths(className);

            return ResponseEntity.ok(Map.of(
                    "message", "DTO generation completed successfully",
                    "generatedFiles", filePaths,
                    "generatedFields", resultMap.keySet()
            ));

        } catch (IOException e) {
            log.error("Failed to generate DTO", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to generate DTO",
                            "message", e.getMessage()
                    ));
        }
    }
    @PostMapping("/{id}")
    public ResponseEntity<String> updateUser(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam Integer age,
            @RequestParam String address,
            @RequestParam String phone) {
        userService.updateUserAndInfo(id, name, age, address, phone);
        return ResponseEntity.ok("User and UserInfo updated");
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @GetMapping("/info/{userId}")
    public ResponseEntity<UserInfo> getUserInfo(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

}