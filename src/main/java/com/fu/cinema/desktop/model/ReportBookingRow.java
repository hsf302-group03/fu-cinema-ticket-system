package com.fu.cinema.desktop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Record dữ liệu từng đơn hàng CONFIRMED trong bảng chi tiết báo cáo (TODO 45).
 */
public record ReportBookingRow(
        Long id,
        LocalDateTime bookingDate,
        String customerName,
        String movieTitle,
        BigDecimal total
) {
}

