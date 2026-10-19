package com.fu.cinema.service.impl;

import com.fu.cinema.entity.Booking;
import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.entity.Customer;
import com.fu.cinema.entity.Showtime;
import com.fu.cinema.entity.Ticket;
import com.fu.cinema.enums.ShowtimeStatus;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.repository.ShowtimeRepository;
import com.fu.cinema.repository.TicketRepository;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TicketRepository ticketRepository;
    private static final Pattern SEAT_PATTERN = Pattern.compile("^[A-Z][1-9]\\d?$");

    @Override
    @Transactional
    public Booking create(Long customerId, BookingRequest request) {
        if (request == null || request.getShowtimeId() == null || request.getShowtimeId() <= 0) {
            throw new BusinessException("Vui lòng chọn suất chiếu hợp lệ");
        }
        if (request.getSeatCodes() == null || request.getSeatCodes().isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất một ghế");
        }
        if (request.getSeatCodes().stream().anyMatch(s -> s == null || s.isBlank())) {
            throw new BusinessException("Mã ghế không hợp lệ");
        }
        List<String> seats = request.getSeatCodes().stream()
                .map(s -> s.trim().toUpperCase(Locale.ROOT)).distinct().toList();
        if (seats.size() > 8) {
            throw new BusinessException("Mỗi đơn chỉ được đặt tối đa 8 ghế");
        }
        // Hold this database lock until Booking AND all Tickets have committed (BR-12).
        Showtime showtime = showtimeRepository.findByIdForUpdate(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Suất chiếu", request.getShowtimeId()));
        if (showtime.getStatus() != ShowtimeStatus.OPEN
                || !showtime.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Suất chiếu không còn mở bán");
        }
        for (String seat : seats) {
            if (!SEAT_PATTERN.matcher(seat).matches()
                    || seat.charAt(0) - 'A' >= showtime.getRoom().getTotalRows()
                    || Integer.parseInt(seat.substring(1)) > showtime.getRoom().getSeatsPerRow()) {
                throw new BusinessException("Ghế " + seat + " không tồn tại trong phòng chiếu");
            }
        }
        var sold = new HashSet<>(ticketRepository.findSoldSeatCodes(showtime.getShowtimeId()));
        List<String> taken = seats.stream().filter(sold::contains).toList();
        if (!taken.isEmpty()) {
            throw new BusinessException("Ghế " + String.join(", ", taken) + " đã bán. Vui lòng chọn ghế khác.");
        }
        if (customerId == null) {
            throw new BusinessException("Vui lòng đăng nhập bằng tài khoản khách hàng");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng", customerId));
        int age = Period.between(customer.getBirthday(), showtime.getStartTime().toLocalDate()).getYears();
        if (age < showtime.getMovie().getAgeRating().getMinAge()) {
            throw new BusinessException("Bạn chưa đủ tuổi xem phim " + showtime.getMovie().getAgeRating());
        }
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setShowtime(showtime);
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(showtime.getTicketPrice().multiply(BigDecimal.valueOf(seats.size())));
        seats.forEach(seat -> booking.addTicket(new Ticket(showtime, seat, showtime.getTicketPrice())));
        return bookingRepository.saveAndFlush(booking);
    }

    @Override
    public Booking getConfirmation(Long customerId, Long bookingId) {
        Booking booking = getDetail(bookingId);
        if (!booking.getCustomer().getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("Đơn đặt vé", bookingId);
        }
        return booking;
    }

    // ================= Admin =================

    @Override
    public Page<Booking> search(BookingStatus status, LocalDate fromDate, LocalDate toDate,
                                String keyword, int page, int size) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException("Từ ngày phải trước hoặc bằng đến ngày");
        }
        return bookingRepository.search(
                status,
                fromDate == null ? null : fromDate.atStartOfDay(),
                toDate  == null ? null : toDate.plusDays(1).atStartOfDay(),
                ValidationUtils.blankToNull(keyword),
                PageRequest.of(Math.max(page, 0), size, Sort.by("bookingDate").descending()));
    }

    @Override
    public Booking getDetail(Long bookingId) {
        return bookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn đặt vé", bookingId));
    }

    @Override
    @Transactional
    public void confirm(Long bookingId) {
        Booking booking = getDetail(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {              // BR-13
            throw new BusinessException("Chỉ xác nhận được đơn đang ở trạng thái PENDING");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public void cancelByAdmin(Long bookingId) {
        Booking booking = getDetail(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Đơn đã được huỷ trước đó");
        }
        booking.setStatus(BookingStatus.CANCELLED);
    }

    @Override
    public long countPending() {
        return bookingRepository.countByStatus(BookingStatus.PENDING);
    }
}
