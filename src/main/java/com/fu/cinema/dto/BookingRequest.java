package com.fu.cinema.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BookingRequest {
    @NotNull(message = "Vui lòng chọn suất chiếu")
    @Positive(message = "Suất chiếu không hợp lệ")
    private Long showtimeId;

    @NotEmpty(message = "Vui lòng chọn ít nhất một ghế")
    @Size(max = 8, message = "Mỗi đơn chỉ được đặt tối đa 8 ghế")
    private List<String> seatCodes = new ArrayList<>();
}
