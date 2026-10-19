package com.fu.cinema.controller;

import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.*;
import com.fu.cinema.enums.*;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.*;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.service.ShowtimeService;
import com.fu.cinema.util.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class SeatBookingTests {
    @Autowired BookingService service;
    @Autowired ShowtimeService showtimeService;
    @Autowired BookingRepository bookings;
    @Autowired TicketRepository tickets;
    @Autowired ShowtimeRepository showtimes;
    @Autowired CustomerRepository customers;
    @Autowired MovieRepository movies;
    @Autowired RoomRepository rooms;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    Showtime showtime;
    Customer adult;
    Customer other;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        adult = customers.findAll().stream().filter(c -> c.getEmail().equals("an.nv@gmail.com")).findFirst().orElseThrow();
        other = customers.findAll().stream().filter(c -> c.getEmail().equals("binh.tt@gmail.com")).findFirst().orElseThrow();
        showtime = new Showtime();
        showtime.setMovie(movies.findAll().stream().filter(m -> m.getAgeRating() == AgeRating.P).findFirst().orElseThrow());
        showtime.setRoom(rooms.findAll().stream().filter(r -> r.getTotalRows() == 8).findFirst().orElseThrow());
        showtime.setStartTime(LocalDateTime.now().plusDays(10));
        showtime.setEndTime(showtime.getStartTime().plusHours(2));
        showtime.setTicketPrice(BigDecimal.valueOf(80000));
        showtime.setStatus(ShowtimeStatus.OPEN);
        showtime = showtimes.saveAndFlush(showtime);
    }

    private BookingRequest request(String... seats) {
        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtime.getShowtimeId());
        request.setSeatCodes(List.of(seats));
        return request;
    }

    private MockHttpSession session(Customer customer) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(1L, customer.getCustomerId(), customer.getFullName(), Role.CUSTOMER));
        return session;
    }

    @Test
    void seatMapIncludesPendingAndConfirmedButReleasesCancelledSeats() {
        Booking pending = service.create(adult.getCustomerId(), request("E5", "E6"));
        Booking confirmed = service.create(other.getCustomerId(), request("A1"));
        service.confirm(confirmed.getBookingId());
        Booking cancelled = service.create(other.getCustomerId(), request("F1"));
        service.cancelByAdmin(cancelled.getBookingId());
        var map = showtimeService.getSeatMap(showtime.getShowtimeId());
        assertThat(map.getSoldSeats()).containsExactlyInAnyOrder("E5", "E6", "A1");
        assertThat(map.getRowLabels()).containsExactly("A", "B", "C", "D", "E", "F", "G", "H");
        service.create(adult.getCustomerId(), request("F1"));
        assertThat(service.getConfirmation(adult.getCustomerId(), pending.getBookingId()).getTickets()).hasSize(2);
    }

    @Test
    void concurrentCustomersCannotBuyTheSameSeat() throws Exception {
        long before = bookings.count();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<String>> results = List.of(adult, other).stream().map(customer -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                try {
                    service.create(customer.getCustomerId(), request("B1"));
                    return "success";
                } catch (BusinessException ex) {
                    return ex.getMessage();
                }
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<String> outcomes = List.of(results.get(0).get(15, TimeUnit.SECONDS), results.get(1).get(15, TimeUnit.SECONDS));
            assertThat(outcomes).containsExactlyInAnyOrder("success", "Ghế B1 đã bán. Vui lòng chọn ghế khác.");
        }
        assertThat(bookings.count()).isEqualTo(before + 1);
        assertThat(tickets.findSoldSeatCodes(showtime.getShowtimeId())).containsExactly("B1");
    }

    @Test
    void ticketInsertFailureRollsBackBookingAndPreviouslyInsertedTicket() {
        long bookingCount = bookings.count();
        long ticketCount = tickets.count();
        // First ticket H11 inserts successfully; the database rejects the second H12.
        jdbc.execute("ALTER TABLE tickets ADD CONSTRAINT test_ticket_failure CHECK (seat_code <> 'H12')");
        try {
            assertThatThrownBy(() -> service.create(adult.getCustomerId(), request("H11", "H12")))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(bookings.count()).isEqualTo(bookingCount);
            assertThat(tickets.count()).isEqualTo(ticketCount);
            assertThat(tickets.findSoldSeatCodes(showtime.getShowtimeId())).isEmpty();
        } finally {
            jdbc.execute("ALTER TABLE tickets DROP CONSTRAINT test_ticket_failure");
        }
    }

    @Test
    void customerRoutesRequireLoginAndCustomerRole() throws Exception {
        String url = "/customer/showtimes/" + showtime.getShowtimeId() + "/seats";
        mvc.perform(get(url)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/customer/bookings")).andExpect(redirectedUrl("/login"));
        MockHttpSession admin = new MockHttpSession();
        admin.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(1L, null, "Admin", Role.ADMIN));
        mvc.perform(get(url).session(admin)).andExpect(status().isForbidden());
        mvc.perform(post("/customer/bookings").session(admin)).andExpect(status().isForbidden());
    }

    @Test
    void rendersSeatMapValidatesFormAndCreatesOwnedConfirmation() throws Exception {
        MockHttpSession session = session(adult);
        String id = showtime.getShowtimeId().toString();
        mvc.perform(get("/customer/showtimes/{id}/seats", id).session(session))
                .andExpect(status().isOk()).andExpect(view().name("customer/seats"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("seat-H12")));
        mvc.perform(post("/customer/bookings").session(session).param("showtimeId", id))
                .andExpect(redirectedUrl("/customer/showtimes/" + id + "/seats"))
                .andExpect(flash().attribute("error", "Vui lòng chọn ít nhất một ghế"));
        mvc.perform(post("/customer/bookings").session(session).param("showtimeId", "bad"))
                .andExpect(redirectedUrl("/"));
        String confirmation = mvc.perform(post("/customer/bookings").session(session)
                        .param("showtimeId", id).param("seatCodes", "C1", "C2")
                        .param("customerId", other.getCustomerId().toString()).param("totalAmount", "1"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        mvc.perform(get(confirmation).session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("160.000 đ")));
        mvc.perform(get(confirmation).session(session(other))).andExpect(status().isNotFound());
        mvc.perform(post("/customer/bookings").session(session(other)).param("showtimeId", id).param("seatCodes", "C1"))
                .andExpect(redirectedUrl("/customer/showtimes/" + id + "/seats"))
                .andExpect(flash().attribute("error", "Ghế C1 đã bán. Vui lòng chọn ghế khác."));
    }
}
