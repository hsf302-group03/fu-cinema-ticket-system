package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.GenreItem;
import com.fu.cinema.desktop.model.MovieRow;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.enums.MovieStatus;
import com.fu.cinema.service.GenreService;
import com.fu.cinema.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MovieFacade {

    public static final int PAGE_SIZE = 10;

    private final MovieService movieService;
    private final GenreService genreService;
    private final DesktopSession session;

    public PageResult<MovieRow> search(String keyword, Long genreId, MovieStatus status, String sort, int page) {
        return PageResult.of(movieService.searchForAdmin(keyword, genreId, status, page, PAGE_SIZE, sort),
                MovieRow::from);
    }

    public List<GenreItem> genres() {
        return genreService.findAll().stream()
                .map(g -> new GenreItem(g.getGenreId(), g.getGenreName()))
                .toList();
    }

    public MovieForm getForm(Long movieId) {
        return movieService.getForm(movieId);
    }

    @Transactional
    public void save(MovieForm form) {
        session.requireAdmin();
        if (form.getMovieId() == null) {
            movieService.create(form);
        } else {
            movieService.update(form.getMovieId(), form);
        }
    }

    @Transactional
    public boolean delete(Long movieId) {
        session.requireAdmin();
        return movieService.delete(movieId);
    }
}
