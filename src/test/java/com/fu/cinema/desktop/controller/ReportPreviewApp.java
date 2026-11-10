package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.model.ReportData;
import com.fu.cinema.desktop.model.RevenueRow;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.CsvExporter;
import com.fu.cinema.desktop.util.MoneyUtil;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Ứng dụng xem trước và kiểm thử màn hình Báo cáo doanh thu & Xuất CSV (TODO 45).
 * Dùng để kiểm tra tương tác và chụp ảnh kết quả cho Issue #55.
 */
public class ReportPreviewApp extends Application {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm  dd/MM/yyyy");

    private final ReportData sampleData = new ReportData(
            LocalDate.now().withDayOfMonth(1),
            LocalDate.now(),
            List.of(
                    new RevenueRow("Biệt Đội Siêu Tốc", 45, BigDecimal.valueOf(3600000)),
                    new RevenueRow("Chú Mèo Phiêu Lưu", 32, BigDecimal.valueOf(2240000)),
                    new RevenueRow("Ngôi Nhà Bí Ẩn", 18, BigDecimal.valueOf(1710000))
            ),
            List.of(
                    new ReportBookingRow(102L, LocalDateTime.now().minusDays(1), "Tran Thi Binh", "Chú Mèo Phiêu Lưu", BigDecimal.valueOf(210000)),
                    new ReportBookingRow(105L, LocalDateTime.now().minusHours(5), "Le Hoang Nam", "Chú Mèo Phiêu Lưu", BigDecimal.valueOf(360000)),
                    new ReportBookingRow(106L, LocalDateTime.now().minusHours(2), "Nguyen Van An", "Biệt Đội Siêu Tốc", BigDecimal.valueOf(160000)),
                    new ReportBookingRow(107L, LocalDateTime.now().minusDays(2), "Phan Anh Duc", "Ngôi Nhà Bí Ẩn", BigDecimal.valueOf(285000))
            ),
            4,
            95,
            BigDecimal.valueOf(7550000)
    );

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("page");

        // Tiêu đề
        Label lblTitle = new Label("BÁO CÁO DOANH THU");
        lblTitle.getStyleClass().add("page-title");

        // Bộ lọc thời gian
        HBox filterBox = new HBox(10);
        filterBox.setStyle("-fx-background-color: #f9fafb; -fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #e5e7eb; -fx-border-radius: 6;");

        DatePicker dpStart = new DatePicker(sampleData.start());
        DatePicker dpEnd = new DatePicker(sampleData.end());
        Button btnGenerate = new Button("Tạo báo cáo");
        btnGenerate.getStyleClass().add("primary");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnExport = new Button("Xuất file CSV");
        btnExport.setStyle("-fx-background-color: #15803d; -fx-text-fill: white; -fx-font-weight: bold;");

        filterBox.getChildren().addAll(
                new Label("Từ ngày:"), dpStart,
                new Label("Đến ngày:"), dpEnd,
                btnGenerate,
                spacer,
                btnExport
        );

        // Biểu đồ
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Phim");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Doanh thu (VNĐ)");
        BarChart<String, Number> chartRevenue = new BarChart<>(xAxis, yAxis);
        chartRevenue.setAnimated(false);
        chartRevenue.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        sampleData.revenue().forEach(r -> series.getData().add(new XYChart.Data<>(r.title(), r.revenue())));
        chartRevenue.getData().add(series);

        VBox chartBox1 = new VBox(new Label("Doanh thu theo phim (VNĐ)"), chartRevenue);
        chartBox1.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 6; -fx-padding: 10;");
        HBox.setHgrow(chartBox1, Priority.ALWAYS);

        PieChart pieTickets = new PieChart();
        pieTickets.setAnimated(false);
        sampleData.revenue().forEach(r -> pieTickets.getData().add(new PieChart.Data(r.title() + " (" + r.tickets() + " vé)", r.tickets())));

        VBox chartBox2 = new VBox(new Label("Tỷ trọng số vé bán ra"), pieTickets);
        chartBox2.setPrefWidth(380);
        chartBox2.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 6; -fx-padding: 10;");

        HBox chartsHBox = new HBox(16, chartBox1, chartBox2);
        chartsHBox.setPrefHeight(330);

        // Bảng đơn hàng
        TableView<ReportBookingRow> table = new TableView<>();
        table.setPrefHeight(240);

        TableColumn<ReportBookingRow, Long> colId = new TableColumn<>("Mã đơn");
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().id()));
        colId.setPrefWidth(80);

        TableColumn<ReportBookingRow, LocalDateTime> colDate = new TableColumn<>("Ngày đặt vé");
        colDate.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().bookingDate()));
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.format(DATE_TIME_FORMATTER));
            }
        });
        colDate.setPrefWidth(150);

        TableColumn<ReportBookingRow, String> colCust = new TableColumn<>("Khách hàng");
        colCust.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().customerName()));
        colCust.setPrefWidth(200);

        TableColumn<ReportBookingRow, String> colMovie = new TableColumn<>("Phim đã xem");
        colMovie.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().movieTitle()));
        colMovie.setPrefWidth(240);

        TableColumn<ReportBookingRow, BigDecimal> colTotal = new TableColumn<>("Tổng tiền");
        colTotal.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().total()));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : MoneyUtil.format(val));
            }
        });
        colTotal.setPrefWidth(140);

        table.getColumns().addAll(colId, colDate, colCust, colMovie, colTotal);
        table.setItems(FXCollections.observableArrayList(sampleData.bookings()));

        Label lblTotals = new Label("Tổng cộng: %d đơn · %d vé · %s".formatted(
                sampleData.totalBookings(), sampleData.totalTickets(), MoneyUtil.format(sampleData.totalRevenue())
        ));
        lblTotals.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #e50914;");

        HBox tableHeader = new HBox(new Label("Danh sách đơn đặt vé thành công (CONFIRMED):"), new Region(), lblTotals);
        HBox.setHgrow(tableHeader.getChildren().get(1), Priority.ALWAYS);

        VBox tableBox = new VBox(8, tableHeader, table);
        tableBox.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 6; -fx-padding: 12;");

        VBox scrollContent = new VBox(16, chartsHBox, tableBox);
        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Xuất file CSV
        btnExport.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Lưu báo cáo doanh thu");
            fc.setInitialFileName("doanh-thu_%s_%s.csv".formatted(sampleData.start(), sampleData.end()));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tập tin CSV (*.csv)", "*.csv"));
            File file = fc.showSaveDialog(stage);
            if (file != null) {
                CsvExporter.write(file.toPath(), sampleData);
                AlertUtil.info("Xuất báo cáo thành công ra file:\n" + file.getAbsolutePath());
            }
        });

        root.getChildren().addAll(lblTitle, filterBox, scrollPane);

        Scene scene = new Scene(root, 1080, 700);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/desktop/css/desktop.css")).toExternalForm());

        stage.setTitle("FU Cinema — Báo cáo doanh thu (TODO 45)");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

