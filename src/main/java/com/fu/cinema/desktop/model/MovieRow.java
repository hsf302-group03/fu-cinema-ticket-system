package com.fu.cinema.desktop.model;

import com.fu.cinema.entity.Movie;
import com.fu.cinema.enums.AgeRating;
import com.fu.cinema.enums.MovieStatus;

import java.time.LocalDate;

public record MovieRow(Long id, String title, String genreName, int duration,
                       LocalDate releaseDate, AgeRating ageRating, MovieStatus status) {
    public static MovieRow from(Movie m) {
        return new MovieRow(m.getMovieId(), m.getTitle(), m.getGenre().getGenreName(),
                m.getDuration(), m.getReleaseDate(), m.getAgeRating(), m.getStatus());
    }
}
