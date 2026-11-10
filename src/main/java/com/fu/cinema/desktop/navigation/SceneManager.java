package com.fu.cinema.desktop.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SceneManager {

    private static final String CSS = "/desktop/css/desktop.css";

    private final ApplicationContext context;
    private Stage stage;
    private Scene scene;

    public void init(Stage stage) {
        this.stage = stage;
        this.scene = new Scene(new StackPane(), 1200, 720);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(CSS)).toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        stage.show();
    }

    /** Đổi toàn bộ cửa sổ (ví dụ: Login <-> Main) */
    public void switchTo(View view) {
        scene.setRoot(load(view).root());
        stage.setTitle(view.title());
    }

    /** Nạp màn hình con vào vùng center của khung chính */
    public void showIn(BorderPane host, View view) {
        try {
            host.setCenter(load(view).root());
        } catch (Exception e) {
            // Khi chưa tạo các FXML con ở các TODO sau
            host.setCenter(new javafx.scene.control.Label("Màn hình: " + view.title()));
        }
    }

    public <C> Loaded<C> load(View view) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(view.path()));
        // TECH-13: Controller là Spring Bean (prototype)
        loader.setControllerFactory(context::getBean);
        try {
            Parent root = loader.load();
            return new Loaded<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Không nạp được " + view.path(), e);
        }
    }

    public Stage getStage() {
        return stage;
    }

    public record Loaded<C>(Parent root, C controller) {}
}