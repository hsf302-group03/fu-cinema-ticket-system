package com.fu.cinema.dto;

import com.fu.cinema.entity.Showtime;
import com.fu.cinema.enums.ShowtimeStatus;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Value
public class SeatMap {
    Showtime showtime;
    List<String> rowLabels;
    int seatsPerRow;
    Set<String> soldSeats;

    public boolean isSold(String seatCode) {
        return soldSeats.contains(seatCode);
    }

    public boolean isBookable() {
        return showtime.getStatus() == ShowtimeStatus.OPEN
                && showtime.getStartTime().isAfter(LocalDateTime.now());
    }
}
