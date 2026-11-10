package com.fu.cinema.desktop.model;

public record GenreItem(Long id, String name) {
    @Override
    public String toString() {
        return name;
    }
}
