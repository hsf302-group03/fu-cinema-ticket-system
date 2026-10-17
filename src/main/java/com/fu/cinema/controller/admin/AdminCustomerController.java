package com.fu.cinema.controller.admin;

import com.fu.cinema.dto.CustomerForm;
import com.fu.cinema.enums.AccountStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.util.PageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private static final String FORM_VIEW = "admin/customers/form";

    private final CustomerService customerService;

    @GetMapping
    public String list(@RequestParam(required = false) String kw,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("customers", customerService.search(kw, page, PageUtils.DEFAULT_SIZE));
        model.addAttribute("kw", kw);
        model.addAttribute("pageUrl", PageUtils.currentUrlWithoutPage());
        return "admin/customers/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new CustomerForm());
        model.addAttribute("statuses", AccountStatus.values());
        return FORM_VIEW;
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") CustomerForm form, BindingResult br,
                         Model model, RedirectAttributes ra) {
        model.addAttribute("statuses", AccountStatus.values());
        if (br.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            customerService.create(form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return FORM_VIEW;
        }
        ra.addFlashAttribute("success", "Đã thêm khách hàng " + form.getFullName());
        return "redirect:/admin/customers";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", customerService.getForm(id));
        model.addAttribute("statuses", AccountStatus.values());
        return FORM_VIEW;
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") CustomerForm form, BindingResult br,
                         Model model, RedirectAttributes ra) {
        form.setCustomerId(id);
        model.addAttribute("statuses", AccountStatus.values());
        if (br.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            customerService.update(id, form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return FORM_VIEW;
        }
        ra.addFlashAttribute("success", "Đã cập nhật khách hàng " + form.getFullName());
        return "redirect:/admin/customers";
    }

    @PostMapping("/{id}/lock")
    public String toggleLock(@PathVariable Long id, RedirectAttributes ra) {
        customerService.toggleLock(id);
        ra.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản");
        return "redirect:/admin/customers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        boolean deleted = customerService.delete(id);
        ra.addFlashAttribute("success", deleted
                ? "Đã xoá khách hàng"
                : "Khách hàng đã có đơn đặt vé nên chỉ khoá tài khoản");
        return "redirect:/admin/customers";
    }
}
