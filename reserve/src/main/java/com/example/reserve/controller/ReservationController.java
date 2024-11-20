package com.example.reserve.controller;

import com.example.reserve.dto.RabbitMessage;
import com.example.reserve.dto.ReserveRequestDto;
import com.example.reserve.entity.Reservation;
import com.example.reserve.event.RabbitPublisher;
import com.example.reserve.event.StockAlertPublisher;
import com.example.reserve.service.ReservationService;
import com.example.reserve.service.ReservationServiceV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Random;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/reservations")
@Slf4j
@RequiredArgsConstructor
public class ReservationController {
//    private final ReservationService reservationService;

    private final ReservationServiceV3 reservationServiceV3;

    private final StockAlertPublisher stockAlertPublisher;
    private final RabbitPublisher rabbitPublisher;





    @PostMapping
    public ResponseEntity<Reservation> createReservation(@RequestBody ReserveRequestDto reserveRequestDto) {


        Reservation createdReservation =
                reservationServiceV3.createReservation(reserveRequestDto.getUserId(),reserveRequestDto.getProductId(),reserveRequestDto.getQuantity());
        return ResponseEntity.ok(createdReservation);
    }
//    @PostMapping("/test")
//    public String createRabbit() {
//        stockAlertPublisher.publishStockAlert("Product ID " + "testapi" + "의 재고가 5000개 이하입니다. 현재 재고: 100" );
//        return "test";
//    }

    /**
     * Simple Queue 테스트(Exchange 활용)
     */
    @GetMapping("/send")
    public void sendMessage() {
        RabbitMessage rabbitMessage = RabbitMessage.builder().id("1").fName("First Name").lName("Last Name").build();

        IntStream.range(0, 100).forEachOrdered(n -> {
            rabbitMessage.setId(String.valueOf(n));
//            rabbitPublisher.sendMessage(rabbitMessage);
            rabbitPublisher.sendSubMessage(rabbitMessage);
            log.info("rabbitMessage {}",rabbitMessage);
        });
    }
}