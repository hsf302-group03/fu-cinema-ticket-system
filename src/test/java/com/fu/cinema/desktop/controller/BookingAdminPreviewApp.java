package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.model.BookingDetail;
import com.fu.cinema.desktop.model.BookingRow;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.MoneyUtil;
import com.fu.cinema.enums.BookingStatus;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Ứng dụng xem trước và kiểm thử giao diện Quản lý đơn đặt vé (TODO 44).
 * Dùng để kiểm tra tương tác và chụp ảnh kết quả cho Issue #54.
 */
public class BookingAdminPreviewApp extends Application {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm  dd/MM/yyyy");

    private final ObservableList<BookingRow> allRows = FXCollections.observableArrayList(
            new BookingRow(101L, "Nguyen Van An", "Biệt Đội Siêu Tốc", "Room 01", LocalDateTime.now().plusDays(1).withHour(9).withMinute(0), 2, BigDecimal.valueOf(160000), BookingStatus.PENDING, LocalDateTime.now().minusHours(2)),
            new BookingRow(102L, "Tran Thi Binh", "Chú Mèo Phiêu Lưu", "Room 01", LocalDateTime.now().plusDays(1).withHour(14).withMinute(0), 3, BigDecimal.valueOf(210000), BookingStatus.CONFIRMED, LocalDateTime.now().minusDays(1)),
            new BookingRow(103L, "Nguyen Van An", "Ngôi Nhà Bí Ẩn", "Room 02", LocalDateTime.now().plusDays(1).withHour(21).withMinute(0), 1, BigDecimal.valueOf(95000), BookingStatus.PENDING, LocalDateTime.now().minusMinutes(45)),
            new BookingRow(104L, "Tran Thi Binh", "Biệt Đội Siêu Tốc", "Room 01", LocalDateTime.now().plusDays(1).withHour(9).withMinute(0), 1, BigDecimal.valueOf(80000), BookingStatus.CANCELLED, LocalDateTime.now().minusDays(2)),
            new BookingRow(105L, "Le Hoang Nam", "Chú Mèo Phiêu Lưu", "Room 02", LocalDateTime.now().plusDays(2).withHour(18).withMinute(30), 4, BigDecimal.valueOf(360000), BookingStatus.CONFIRMED, LocalDateTime.now().minusHours(5))
    );

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("page");

        // Tiêu đề
        HBox header = new HBox(16);
        Label lblTitle = new Label("QUẢN LÝ ĐƠN ĐẶT VÉ");
        lblTitle.getStyleClass().add("page-title");
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Label lblSummary = new Label("Tổng cộng: " + allRows.size() + " đơn đặt vé");
        lblSummary.setStyle("-fx-font-style: italic; -fx-text-fill: #6b7280;");
        header.getChildren().addAll(lblTitle, spacer1, lblSummary);

        // Thanh lọc
        HBox filterBox = new HBox(10);
        filterBox.setStyle("-fx-background-color: #f9fafb; -fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #e5e7eb; -fx-border-radius: 6;");
        ComboBox<String> cbStatus = new ComboBox<>(FXCollections.observableArrayList("Tất cả trạng thái", "PENDING", "CONFIRMED", "CANCELLED"));
        cbStatus.setValue("Tất cả trạng thái");
        DatePicker dpFrom = new DatePicker();
        dpFrom.setPromptText("Từ ngày");
        DatePicker dpTo = new DatePicker();
        dpTo.setPromptText("Đến ngày");
        TextField tfKeyword = new TextField();
        tfKeyword.setPromptText("Tìm theo tên hoặc SĐT khách...");
        HBox.setHgrow(tfKeyword, Priority.ALWAYS);
        Button btnRefresh = new Button("Làm mới");
        filterBox.getChildren().addAll(new Label("Lọc:"), cbStatus, dpFrom, dpTo, tfKeyword, btnRefresh);

