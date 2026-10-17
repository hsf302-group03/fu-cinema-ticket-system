package com.fu.cinema.repository;

import com.fu.cinema.entity.Booking;
import com.fu.cinema.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ===== Derived query (mục 9.1) =====
    boolean existsByCustomer_CustomerId(Long customerId);

    boolean existsByShowtime_ShowtimeId(Long showtimeId);

    long countByStatus(BookingStatus status);

    @EntityGraph(attributePaths = {"showtime", "showtime.movie", "showtime.room"})
    Page<Booking> findByCustomer_CustomerId(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"showtime", "showtime.movie", "showtime.room"})
    Page<Booking> findByCustomer_CustomerIdAndStatus(Long customerId, BookingStatus status, Pageable pageable);

    // ===== Custom query: mỗi người thêm method của mình ở CUỐI file, mỗi method một commit =====
    // [B] #12: Q5 findDetailById, Q6 search   · [D] #11: Q9 cancelAllByShowtime
    // [D] #17: Q7 revenueByMovie, Q8 findConfirmedInPeriod   · [C] #18: Q12 countWatched
}
