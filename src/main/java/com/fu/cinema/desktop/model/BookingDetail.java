package com.fu.cinema.desktop.model;

import com.fu.cinema.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Record hiển thị chi tiết đầy đủ của đơn đặt vé khi double-click hoặc bấm Xem chi tiết (TODO 44).
 */
public record BookingDetail(
        Long id,
        String customerName,
        String mobile,
        String movieTitle,
        String roomName,
        LocalDateTime startTime,
        List<String> seats,
        BigDecimal total,
        BookingStatus status,
        LocalDateTime bookingDate
) {
}

