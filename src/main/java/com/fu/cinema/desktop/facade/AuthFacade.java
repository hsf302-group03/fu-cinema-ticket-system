package com.fu.cinema.desktop.facade;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final AuthService authService; // Tái sử dụng AuthService của web (đã có BCrypt & kiểm tra LOCKED BR-03)

    public SessionUser loginAdmin(String username, String password) {
        SessionUser user = authService.login(username, password);
        // Quy tắc BR-D1: Desktop chỉ cho phép ADMIN đăng nhập
        if (!user.isAdmin()) {
            throw new BusinessException("Ứng dụng quầy vé chỉ dành cho nhân viên (tài khoản ADMIN)");
        }
        return user;
    }
}