package com.fu.cinema.controller;

import com.fu.cinema.dto.LoginForm;
import com.fu.cinema.dto.RegisterForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("form", new LoginForm());
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("form") LoginForm form, BindingResult br,
                        HttpServletRequest request, Model model) {
        if (br.hasErrors()) {
            return "auth/login";
        }
        try {
            SessionUser user = authService.login(form.getUsername(), form.getPassword());
            startSession(request, user);
            return user.isAdmin() ? "redirect:/admin" : "redirect:/";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult br,
                           HttpServletRequest request, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "auth/register";                                   // giữ lại dữ liệu đã nhập
        }
        try {
            startSession(request, authService.register(form));
            ra.addFlashAttribute("success", "Đăng ký thành công. Chào mừng bạn đến FU Cinema!");
            return "redirect:/";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        session.invalidate();
        ra.addFlashAttribute("success", "Đã đăng xuất");
        return "redirect:/login";
    }

    private void startSession(HttpServletRequest request, SessionUser user) {
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();                                         // chống session fixation
        }
        request.getSession(true).setAttribute(SessionUtils.CURRENT_USER, user);
    }
}
