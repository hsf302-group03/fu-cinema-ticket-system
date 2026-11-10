package com.fu.cinema.desktop.model;

import com.fu.cinema.enums.AgeRating;

/**
 * Record đại diện cho phim trong ComboBox chọn phim (TODO 41 & 43).
 */
public record MovieItem(Long id, String title, int duration, AgeRating ageRating) {

    @Override
    public String toString() {
        return title + " (" + duration + "')";
    }
}

