package com.fu.cinema.controller;

import com.fu.cinema.dto.ReportResult;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.*;
import com.fu.cinema.enums.*;
import com.fu.cinema.repository.*;
import com.fu.cinema.service.ReportService;
import com.fu.cinema.util.SessionUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RevenueReportTests {
    private static final LocalDate START = LocalDate.of(2001, 6, 15);
    private static final LocalDate END = START.plusDays(2);
    @Autowired ReportService service;
    @Autowired BookingRepository bookings;
    @Autowired ShowtimeRepository showtimes;
    @Autowired MovieRepository movies;
    @Autowired CustomerRepository customers;
    @Autowired RoomRepository rooms;
    @Autowired EntityManager em;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    Customer customer;
    Showtime first;
    Showtime second;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        customer = customers.findAll().getFirst();
        var allMovies = movies.findAll();
        first = showtime(allMovies.get(0));
        second = showtime(allMovies.get(1));
    }

    Showtime showtime(Movie movie) {
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(rooms.findAll().getFirst());
        // Neither the showtime date nor its current price should determine historical revenue.
        showtime.setStartTime(LocalDateTime.of(2002, 1, 1, 12, 0));
        showtime.setEndTime(showtime.getStartTime().plusHours(2));
        showtime.setTicketPrice(BigDecimal.valueOf(999999));
        showtime.setStatus(ShowtimeStatus.OPEN);
        return showtimes.save(showtime);
    }

    Booking booking(Showtime showtime, LocalDateTime date, BookingStatus status, int count, long price) {
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setShowtime(showtime);
        booking.setBookingDate(date);
        booking.setStatus(status);
        booking.setTotalAmount(BigDecimal.valueOf(price * count));
        for (int i = 1; i <= count; i++) booking.addTicket(new Ticket(showtime, "A" + i, BigDecimal.valueOf(price)));
        return bookings.saveAndFlush(booking);
    }

    MockHttpSession session(Role role) {
        var session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(1L, null, "Report test", role));
        return session;
    }

    @Test
    void aggregatesHistoricalTicketPricesAndFiltersStatusAndBothDateBoundaries() {
        Booking oldest = booking(first, START.atStartOfDay(), BookingStatus.CONFIRMED, 2, 75000);
        Booking middle = booking(second, START.plusDays(1).atTime(12, 0), BookingStatus.CONFIRMED, 1, 100000);
        // H2 persists timestamp(6); use its last representable instant before midnight.
        Booking newest = booking(showtime(first.getMovie()), END.atTime(23, 59, 59, 999999000), BookingStatus.CONFIRMED, 3, 50000);
        booking(first, START.atStartOfDay().minusNanos(1000), BookingStatus.CONFIRMED, 1, 900000);
        booking(first, END.plusDays(1).atStartOfDay(), BookingStatus.CONFIRMED, 1, 900000);
        booking(first, START.atTime(12, 0), BookingStatus.PENDING, 1, 900000);
        booking(first, START.atTime(12, 0), BookingStatus.CANCELLED, 1, 900000);
        em.clear();

        ReportResult report = service.generate(START, END);
        assertThat(report.getTotalBookings()).isEqualTo(3);
        assertThat(report.getTotalTickets()).isEqualTo(6);
        assertThat(report.getTotalRevenue()).isEqualByComparingTo("400000");
        assertThat(report.getRevenueByMovie()).hasSize(2);
        assertThat(report.getRevenueByMovie().getFirst().getTicketCount()).isEqualTo(5);
        assertThat(report.getRevenueByMovie().getFirst().getRevenue()).isEqualByComparingTo("300000");
        assertThat(report.getRevenueByMovie().getLast().getRevenue()).isEqualByComparingTo("100000");
        assertThat(report.getBookings()).extracting(Booking::getBookingId)
                .containsExactly(newest.getBookingId(), middle.getBookingId(), oldest.getBookingId());
        // Q8 fetches the customer and movie used by the reference report template.
        em.clear();
        assertThat(report.getBookings().getFirst().getCustomer().getFullName()).isNotBlank();
        assertThat(report.getBookings().getFirst().getShowtime().getMovie().getTitle()).isNotBlank();

        ReportResult singleDay = service.generate(END, END);
        assertThat(singleDay.getTotalBookings()).isEqualTo(1);
        assertThat(singleDay.getTotalRevenue()).isEqualByComparingTo("150000");
    }

    @Test
    void distinctMoviesWithTheSameTitleStaySeparate() {
        second.getMovie().setTitle(first.getMovie().getTitle());
        movies.saveAndFlush(second.getMovie());
        Booking older = booking(first, START.atTime(12, 0), BookingStatus.CONFIRMED, 1, 75000);
        Booking newer = booking(second, START.atTime(12, 0), BookingStatus.CONFIRMED, 1, 75000);
        var report = service.generate(START, START);
        assertThat(report.getRevenueByMovie()).hasSize(2);
        assertThat(report.getTotalRevenue()).isEqualByComparingTo("150000");
        assertThat(report.getBookings()).extracting(Booking::getBookingId).containsExactlyInAnyOrder(newer.getBookingId(), older.getBookingId());
    }

    @Test
    void emptyPeriodRendersZeroTotalsAndHelpfulMessage() throws Exception {
        var report = service.generate(START, END);
        assertThat(report.isEmpty()).isTrue();
        assertThat(report.getTotalBookings()).isZero();
        assertThat(report.getTotalTickets()).isZero();
        assertThat(report.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        mvc.perform(get("/admin/reports").session(session(Role.ADMIN))
                        .param("startDate", START.toString()).param("endDate", END.toString()))
                .andExpect(status().isOk()).andExpect(view().name("admin/report"))
                .andExpect(content().string(containsString("Không có dữ liệu trong khoảng thời gian đã chọn.")))
                .andExpect(content().string(not(containsString("Tổng số đơn"))));
    }

    @Test
    void rendersTablesTotalsAndLinksToExistingBookingDetails() throws Exception {
        var booking = booking(first, START.atTime(10, 30), BookingStatus.CONFIRMED, 2, 75000);
        mvc.perform(get("/admin/reports").session(session(Role.ADMIN))
                        .param("startDate", START.toString()).param("endDate", END.toString()))
                .andExpect(status().isOk()).andExpect(model().attributeDoesNotExist("error"))
                .andExpect(content().string(containsString("150,000đ")))
                .andExpect(content().string(containsString("Tổng số đơn")))
                .andExpect(content().string(containsString("/admin/bookings/" + booking.getBookingId())));
    }

    @Test
    void defaultsToCurrentMonthOnlyWhenNeitherDateWasSupplied() throws Exception {
        mvc.perform(get("/admin/reports").session(session(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("startDate", LocalDate.now().withDayOfMonth(1)))
                .andExpect(model().attribute("endDate", LocalDate.now()))
                .andExpect(model().attributeExists("report"));
    }

    @Test
    void reversedDatesShowAnErrorAndMissingDatesUseReferenceDefaults() throws Exception {
        mvc.perform(get("/admin/reports").session(session(Role.ADMIN))
                        .param("startDate", END.toString()).param("endDate", START.toString()))
                .andExpect(status().isOk()).andExpect(view().name("admin/report"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeDoesNotExist("report"))
                .andExpect(model().attribute("startDate", END));
        mvc.perform(get("/admin/reports").session(session(Role.ADMIN)).param("startDate", START.toString()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("report"))
                .andExpect(model().attribute("endDate", LocalDate.now()));
        mvc.perform(get("/admin/reports").session(session(Role.ADMIN))
                        .param("startDate", "").param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("report"))
                .andExpect(model().attribute("startDate", LocalDate.now().withDayOfMonth(1)));
        mvc.perform(get("/admin/reports").session(session(Role.ADMIN)).param("startDate", "2001-02-30"))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyAdminsCanReadRevenue() throws Exception {
        mvc.perform(get("/admin/reports")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/admin/reports").session(session(Role.CUSTOMER))).andExpect(status().isForbidden());
    }
}
