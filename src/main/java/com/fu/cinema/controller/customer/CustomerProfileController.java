package com.fu.cinema.controller.customer;

import com.fu.cinema.dto.ChangePasswordForm;
import com.fu.cinema.dto.ProfileForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerService customerService;

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        model.addAttribute("form",
                customerService.getProfile(SessionUtils.getCurrentCustomerId(session)));
        return "customer/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("form") ProfileForm form,
                                BindingResult br,
                                HttpSession session, Model model, RedirectAttributes ra) {
        SessionUser user = SessionUtils.getCurrentUser(session);
        // Giữ lại username (không cho sửa)
        form.setUsername(customerService.getProfile(user.getCustomerId()).getUsername());
        if (br.hasErrors()) {
            return "customer/profile";
        }
        try {
            customerService.updateProfile(user.getCustomerId(), form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "customer/profile";
        }
        // Cập nhật tên hiển thị trên navbar sau khi đổi họ tên
        session.setAttribute(SessionUtils.CURRENT_USER,
                new SessionUser(user.getAccountId(), user.getCustomerId(),
                        form.getFullName().trim(), user.getRole()));
        ra.addFlashAttribute("success", "Đã cập nhật hồ sơ");
        return "redirect:/customer/profile";
    }

    @GetMapping("/password")
    public String passwordForm(Model model) {
        model.addAttribute("form", new ChangePasswordForm());
        return "customer/password";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("form") ChangePasswordForm form,
                                 BindingResult br,
                                 HttpSession session, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "customer/password";
        }
        try {
            customerService.changePassword(
                    SessionUtils.getCurrentUser(session).getAccountId(), form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "customer/password";
        }
        ra.addFlashAttribute("success", "Đã đổi mật khẩu thành công");
        return "redirect:/customer/profile";
    }
}
