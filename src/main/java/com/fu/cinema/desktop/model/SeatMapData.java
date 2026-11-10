package com.fu.cinema.desktop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Record dữ liệu sơ đồ ghế cho Desktop (TODO 42).
 * UI chỉ làm việc với record (TECH-14).
 */
public record SeatMapData(
        Long showtimeId,
        String movieTitle,
        String roomName,
        LocalDateTime startTime,
        BigDecimal price,
        List<String> rowLabels,
        int seatsPerRow,
        Set<String> soldSeats
) {
}

