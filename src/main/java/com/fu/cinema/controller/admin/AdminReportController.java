package com.fu.cinema.controller.admin;

import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {
    private final ReportService reportService;

    @GetMapping
    public String report(@RequestParam(required = false) String startDate,
                         @RequestParam(required = false) String endDate, Model model) {
        if (startDate == null && endDate == null) {
            LocalDate today = LocalDate.now();
            startDate = today.withDayOfMonth(1).toString();
            endDate = today.toString();
        }
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        try {
            LocalDate start = startDate == null || startDate.isBlank() ? null : LocalDate.parse(startDate);
            LocalDate end = endDate == null || endDate.isBlank() ? null : LocalDate.parse(endDate);
            model.addAttribute("report", reportService.generate(start, end));
        } catch (DateTimeParseException ex) {
            model.addAttribute("error", "Ngày không hợp lệ. Vui lòng chọn lại từ ngày và đến ngày.");
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "admin/report";
    }
}
