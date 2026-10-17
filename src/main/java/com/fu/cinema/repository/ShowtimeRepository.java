package com.fu.cinema.repository;

import com.fu.cinema.entity.Showtime;
import com.fu.cinema.enums.ShowtimeStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    // ===== Derived query (mục 9.1) =====
    boolean existsByMovie_MovieId(Long movieId);

    boolean existsByRoom_RoomId(Long roomId);

    @EntityGraph(attributePaths = "room")               // nạp luôn Room → tránh LazyInitializationException ở view
    List<Showtime> findByMovie_MovieIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
            Long movieId, ShowtimeStatus status, LocalDateTime after);

    // [D] issue #11 thêm search(...), findDetailById(...), Q3 findOverlapping(...), Q11 countTodayShowtimesByRoom()
    // [D] issue #14 thêm findByIdForUpdate(...)
}
