package com.fu.cinema.desktop;

import com.fu.cinema.FuCinemaTicketSystemApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Lớp vòng đời ứng dụng JavaFX tích hợp Spring Boot (TECH-12, TODO 33).
 */
public class CinemaFxApp extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        // Khởi động Spring Boot trong thread Launcher, tắt web server (không mở Tomcat)
        context = new SpringApplicationBuilder(FuCinemaTicketSystemApplication.class)
                .web(WebApplicationType.NONE)
                .headless(false)
                .profiles("desktop")
                .run(getParameters().getRaw().toArray(String[]::new));
    }

    @Override
    public void start(Stage stage) {
        context.publishEvent(new StageReadyEvent(stage));
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
        Platform.exit();
    }
}

