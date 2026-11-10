package com.fu.cinema.desktop;

import javafx.application.Application;

/**
 * Entry point thứ hai cho ứng dụng Desktop JavaFX (TECH-12, TODO 33).
 * KHÔNG extends Application để tránh lỗi "JavaFX runtime components are missing".
 * Entry point cho ứng dụng Desktop.
 * TECH-12: KHÔNG kế thừa Application để tránh lỗi "JavaFX runtime components are missing".
 */
public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        // Tắt DevTools restart trước khi Spring khởi động (tránh ClassCastException trong FXMLLoader)
        System.setProperty("spring.devtools.restart.enabled", "false");
        Application.launch(CinemaFxApp.class, args);
    }
}

        // Bắt buộc đặt TRƯỚC khi Spring khởi động để tránh ClassCastException trên FXML
        System.setProperty("spring.devtools.restart.enabled", "false");
        Application.launch(CinemaFxApp.class, args);
    }
}
