package com.fu.cinema.repository;

import com.fu.cinema.entity.Booking;
import com.fu.cinema.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // ===== Q9: huỷ hàng loạt đơn của một suất chiếu (BR-09) =====
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Booking b SET b.status = com.fu.cinema.enums.BookingStatus.CANCELLED
            WHERE b.showtime.showtimeId = :showtimeId
              AND b.status <> com.fu.cinema.enums.BookingStatus.CANCELLED
            """)
    int cancelAllByShowtime(@Param("showtimeId") Long showtimeId);

    // ===== Q5: chi tiết đơn kèm khách, suất chiếu, phim, phòng, vé =====
    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.customer c
            JOIN FETCH b.showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.room
            LEFT JOIN FETCH b.tickets
            WHERE b.bookingId = :id
            """)
    java.util.Optional<Booking> findDetailById(@Param("id") Long id);

    // ===== Q6: lọc đơn cho Admin (trạng thái, khoảng ngày, từ khoá khách hàng) =====
    @Query(value = """
            SELECT b FROM Booking b
            JOIN FETCH b.customer c
            JOIN FETCH b.showtime s
            JOIN FETCH s.movie m
            WHERE (:status IS NULL OR b.status = :status)
              AND (:from IS NULL OR b.bookingDate >= :from)
              AND (:to IS NULL OR b.bookingDate < :to)
              AND (:kw IS NULL
                   OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :kw, '%'))
                   OR LOWER(c.email)    LIKE LOWER(CONCAT('%', :kw, '%'))
                   OR c.mobile          LIKE CONCAT('%', :kw, '%'))
            """,
            countQuery = """
            SELECT COUNT(b) FROM Booking b JOIN b.customer c
            WHERE (:status IS NULL OR b.status = :status)
              AND (:from IS NULL OR b.bookingDate >= :from)
              AND (:to IS NULL OR b.bookingDate < :to)
              AND (:kw IS NULL
                   OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :kw, '%'))
                   OR LOWER(c.email)    LIKE LOWER(CONCAT('%', :kw, '%'))
                   OR c.mobile          LIKE CONCAT('%', :kw, '%'))
            """)
    Page<Booking> search(@Param("status") BookingStatus status,
                         @Param("from") java.time.LocalDateTime from,
                         @Param("to") java.time.LocalDateTime to,
                         @Param("kw") String keyword,
                         Pageable pageable);
}
