package com.fu.cinema.desktop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Record đại diện cho suất chiếu trong ComboBox chọn suất chiếu (TODO 43).
 */
public record ShowtimeItem(Long id, LocalDateTime start, String roomName, BigDecimal price) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm EEE dd/MM", Locale.of("vi", "VN"));

    @Override
    public String toString() {
        return start.format(FORMATTER) + " · " + roomName;
    }
}

