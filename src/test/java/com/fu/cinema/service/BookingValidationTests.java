package com.fu.cinema.service;

import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.entity.*;
import com.fu.cinema.enums.*;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.*;
import com.fu.cinema.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingValidationTests {
    @Mock BookingRepository bookings;
    @Mock CustomerRepository customers;
    @Mock ShowtimeRepository showtimes;
    @Mock TicketRepository tickets;
    BookingServiceImpl service;
    Showtime showtime;
    Customer customer;

    @BeforeEach
    void setup() {
        service = new BookingServiceImpl(bookings, customers, showtimes, tickets);
        Room room = new Room("Test", RoomType.TWO_D, 8, 12, RoomStatus.ACTIVE);
        Movie movie = new Movie();
        movie.setAgeRating(AgeRating.T18);
        showtime = new Showtime();
        showtime.setShowtimeId(1L);
        showtime.setRoom(room);
        showtime.setMovie(movie);
        showtime.setStatus(ShowtimeStatus.OPEN);
        showtime.setStartTime(LocalDateTime.now().plusDays(1));
        showtime.setTicketPrice(BigDecimal.valueOf(80000));
        customer = new Customer();
        customer.setCustomerId(2L);
        customer.setBirthday(LocalDate.now().minusYears(20));
    }

    private BookingRequest request(List<String> seats) {
        BookingRequest request = new BookingRequest();
        request.setShowtimeId(1L);
        request.setSeatCodes(seats);
        return request;
    }

    private void available() {
        when(showtimes.findByIdForUpdate(1L)).thenReturn(Optional.of(showtime));
    }

    @Test
    void rejectsMissingRequestShowtimeAndSeats() {
        assertThatThrownBy(() -> service.create(2L, null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(2L, new BookingRequest())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(2L, request(null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(2L, request(List.of()))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(2L, request(Collections.singletonList(null)))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(bookings, showtimes);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "A0", "A01", "A-1", "AA1", "A13", "I1", "Z99", "A100", "<script>"})
    void rejectsMalformedOrOutOfRoomSeats(String seat) {
        if (seat != null && !seat.isBlank()) available();
        assertThatThrownBy(() -> service.create(2L, request(Collections.singletonList(seat))))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(bookings);
    }

    @Test
    void rejectsMoreThanEightSeats() {
        assertThatThrownBy(() -> service.create(2L, request(List.of("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9"))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("8 ghế");
        verifyNoInteractions(bookings, showtimes);
    }

    @Test
    void rejectsClosedCancelledAndStartedShowtimes() {
        available();
        for (ShowtimeStatus status : ShowtimeStatus.values()) {
            if (status == ShowtimeStatus.OPEN) continue;
            showtime.setStatus(status);
            assertThatThrownBy(() -> service.create(2L, request(List.of("A1"))))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("không còn mở bán");
        }
        showtime.setStatus(ShowtimeStatus.OPEN);
        showtime.setStartTime(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> service.create(2L, request(List.of("A1"))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("không còn mở bán");
        verifyNoInteractions(bookings, tickets);
    }

    @Test
    void rejectsAnySoldSeatWithoutSavingOtherSeats() {
        available();
        when(tickets.findSoldSeatCodes(1L)).thenReturn(List.of("A2"));
        assertThatThrownBy(() -> service.create(2L, request(List.of("A1", "A2"))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("A2 đã bán");
        verifyNoInteractions(bookings);
    }

    @Test
    void enforcesAgeOnShowDateIncludingExactBirthday() {
        available();
        when(customers.findById(2L)).thenReturn(Optional.of(customer));
        LocalDate date = showtime.getStartTime().toLocalDate();
        customer.setBirthday(date.minusYears(18).plusDays(1));
        assertThatThrownBy(() -> service.create(2L, request(List.of("A1"))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("chưa đủ tuổi");
        verifyNoInteractions(bookings);
        customer.setBirthday(date.minusYears(18));
        when(bookings.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.create(2L, request(List.of("A1"))).getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void normalizesDeduplicatesAndCalculatesServerPrice() {
        available();
        when(customers.findById(2L)).thenReturn(Optional.of(customer));
        when(bookings.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Booking booking = service.create(2L, request(List.of(" a1 ", "A1", "h12")));
        assertThat(booking.getTotalAmount()).isEqualByComparingTo("160000");
        assertThat(booking.getTickets()).extracting(Ticket::getSeatCode).containsExactly("A1", "H12");
        assertThat(booking.getTickets()).allSatisfy(ticket -> {
            assertThat(ticket.getBooking()).isSameAs(booking);
            assertThat(ticket.getPrice()).isEqualByComparingTo("80000");
        });
    }
}
