package com.fu.cinema.util;

import com.fu.cinema.dto.SessionUser;
import jakarta.servlet.http.HttpSession;

public final class SessionUtils {

    public static final String CURRENT_USER = "currentUser";

    private SessionUtils() {
    }

    public static SessionUser getCurrentUser(HttpSession session) {
        return session == null ? null : (SessionUser) session.getAttribute(CURRENT_USER);
    }

    /** Luôn lấy customerId từ session, KHÔNG lấy từ form/URL. */
    public static Long getCurrentCustomerId(HttpSession session) {
        SessionUser user = getCurrentUser(session);
        return user == null ? null : user.getCustomerId();
    }
}
