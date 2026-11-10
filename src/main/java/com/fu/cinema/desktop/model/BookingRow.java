package com.fu.cinema.desktop.model;

import com.fu.cinema.entity.Booking;
import com.fu.cinema.entity.Showtime;
import com.fu.cinema.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Record hiển thị từng dòng đơn hàng trong TableView quản lý đơn (TODO 44).
 */
public record BookingRow(
        Long id,
        String customerName,
        String movieTitle,
        String roomName,
        LocalDateTime startTime,
        int seatCount,
        BigDecimal total,
        BookingStatus status,
        LocalDateTime bookingDate
) {

    public static BookingRow from(Booking b) {
        Showtime s = b.getShowtime();
        return new BookingRow(
                b.getBookingId(),
                b.getCustomer() != null ? b.getCustomer().getFullName() : "—",
                s != null && s.getMovie() != null ? s.getMovie().getTitle() : "—",
                s != null && s.getRoom() != null ? s.getRoom().getRoomName() : "—",
                s != null ? s.getStartTime() : null,
                b.getTickets() != null ? b.getTickets().size() : 0,
                b.getTotalAmount(),
                b.getStatus(),
                b.getBookingDate()
        );
    }
}

