package com.example.reserve.controller;

import com.example.reserve.service.ReservationServiceV3;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    private final RedisTemplate<String, String> redisTemplate;
    private final ReservationServiceV3 reservationService;

    public QueueController(RedisTemplate<String, String> redisTemplate, ReservationServiceV3 reservationService) {
        this.redisTemplate = redisTemplate;
        this.reservationService = reservationService;
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<String> getStatus(@PathVariable Long userId) {
        String status = redisTemplate.opsForValue().get("request:status:" + userId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

//    @GetMapping("/position")
//    public ResponseEntity<Integer> getQueuePosition(
//            @RequestParam Long userId,
//            @RequestParam Long productId,
//            @RequestParam int quantity) {
//        int position = reservationService.getQueuePosition(userId, productId, quantity);
//        if (position == -1) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(position);
//        }
//        return ResponseEntity.ok(position);
//    }
//    @GetMapping("/position")
//    public ResponseEntity<Integer> getQueuePosition(@RequestParam Long userId) {
//        int position = reservationService.getQueuePositionByUserId(userId);
//        if (position == -1) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(position);
//        }
//        return ResponseEntity.ok(position);
//    }
    @GetMapping("/size")
    public ResponseEntity<Long> getQueueSize() {
        Long size = redisTemplate.opsForList().size("reservation:queue");
        return ResponseEntity.ok(size);
    }

    @GetMapping("/position")
    public ResponseEntity<Integer> getQueuePosition(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam int quantity) {
        int position = reservationService.getQueuePosition(userId, productId, quantity);
        if (position == -1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(position);
        }
        return ResponseEntity.ok(position);
    }

}

