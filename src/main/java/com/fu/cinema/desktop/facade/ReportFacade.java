package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.model.ReportData;
import com.fu.cinema.desktop.model.RevenueRow;
import com.fu.cinema.dto.ReportResult;
import com.fu.cinema.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Facade xử lý nghiệp vụ báo cáo doanh thu trên Desktop (TODO 45).
 * Gọi ReportService, map Entity sang Record bên trong Transaction (TECH-14).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportFacade {

    private final ReportService reportService;

    /**
     * Tạo dữ liệu báo cáo doanh thu theo khoảng ngày (BR-15).
     * Nếu start > end, ReportService sẽ ném BusinessException.
     */
    public ReportData generate(LocalDate start, LocalDate end) {
        ReportResult r = reportService.generate(start, end);

        List<RevenueRow> revenueRows = r.getRevenueByMovie() != null
                ? r.getRevenueByMovie().stream()
                .map(m -> new RevenueRow(m.getTitle(), m.getTicketCount(), m.getRevenue()))
                .toList()
                : List.of();

        List<ReportBookingRow> bookingRows = r.getBookings() != null
                ? r.getBookings().stream()
                .map(b -> new ReportBookingRow(
                        b.getBookingId(),
                        b.getBookingDate(),
                        b.getCustomer() != null ? b.getCustomer().getFullName() : "—",
                        b.getShowtime() != null && b.getShowtime().getMovie() != null ? b.getShowtime().getMovie().getTitle() : "—",
                        b.getTotalAmount()
                ))
                .toList()
                : List.of();

        return new ReportData(
                start,
                end,
                revenueRows,
                bookingRows,
                r.getTotalBookings(),
                r.getTotalTickets(),
                r.getTotalRevenue()
        );
    }
}

