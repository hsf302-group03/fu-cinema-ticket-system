package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.BookingDetail;
import com.fu.cinema.desktop.model.BookingRow;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.entity.Showtime;
import com.fu.cinema.entity.Ticket;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Facade quản lý đơn đặt vé trên Desktop (TODO 44).
 * Kết nối BookingService, ánh xạ Entity -> Record trong Transaction tránh LazyInitializationException.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookingFacade {

    public static final int PAGE_SIZE = 15;

    private final BookingService bookingService;
    private final DesktopSession session;

    public PageResult<BookingRow> search(BookingStatus status, LocalDate from, LocalDate to, String keyword, int page) {
        return PageResult.of(
                bookingService.search(status, from, to, keyword, page, PAGE_SIZE),
                BookingRow::from
        );
    }

    public BookingDetail detail(Long id) {
        Booking b = bookingService.getDetail(id); // Q5 JOIN FETCH customer, showtime, movie, room, tickets
        Showtime s = b.getShowtime();
        List<String> seats = b.getTickets() != null
                ? b.getTickets().stream().map(Ticket::getSeatCode).sorted().toList()
                : List.of();

        return new BookingDetail(
                b.getBookingId(),
                b.getCustomer() != null ? b.getCustomer().getFullName() : "—",
                b.getCustomer() != null ? b.getCustomer().getMobile() : "—",
                s != null && s.getMovie() != null ? s.getMovie().getTitle() : "—",
                s != null && s.getRoom() != null ? s.getRoom().getRoomName() : "—",
                s != null ? s.getStartTime() : null,
                seats,
                b.getTotalAmount(),
                b.getStatus(),
                b.getBookingDate()
        );
    }

    /** Xác nhận đơn PENDING -> CONFIRMED (BR-13) */
    @Transactional
    public void confirm(Long id) {
        session.requireAdmin();
        bookingService.confirm(id);
    }

    /** Hủy đơn PENDING/CONFIRMED -> CANCELLED (BR-13) */
    @Transactional
    public void cancel(Long id) {
        session.requireAdmin();
        bookingService.cancelByAdmin(id);
    }
}

