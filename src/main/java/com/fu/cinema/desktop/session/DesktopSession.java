package com.fu.cinema.desktop.session;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.exception.BusinessException;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.springframework.stereotype.Component;

/**
 * Quản lý phiên làm việc Desktop thay cho HttpSession của web.
 * Singleton: 1 người dùng / 1 ứng dụng Desktop (TODO 36 & TECH-12).
 */
@Component
public class DesktopSession {

    private volatile SessionUser user;
    private final ReadOnlyStringWrapper displayName = new ReadOnlyStringWrapper("");

    public void login(SessionUser user) {
        this.user = user;
        displayName.set(user.getDisplayName());
    }

    public void logout() {
        this.user = null;
        displayName.set("");
    }

    public SessionUser getUser() {
        return user;
    }

    public ReadOnlyStringProperty displayNameProperty() {
        return displayName.getReadOnlyProperty();
    }

    /**
     * Kiểm tra quyền ADMIN ở mọi thao tác ghi của Facade (BR-D1).
     */
    public void requireAdmin() {
        SessionUser u = user;
        if (u == null || !u.isAdmin()) {
            throw new BusinessException("Bạn không có quyền thực hiện thao tác này");
        }
    }
}

