package com.fu.cinema.desktop.model;

import java.math.BigDecimal;

/**
 * Record dữ liệu doanh thu của từng phim trong báo cáo (TODO 45).
 */
public record RevenueRow(
        String title,
        long tickets,
        BigDecimal revenue
) {
}

