package com.fu.cinema.config;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        SessionUser user = SessionUtils.getCurrentUser(request.getSession(false));
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (user == null) {                                                  // chưa đăng nhập
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        boolean forbidden = (path.startsWith("/admin") && user.getRole() != Role.ADMIN)
                || (path.startsWith("/customer") && user.getRole() != Role.CUSTOMER);
        if (forbidden) {                                                     // sai vai trò → 403
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}
