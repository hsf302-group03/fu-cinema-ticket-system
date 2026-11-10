package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.DashboardFacade;
import com.fu.cinema.desktop.model.DashboardData;
import com.fu.cinema.desktop.util.FxAsync;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class DashboardController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DashboardFacade facade;
    private final FxAsync fxAsync;

    @FXML private VBox root;
    @FXML private Label lblShowing;
    @FXML private Label lblCustomers;
    @FXML private Label lblPending;
    @FXML private Label lblUpdated;
    @FXML private BarChart<String, Number> chartRooms;
    @FXML private Button btnRefresh;

    private Timeline autoRefresh;

    @FXML
    private void initialize() {
        chartRooms.setAnimated(false); // Tránh giật animation khi cập nhật biểu đồ

        // Tự động làm mới dữ liệu mỗi 60 giây
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(60), e -> refresh()));
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        autoRefresh.play();

        // Khi người dùng chuyển sang màn hình khác, dừng timeline để tránh lãng phí RAM
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && autoRefresh != null) {
                autoRefresh.stop();
            }
        });

        refresh();
    }

    @FXML
    private void refresh() {
        fxAsync.run(facade::load, this::render, btnRefresh);
    }

    private void render(DashboardData d) {
        lblShowing.setText(String.valueOf(d.showingMovies()));
        lblCustomers.setText(String.valueOf(d.customers()));
        lblPending.setText(String.valueOf(d.pendingBookings()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Suất chiếu hôm nay");
        d.todayByRoom().forEach(r -> series.getData().add(new XYChart.Data<>(r.roomName(), r.count())));
        chartRooms.getData().setAll(List.of(series));

        lblUpdated.setText("Cập nhật lúc: " + LocalTime.now().format(TIME_FMT));
    }
}