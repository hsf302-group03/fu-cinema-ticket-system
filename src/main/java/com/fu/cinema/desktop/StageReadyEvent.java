package com.fu.cinema.desktop;

import javafx.stage.Stage;
import org.springframework.context.ApplicationEvent;

/**
 * Sự kiện Spring thông báo JavaFX Stage chính đã sẵn sàng (TODO 33).
 */
public class StageReadyEvent extends ApplicationEvent {

    public StageReadyEvent(Stage stage) {
        super(stage);
    }

    public Stage getStage() {
        return (Stage) getSource();
    }
}

