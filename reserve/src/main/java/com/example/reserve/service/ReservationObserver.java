package com.example.reserve.service;

import com.example.reserve.entity.Reservation;

public interface ReservationObserver {
    void onReservationCreated(Reservation reservation);
}