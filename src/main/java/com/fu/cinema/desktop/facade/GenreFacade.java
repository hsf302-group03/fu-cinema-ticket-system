package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.GenreRow;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.GenreForm;
import com.fu.cinema.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GenreFacade {

    private final GenreService genreService;
    private final DesktopSession session;

    public List<GenreRow> findAll() {
        return genreService.findAll().stream()
                .map(g -> new GenreRow(g.getGenreId(), g.getGenreName(), g.getDescription()))
                .toList();
    }

    @Transactional
    public void save(GenreForm form) {
        session.requireAdmin();
        if (form.getGenreId() == null) {
            genreService.create(form);
        } else {
            genreService.update(form.getGenreId(), form);
        }
    }

    @Transactional
    public void delete(Long genreId) {
        session.requireAdmin();
        genreService.delete(genreId);
    }
}
