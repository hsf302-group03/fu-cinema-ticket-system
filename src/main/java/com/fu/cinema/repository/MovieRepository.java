package com.fu.cinema.repository;

import com.fu.cinema.entity.Movie;
import com.fu.cinema.enums.MovieStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    // ===== Derived query (mục 9.1) =====
    boolean existsByGenre_GenreId(Long genreId);

    long countByStatus(MovieStatus status);

    List<Movie> findByStatusInOrderByReleaseDateDesc(Collection<MovieStatus> statuses);

    // [C] issue #9 thêm Q2 search(...), findWithGenreById(...), findByStatusNotOrderByTitleAsc(...) ở dưới đây
}
