package com.fu.cinema.desktop;

import javafx.application.Application;

/**
 * Entry point cho ứng dụng Desktop.
 * TECH-12: KHÔNG kế thừa Application để tránh lỗi "JavaFX runtime components are missing".
 */
public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        // Bắt buộc đặt TRƯỚC khi Spring khởi động để tránh ClassCastException trên FXML
        System.setProperty("spring.devtools.restart.enabled", "false");
        Application.launch(CinemaFxApp.class, args);
    }
}