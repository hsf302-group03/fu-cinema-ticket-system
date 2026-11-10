package com.fu.cinema.desktop;

import com.fu.cinema.FuCinemaTicketSystemApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class CinemaFxApp extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        // Chạy trên thread JavaFX-Launcher, khởi động Spring trước khi vẽ UI
        context = new SpringApplicationBuilder(FuCinemaTicketSystemApplication.class)
                .web(WebApplicationType.NONE)
                .headless(false)
                .profiles("desktop") // Tự động kết hợp với profile "local" trong application.properties
                .run(getParameters().getRaw().toArray(String[]::new));
    }

    @Override
    public void start(Stage stage) {
        // Phát sự kiện để Spring Bean (StageInitializer) nhận và mở cửa sổ
        context.publishEvent(new StageReadyEvent(stage));
    }

    @Override
    public void stop() {
        // Đóng ApplicationContext để giải phóng tài nguyên và kết nối DB
        if (context != null) {
            context.close();
        }
        Platform.exit();
    }
}