package com.fu.cinema.service;

import com.fu.cinema.entity.Booking;
import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.enums.BookingStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface BookingService {

    // ===== Admin =====
    Page<Booking> search(BookingStatus status, LocalDate fromDate, LocalDate toDate,
                         String keyword, int page, int size);

    Booking getDetail(Long bookingId);

    void confirm(Long bookingId);

    void cancelByAdmin(Long bookingId);

    long countPending();

    Booking create(Long customerId, BookingRequest request);

    Booking getConfirmation(Long customerId, Long bookingId);
    // [B] issue #15 thêm findByCustomer(...), getDetailForCustomer(...), cancelByCustomer(...)
}
