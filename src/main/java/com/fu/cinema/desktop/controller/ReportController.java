package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.ReportFacade;
import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.model.ReportData;
import com.fu.cinema.desktop.model.RevenueRow;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.CsvExporter;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.MoneyUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller màn hình báo cáo doanh thu và xuất file CSV (TODO 45).
 * Spring bean prototype, hiển thị biểu đồ BarChart + PieChart + Bảng đơn hàng.
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ReportController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm  dd/MM/yyyy");

    private final ReportFacade facade;
    private final FxAsync fxAsync;

    @FXML private DatePicker dpStart;
    @FXML private DatePicker dpEnd;
    @FXML private Button btnGenerate;
    @FXML private Button btnExport;
    @FXML private Label lblTotals;
    @FXML private Label lblEmpty;

    @FXML private BarChart<String, Number> chartRevenue;
    @FXML private PieChart pieTickets;

    @FXML private TableView<ReportBookingRow> tableBookings;
    @FXML private TableColumn<ReportBookingRow, Long> colId;
    @FXML private TableColumn<ReportBookingRow, LocalDateTime> colDate;
    @FXML private TableColumn<ReportBookingRow, String> colCustomer;
    @FXML private TableColumn<ReportBookingRow, String> colMovie;
    @FXML private TableColumn<ReportBookingRow, BigDecimal> colTotal;

    private final ObjectProperty<ReportData> data = new SimpleObjectProperty<>();
    private final ObservableList<ReportBookingRow> bookingRows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupDatePickers();
        setupTable();
        setupCharts();

        // Nút xuất CSV chỉ bật khi có dữ liệu báo cáo
        btnExport.disableProperty().bind(Bindings.createBooleanBinding(
                () -> data.get() == null || data.get().isEmpty(), data
        ));

        // Tự động tạo báo cáo cho tháng hiện tại
        onGenerate();
    }

    private void setupDatePickers() {
        dpStart.setValue(LocalDate.now().withDayOfMonth(1));
        dpEnd.setValue(LocalDate.now());

        // Chặn không cho chọn ngày kết thúc nhỏ hơn ngày bắt đầu
        dpEnd.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || (dpStart.getValue() != null && d.isBefore(dpStart.getValue())));
            }
        });
    }

    private void setupTable() {
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().id()));
        colCustomer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().customerName()));
        colMovie.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().movieTitle()));

        colDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().bookingDate()));
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime time, boolean empty) {
                super.updateItem(time, empty);
                setText(empty || time == null ? null : time.format(DATE_TIME_FORMATTER));
            }
        });

        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().total()));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : MoneyUtil.format(val));
            }
        });

        tableBookings.setItems(bookingRows);
    }

    private void setupCharts() {
        chartRevenue.setAnimated(false);
        pieTickets.setAnimated(false);
    }

    @FXML
    private void onGenerate() {
        LocalDate start = dpStart.getValue();
        LocalDate end = dpEnd.getValue();
        if (start == null || end == null) {
            AlertUtil.warn("Vui lòng chọn khoảng thời gian hợp lệ.");
            return;
        }

        fxAsync.run(
                () -> facade.generate(start, end),
                this::render,
                btnGenerate
        );
    }

    private void render(ReportData d) {
        data.set(d);
        boolean empty = d.isEmpty();
        lblEmpty.setVisible(empty);
        lblEmpty.setManaged(empty);

        bookingRows.setAll(d.bookings());
        lblTotals.setText("Tổng cộng: %d đơn · %d vé · %s".formatted(
                d.totalBookings(),
                d.totalTickets(),
                MoneyUtil.format(d.totalRevenue())
        ));

        // 1. Vẽ biểu đồ BarChart doanh thu theo phim
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu (VNĐ)");
        d.revenue().forEach(r -> series.getData().add(new XYChart.Data<>(r.title(), r.revenue())));
        chartRevenue.getData().setAll(List.of(series));

        // Cài đặt Tooltip cho từng cột sau khi series đã nạp vào chart
        for (int i = 0; i < series.getData().size(); i++) {
            RevenueRow r = d.revenue().get(i);
            XYChart.Data<String, Number> item = series.getData().get(i);
            if (item.getNode() != null) {
                Tooltip.install(item.getNode(), new Tooltip("%s: %d vé – %s".formatted(
                        r.title(), r.tickets(), MoneyUtil.format(r.revenue()))));
            }
        }

        // 2. Vẽ biểu đồ PieChart tỷ trọng số vé
        pieTickets.getData().setAll(d.revenue().stream()
                .filter(r -> r.tickets() > 0)
                .map(r -> new PieChart.Data(r.title() + " (" + r.tickets() + " vé)", r.tickets()))
                .toList()
        );
    }

    @FXML
    private void onExport() {
        if (data.get() == null || data.get().isEmpty()) {
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Lưu báo cáo doanh thu");
        fc.setInitialFileName("doanh-thu_%s_%s.csv".formatted(data.get().start(), data.get().end()));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tập tin CSV (*.csv)", "*.csv"));

        File file = fc.showSaveDialog(btnExport.getScene().getWindow());
        if (file == null) {
            return; // Người dùng bấm Cancel -> không làm gì, không báo lỗi
        }

        ReportData snapshot = data.get();
        fxAsync.runVoid(
                () -> CsvExporter.write(file.toPath(), snapshot),
                () -> AlertUtil.info("Xuất báo cáo thành công ra file:\n" + file.getAbsolutePath()),
                btnExport
        );
    }
}

