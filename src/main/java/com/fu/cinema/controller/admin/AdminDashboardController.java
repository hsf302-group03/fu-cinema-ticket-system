package com.fu.cinema.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Bản TẠM để Admin đăng nhập xong có trang đích. Hoàn thiện số liệu ở issue #20 (Sprint 3).
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @GetMapping
    public String dashboard() {
        return "admin/dashboard";
    }
}
