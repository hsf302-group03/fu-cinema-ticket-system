package com.fu.cinema.repository;

import com.fu.cinema.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // [D] issue #14 thêm Q4 findSoldSeatCodes(...)
}
