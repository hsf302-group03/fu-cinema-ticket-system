package com.fu.cinema.controller.customer;

import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.service.ShowtimeService;
import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerBookingController {
    private final ShowtimeService showtimeService;
    private final BookingService bookingService;

    @GetMapping("/showtimes/{id}/seats")
    public String seats(@PathVariable Long id, Model model) {
        model.addAttribute("seatMap", showtimeService.getSeatMap(id));
        return "customer/seats";
    }

    @PostMapping("/bookings")
    public String create(@Valid @ModelAttribute BookingRequest request, BindingResult errors,
                         HttpSession session, RedirectAttributes ra) {
        String back = request.getShowtimeId() == null || request.getShowtimeId() <= 0
                ? "redirect:/" : "redirect:/customer/showtimes/" + request.getShowtimeId() + "/seats";
        if (errors.hasErrors()) {
            ra.addFlashAttribute("error", errors.getAllErrors().get(0).getDefaultMessage());
            return back;
        }
        try {
            Booking booking = bookingService.create(SessionUtils.getCurrentCustomerId(session), request);
            return "redirect:/customer/bookings/" + booking.getBookingId() + "/confirmation";
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return back;
        }
    }

    @GetMapping("/bookings/{id}/confirmation")
    public String confirmation(@PathVariable Long id, HttpSession session, Model model) {
        model.addAttribute("booking", bookingService.getConfirmation(SessionUtils.getCurrentCustomerId(session), id));
        return "customer/booking-confirmation";
    }
}
