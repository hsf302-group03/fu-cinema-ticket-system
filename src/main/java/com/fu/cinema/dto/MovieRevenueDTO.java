package com.fu.cinema.dto;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class MovieRevenueDTO {
    String title;
    Long ticketCount;
    BigDecimal revenue;
}
