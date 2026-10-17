package com.fu.cinema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Bản TẠM của Leader để kiểm tra layout (TODO 3). C sẽ thay toàn bộ file này ở issue #13 (TODO 27).
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
