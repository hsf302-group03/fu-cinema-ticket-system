package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.component.SeatMapView;
import com.fu.cinema.desktop.model.CustomerItem;
import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.SeatMapData;
import com.fu.cinema.desktop.model.ShowtimeItem;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.MoneyUtil;
import com.fu.cinema.enums.AgeRating;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Ứng dụng xem trước và kiểm thử giao diện Bán vé tại quầy (TODO 43).
 * Dùng để kiểm tra tương tác và chụp ảnh kết quả cho Issue #53.
 */
public class CounterSalePreviewApp extends Application {

    private final ObservableList<CustomerItem> customers = FXCollections.observableArrayList(
            new CustomerItem(1L, "Nguyen Van An", "0901234567", 26),
            new CustomerItem(2L, "Tran Thi Binh", "0912345678", 15),
            new CustomerItem(3L, "Le Hoang Nam", "0987654321", 30)
    );

    private final ObservableList<MovieItem> movies = FXCollections.observableArrayList(
            new MovieItem(1L, "Biệt Đội Siêu Tốc", 120, AgeRating.T13),
            new MovieItem(2L, "Chú Mèo Phiêu Lưu", 95, AgeRating.P),
            new MovieItem(3L, "Ngôi Nhà Bí Ẩn", 105, AgeRating.T18)
    );

    private BigDecimal currentPrice = BigDecimal.valueOf(80000);

    @Override
    public void start(Stage stage) {
        // --- Cột trái: Form điều khiển ---
        Label lblTitle = new Label("BÁN VÉ TẠI QUẦY");
        lblTitle.getStyleClass().add("page-title");

        TextField tfSearch = new TextField();
        tfSearch.setPromptText("Tìm theo tên hoặc SĐT...");
        ListView<CustomerItem> lvCustomers = new ListView<>(customers);
        lvCustomers.setPrefHeight(120);

        ComboBox<MovieItem> cbMovie = new ComboBox<>(movies);
        cbMovie.setPromptText("-- Chọn phim --");
        cbMovie.setMaxWidth(Double.MAX_VALUE);

        ComboBox<ShowtimeItem> cbShowtime = new ComboBox<>();
        cbShowtime.setPromptText("-- Chọn suất chiếu --");
        cbShowtime.setMaxWidth(Double.MAX_VALUE);

        Label lblSeats = new Label("Ghế: Chưa chọn ghế");
        lblSeats.setStyle("-fx-font-weight: bold;");

        Label lblTotal = new Label("Tổng: 0 đ");
        lblTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e50914;");

        CheckBox chkPaid = new CheckBox("Đã thanh toán (Chuyển CONFIRMED)");
        chkPaid.setSelected(true);
        chkPaid.setStyle("-fx-font-weight: bold;");

        Button btnSell = new Button("Bán vé");
        btnSell.getStyleClass().add("primary");
        btnSell.setMaxWidth(Double.MAX_VALUE);
        btnSell.setPrefHeight(40);

        VBox left = new VBox(10,
                lblTitle,
                new Label("Khách hàng:"), tfSearch, lvCustomers,
                new Separator(),
                new Label("Phim:"), cbMovie,
                new Label("Suất chiếu:"), cbShowtime,
                new Separator(),
                lblSeats, lblTotal, chkPaid,
                new Region(),
                btnSell
        );
        VBox.setVgrow(left.getChildren().get(left.getChildren().size() - 2), Priority.ALWAYS);
        left.setPrefWidth(320);
        left.setPadding(new Insets(15));
        left.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-width: 0 1 0 0;");

        // --- Cột phải: Sơ đồ ghế ---
        SeatMapView seatMap = new SeatMapView();
        ScrollPane center = new ScrollPane(seatMap);
        center.setFitToWidth(true);
        center.setFitToHeight(true);

        // --- Logic tương tác ---
        cbMovie.valueProperty().addListener((obs, oldM, newM) -> {
            cbShowtime.getItems().clear();
            seatMap.setData(null);
            if (newM != null) {
                cbShowtime.getItems().setAll(
                        new ShowtimeItem(10L, LocalDateTime.now().plusHours(2), "Room 01", BigDecimal.valueOf(80000)),
                        new ShowtimeItem(11L, LocalDateTime.now().plusHours(5), "Room 02", BigDecimal.valueOf(90000))
                );
            }
        });

        cbShowtime.valueProperty().addListener((obs, oldSt, newSt) -> {
            if (newSt != null) {
                currentPrice = newSt.price();
                List<String> rows = List.of("A", "B", "C", "D", "E", "F", "G", "H");
                seatMap.setData(new SeatMapData(
                        newSt.id(), cbMovie.getValue().title(), newSt.roomName(), newSt.start(),
                        newSt.price(), rows, 12, Set.of("A1", "A2", "C5", "C6", "H12")
                ));
            } else {
                seatMap.setData(null);
            }
        });

        // Binding tổng tiền
        ObjectBinding<BigDecimal> totalBinding = Bindings.createObjectBinding(
                () -> currentPrice.multiply(BigDecimal.valueOf(seatMap.getSelectedSeats().size())),
                seatMap.getSelectedSeats()
        );
        lblTotal.textProperty().bind(Bindings.createStringBinding(
                () -> "Tổng: " + MoneyUtil.format(totalBinding.get()), totalBinding
        ));
        lblSeats.textProperty().bind(Bindings.createStringBinding(
                () -> "Ghế: " + (seatMap.getSelectedSeats().isEmpty()
                        ? "Chưa chọn ghế"
                        : String.join(", ", seatMap.getSelectedSeats().sorted())),
                seatMap.getSelectedSeats()
        ));

        // Nút Bán vé chỉ bật khi đủ điều kiện
        btnSell.disableProperty().bind(
                lvCustomers.getSelectionModel().selectedItemProperty().isNull()
                        .or(cbShowtime.valueProperty().isNull())
                        .or(Bindings.isEmpty(seatMap.getSelectedSeats()))
        );

        btnSell.setOnAction(e -> {
            CustomerItem c = lvCustomers.getSelectionModel().getSelectedItem();
            List<String> seats = List.copyOf(seatMap.getSelectedSeats());
            String status = chkPaid.isSelected() ? "CONFIRMED" : "PENDING";
            if (AlertUtil.confirm("Xác nhận bán %d vé (%s) cho %s?".formatted(seats.size(), String.join(", ", seats), c.fullName()))) {
                AlertUtil.info("Bán vé thành công!\nMã đơn: #10088\nKhách: %s\nGhế: %s\nTổng tiền: %s\nTrạng thái: %s".formatted(
                        c.fullName(), String.join(", ", seats), MoneyUtil.format(totalBinding.get()), status
                ));
            }
        });

        BorderPane root = new BorderPane();
        root.setLeft(left);
        root.setCenter(center);

        Scene scene = new Scene(root, 1050, 680);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/desktop/css/desktop.css")).toExternalForm());

        stage.setTitle("FU Cinema — Kiểm thử Màn hình Bán vé tại quầy (TODO 43)");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

