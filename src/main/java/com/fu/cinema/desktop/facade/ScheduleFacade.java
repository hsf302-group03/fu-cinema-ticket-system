package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.*;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.RoomForm;
import com.fu.cinema.dto.ShowtimeForm;
import com.fu.cinema.service.MovieService;
import com.fu.cinema.service.RoomService;
import com.fu.cinema.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleFacade {

    private final RoomService roomService;
    private final ShowtimeService showtimeService;
    private final MovieService movieService;
    private final DesktopSession session;

    // ===== Phòng chiếu =====
    public List<RoomRow> findAllRooms() {
        return roomService.findAll().stream()
                .map(r -> new RoomRow(r.getRoomId(), r.getRoomName(), r.getRoomType(),
                        r.getTotalRows(), r.getSeatsPerRow(), r.getStatus()))
                .toList();
    }

    public RoomForm getRoomForm(Long id) {
        return roomService.getForm(id);
    }

    @Transactional
    public void saveRoom(RoomForm form) {
        session.requireAdmin();
        if (form.getRoomId() == null) {
            roomService.create(form);
        } else {
            roomService.update(form.getRoomId(), form);
        }
    }

    @Transactional
    public boolean deleteRoom(Long id) {
        session.requireAdmin();
        return roomService.delete(id);
    }

    // ===== Suất chiếu =====
    public PageResult<ShowtimeRow> searchShowtimes(Long movieId, Long roomId, LocalDate date, int page) {
        return PageResult.of(showtimeService.search(movieId, roomId, date, page, 10), s -> new ShowtimeRow(
                s.getShowtimeId(), s.getMovie().getTitle(), s.getRoom().getRoomName(),
                s.getStartTime(), s.getEndTime(), s.getTicketPrice(), s.getStatus()));
    }

    public List<MovieItem> schedulableMovies() {
        return movieService.findSchedulable().stream()
                .map(m -> new MovieItem(m.getMovieId(), m.getTitle(), m.getDuration(), m.getAgeRating()))
                .toList();
    }

    public List<RoomItem> activeRooms() {
        return roomService.findActive().stream()
                .map(r -> new RoomItem(r.getRoomId(), r.getRoomName()))
                .toList();
    }

    public ShowtimeForm getShowtimeForm(Long id) {
        return showtimeService.getForm(id);
    }

    @Transactional
    public void saveShowtime(ShowtimeForm form) {
        session.requireAdmin();
        if (form.getShowtimeId() == null) {
            showtimeService.create(form);
        } else {
            showtimeService.update(form.getShowtimeId(), form);
        }
    }

    @Transactional
    public boolean deleteShowtime(Long id) {
        session.requireAdmin();
        return showtimeService.delete(id);
    }
}
