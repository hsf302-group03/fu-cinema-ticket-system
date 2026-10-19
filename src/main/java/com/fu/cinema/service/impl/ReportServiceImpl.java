package com.fu.cinema.service.impl;

import com.fu.cinema.dto.MovieRevenueDTO;
import com.fu.cinema.dto.ReportResult;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {
    private final BookingRepository bookingRepository;

    @Override
    public ReportResult generate(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException("Vui lòng chọn đầy đủ từ ngày và đến ngày.");
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("Từ ngày phải trước hoặc bằng đến ngày.");
        }
        // SQL Server dates use years 1–9999; the exclusive upper bound also needs to fit.
        if (startDate.getYear() < 1 || endDate.isAfter(LocalDate.of(9999, 12, 30))) {
            throw new BusinessException("Vui lòng chọn ngày từ 01/01/0001 đến 30/12/9999.");
        }
        var from = startDate.atStartOfDay();
        var to = endDate.plusDays(1).atStartOfDay();
        var byMovie = bookingRepository.revenueByMovie(from, to);
        var bookings = bookingRepository.findConfirmedInPeriod(from, to);
        long totalTickets = byMovie.stream().mapToLong(MovieRevenueDTO::getTicketCount).sum();
        BigDecimal totalRevenue = byMovie.stream().map(MovieRevenueDTO::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReportResult(startDate, endDate, byMovie, bookings,
                bookings.size(), totalTickets, totalRevenue);
    }
}
