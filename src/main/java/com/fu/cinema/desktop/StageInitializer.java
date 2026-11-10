package com.fu.cinema.desktop;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe sự kiện StageReadyEvent để hiển thị giao diện Desktop ban đầu (TODO 33).
 */
@Component
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();
        Label label = new Label("FU Cinema — Ứng dụng Quầy vé Desktop");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e50914;");

        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 1000, 640);
        stage.setTitle("FU Cinema — Quầy vé Desktop");
        stage.setScene(scene);
        stage.show();
    }
}

