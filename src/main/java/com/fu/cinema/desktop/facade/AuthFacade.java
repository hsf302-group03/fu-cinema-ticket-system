package com.fu.cinema.desktop.facade;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Facade xác thực đăng nhập trên Desktop (TODO 36 & BR-D1).
 * Chỉ cho phép tài khoản có quyền ADMIN đăng nhập ứng dụng quầy vé.
 */
@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final AuthService authService;

    /**
     * Đăng nhập nhân viên quầy vé:
     * - Kiểm tra thông tin đăng nhập và trạng thái tài khoản qua AuthService (BR-03)
     * - Ràng buộc BR-D1: Chặn tài khoản CUSTOMER, chỉ cho phép ADMIN
     */
    public SessionUser loginAdmin(String username, String password) {
        SessionUser user = authService.login(username, password);
        if (!user.isAdmin()) {
            throw new BusinessException("Ứng dụng quầy vé chỉ dành cho nhân viên (tài khoản ADMIN)");
        }
        return user;
    }
}