        // Bảng dữ liệu
        TableView<BookingRow> table = new TableView<>();
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<BookingRow, Long> colId = new TableColumn<>("Mã đơn");
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().id()));
        colId.setPrefWidth(75);

        TableColumn<BookingRow, String> colCustomer = new TableColumn<>("Khách hàng");
        colCustomer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().customerName()));
        colCustomer.setPrefWidth(160);

        TableColumn<BookingRow, String> colMovie = new TableColumn<>("Tên phim");
        colMovie.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().movieTitle()));
        colMovie.setPrefWidth(180);

        TableColumn<BookingRow, String> colRoom = new TableColumn<>("Phòng");
        colRoom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().roomName()));
        colRoom.setPrefWidth(80);

        TableColumn<BookingRow, LocalDateTime> colStartTime = new TableColumn<>("Suất chiếu");
        colStartTime.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().startTime()));
        colStartTime.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime time, boolean empty) {
                super.updateItem(time, empty);
                setText(empty || time == null ? null : time.format(DATE_TIME_FORMATTER));
            }
        });
        colStartTime.setPrefWidth(140);

        TableColumn<BookingRow, Integer> colSeats = new TableColumn<>("Số vé");
        colSeats.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().seatCount()));
        colSeats.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer count, boolean empty) {
                super.updateItem(count, empty);
                setText(empty || count == null ? null : count + " vé");
            }
        });
        colSeats.setPrefWidth(70);

        TableColumn<BookingRow, BigDecimal> colTotal = new TableColumn<>("Tổng tiền");
        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().total()));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : MoneyUtil.format(val));
            }
        });
        colTotal.setPrefWidth(120);

        TableColumn<BookingRow, BookingStatus> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().status()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BookingStatus st, boolean empty) {
                super.updateItem(st, empty);
                getStyleClass().removeAll("st-pending", "st-confirmed", "st-cancelled");
                if (empty || st == null) {
                    setText(null);
                } else {
                    setText(st.name());
                    getStyleClass().add("st-" + st.name().toLowerCase());
                }
            }
        });
        colStatus.setPrefWidth(120);

        TableColumn<BookingRow, LocalDateTime> colBookingDate = new TableColumn<>("Thời gian đặt");
        colBookingDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().bookingDate()));
        colBookingDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime time, boolean empty) {
                super.updateItem(time, empty);
                setText(empty || time == null ? null : time.format(DATE_TIME_FORMATTER));
            }
        });
        colBookingDate.setPrefWidth(140);

        table.getColumns().addAll(colId, colCustomer, colMovie, colRoom, colStartTime, colSeats, colTotal, colStatus, colBookingDate);
        table.setItems(allRows);

        // Nút thao tác
        ReadOnlyObjectProperty<BookingRow> sel = table.getSelectionModel().selectedItemProperty();
        Button btnConfirm = new Button("Xác nhận đơn");
        btnConfirm.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold;");
        btnConfirm.disableProperty().bind(Bindings.createBooleanBinding(
                () -> sel.get() == null || sel.get().status() != BookingStatus.PENDING, sel));

        Button btnCancel = new Button("Huỷ đơn");
        btnCancel.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCancel.disableProperty().bind(Bindings.createBooleanBinding(
                () -> sel.get() == null || sel.get().status() == BookingStatus.CANCELLED, sel));

        Button btnDetail = new Button("Xem chi tiết");
        btnDetail.disableProperty().bind(sel.isNull());

        btnConfirm.setOnAction(e -> {
            BookingRow r = sel.get();
            if (r != null && AlertUtil.confirm("Xác nhận đơn #" + r.id() + " của khách " + r.customerName() + "?")) {
                int idx = allRows.indexOf(r);
                allRows.set(idx, new BookingRow(r.id(), r.customerName(), r.movieTitle(), r.roomName(), r.startTime(), r.seatCount(), r.total(), BookingStatus.CONFIRMED, r.bookingDate()));
                AlertUtil.info("Đã xác nhận đơn #" + r.id() + " thành công!");
            }
        });

        btnCancel.setOnAction(e -> {
            BookingRow r = sel.get();
            if (r != null && AlertUtil.confirm("Huỷ đơn đặt vé #" + r.id() + "? Hành động này không thể hoàn tác.")) {
                int idx = allRows.indexOf(r);
                allRows.set(idx, new BookingRow(r.id(), r.customerName(), r.movieTitle(), r.roomName(), r.startTime(), r.seatCount(), r.total(), BookingStatus.CANCELLED, r.bookingDate()));
                AlertUtil.info("Đã huỷ đơn #" + r.id() + " thành công!");
            }
        });

        Runnable showDetail = () -> {
            BookingRow r = sel.get();
            if (r == null) return;
            Dialog<Void> dlg = new Dialog<>();
            dlg.setTitle("Chi tiết đơn #" + r.id());
            dlg.setHeaderText("Chi tiết đơn vé #" + r.id() + " — FU Cinema");

            GridPane g = new GridPane();
            g.setHgap(15);
            g.setVgap(10);
            g.setPadding(new Insets(20));

            addD(g, 0, "Mã đơn:", "#" + r.id());
            addD(g, 1, "Khách hàng:", r.customerName() + " (0901234567)");
            addD(g, 2, "Tên phim:", r.movieTitle());
            addD(g, 3, "Phòng chiếu:", r.roomName());
            addD(g, 4, "Suất chiếu:", r.startTime().format(DATE_TIME_FORMATTER));
            addD(g, 5, "Danh sách ghế:", "E5, E6");
            addD(g, 6, "Tổng tiền:", MoneyUtil.format(r.total()));
            addD(g, 7, "Trạng thái:", r.status().name());
            addD(g, 8, "Thời gian đặt:", r.bookingDate().format(DATE_TIME_FORMATTER));

            dlg.getDialogPane().setContent(g);
            dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dlg.showAndWait();
        };

        btnDetail.setOnAction(e -> showDetail.run());
        table.setRowFactory(tv -> {
            TableRow<BookingRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showDetail.run();
                }
            });
            return row;
        });

        HBox footer = new HBox(12, btnConfirm, btnCancel, btnDetail);

        root.getChildren().addAll(header, filterBox, table, footer);

        Scene scene = new Scene(root, 1080, 640);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/desktop/css/desktop.css")).toExternalForm());

        stage.setTitle("FU Cinema — Quản lý đơn đặt vé (TODO 44)");
        stage.setScene(scene);
        stage.show();
    }

    private void addD(GridPane g, int r, String t, String v) {
        Label l1 = new Label(t);
        l1.setStyle("-fx-font-weight: bold;");
        Label l2 = new Label(v);
        g.add(l1, 0, r);
        g.add(l2, 1, r);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

