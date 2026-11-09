package com.fu.cinema.desktop.session;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.exception.BusinessException;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.springframework.stereotype.Component;

/** Thay cho HttpSession của web. Singleton: 1 người dùng / 1 cửa sổ */
@Component
public class DesktopSession {

    private volatile SessionUser user;
    private final ReadOnlyStringWrapper displayName = new ReadOnlyStringWrapper("Quản trị viên");

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

    public void requireAdmin() {
        SessionUser u = user;
        if (u == null || !u.isAdmin()) {
            throw new BusinessException("Bạn không có quyền thực hiện thao tác này");
        }
    }
}