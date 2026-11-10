package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.BookingFacade;
import com.fu.cinema.desktop.model.BookingDetail;
import com.fu.cinema.desktop.model.BookingRow;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.MoneyUtil;
import com.fu.cinema.enums.BookingStatus;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Controller quản lý đơn đặt vé phía Desktop (TODO 44).
 * Spring bean prototype, constructor injection, UI không đơ khi truy vấn.
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class BookingAdminController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm  dd/MM/yyyy");

    private final BookingFacade facade;
    private final FxAsync fxAsync;

    @FXML private ComboBox<BookingStatus> cbStatus;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private TextField tfKeyword;
    @FXML private Button btnRefresh;
    @FXML private Label lblSummary;

    @FXML private TableView<BookingRow> table;
    @FXML private TableColumn<BookingRow, Long> colId;
    @FXML private TableColumn<BookingRow, String> colCustomer;
    @FXML private TableColumn<BookingRow, String> colMovie;
    @FXML private TableColumn<BookingRow, String> colRoom;
    @FXML private TableColumn<BookingRow, LocalDateTime> colStartTime;
    @FXML private TableColumn<BookingRow, Integer> colSeats;
    @FXML private TableColumn<BookingRow, BigDecimal> colTotal;
    @FXML private TableColumn<BookingRow, BookingStatus> colStatus;
    @FXML private TableColumn<BookingRow, LocalDateTime> colBookingDate;

    @FXML private Pagination pagination;
    @FXML private Button btnConfirm;
    @FXML private Button btnCancel;
    @FXML private Button btnDetail;

    private final ObservableList<BookingRow> rows = FXCollections.observableArrayList();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));

    @FXML
    private void initialize() {
        setupTable();
        setupFilters();
        setupActions();

        load(0);
    }

    private void setupTable() {
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().id()));
        colCustomer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().customerName()));
        colMovie.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().movieTitle()));
        colRoom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().roomName()));

        colStartTime.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().startTime()));
        colStartTime.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime time, boolean empty) {
                super.updateItem(time, empty);
                setText(empty || time == null ? null : time.format(DATE_TIME_FORMATTER));
            }
        });

        colSeats.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().seatCount()));
        colSeats.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer count, boolean empty) {
                super.updateItem(count, empty);
                setText(empty || count == null ? null : count + " vé");
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

        colBookingDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().bookingDate()));
        colBookingDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime time, boolean empty) {
                super.updateItem(time, empty);
                setText(empty || time == null ? null : time.format(DATE_TIME_FORMATTER));
            }
        });

        // Double-click mở chi tiết đơn
        table.setRowFactory(tv -> {
            TableRow<BookingRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onDetail();
                }
            });
            return row;
        });

        table.setItems(rows);
    }

    private void setupFilters() {
        // null = "Tất cả trạng thái"
        cbStatus.getItems().setAll(Arrays.asList((BookingStatus) null));
        cbStatus.getItems().addAll(BookingStatus.values());
        cbStatus.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(BookingStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "Tất cả trạng thái" : item.name()));
            }
        });
        cbStatus.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(BookingStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "Tất cả trạng thái" : item.name()));
            }
        });
        cbStatus.setValue(null);

        // Lắng nghe thay đổi bộ lọc -> quay về trang đầu tiên
        cbStatus.valueProperty().addListener((o, a, b) -> search());
        dpFrom.valueProperty().addListener((o, a, b) -> search());
        dpTo.valueProperty().addListener((o, a, b) -> search());

        debounce.setOnFinished(e -> search());
        tfKeyword.textProperty().addListener((o, a, b) -> debounce.playFromStart());

        pagination.currentPageIndexProperty().addListener((o, a, newPage) -> load(newPage.intValue()));
    }

    private void setupActions() {
        ReadOnlyObjectProperty<BookingRow> sel = table.getSelectionModel().selectedItemProperty();

        // Nút Xác nhận: chỉ bật khi dòng chọn có status PENDING
        btnConfirm.disableProperty().bind(Bindings.createBooleanBinding(
                () -> sel.get() == null || sel.get().status() != BookingStatus.PENDING, sel));

        // Nút Hủy: chỉ bật khi dòng chọn có status KHÁC CANCELLED
        btnCancel.disableProperty().bind(Bindings.createBooleanBinding(
                () -> sel.get() == null || sel.get().status() == BookingStatus.CANCELLED, sel));

        // Nút Chi tiết: bật khi có chọn dòng
        btnDetail.disableProperty().bind(sel.isNull());
    }

    private void search() {
        if (pagination.getCurrentPageIndex() != 0) {
            pagination.setCurrentPageIndex(0);
        } else {
            load(0);
        }
    }

    public void load(int page) {
        BookingStatus status = cbStatus.getValue();
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        String keyword = tfKeyword.getText() != null ? tfKeyword.getText().trim() : "";

        fxAsync.run(
                () -> facade.search(status, from, to, keyword, page),
                result -> {
                    rows.setAll(result.items());
                    pagination.setPageCount(result.totalPages());
                    lblSummary.setText("Tổng cộng: " + result.totalElements() + " đơn đặt vé");
                },
                btnRefresh
        );
    }

    @FXML
    private void onRefresh() {
        load(pagination.getCurrentPageIndex());
    }

    @FXML
    private void onConfirm() {
        BookingRow r = table.getSelectionModel().getSelectedItem();
        if (r == null) return;
        if (AlertUtil.confirm("Xác nhận đơn #" + r.id() + " của khách " + r.customerName() + "?")) {
            fxAsync.runVoid(
                    () -> facade.confirm(r.id()),
                    () -> {
                        AlertUtil.info("Đã xác nhận đơn #" + r.id());
                        load(pagination.getCurrentPageIndex());
                    },
                    null
            );
        }
    }

    @FXML
    private void onCancel() {
        BookingRow r = table.getSelectionModel().getSelectedItem();
        if (r == null) return;
        if (AlertUtil.confirm("Huỷ đơn đặt vé #" + r.id() + "? Hành động này không thể hoàn tác.")) {
            fxAsync.runVoid(
                    () -> facade.cancel(r.id()),
                    () -> {
                        AlertUtil.info("Đã huỷ đơn #" + r.id());
                        load(pagination.getCurrentPageIndex());
                    },
                    null
            );
        }
    }

    @FXML
    private void onDetail() {
        BookingRow r = table.getSelectionModel().getSelectedItem();
        if (r == null) return;

        fxAsync.run(
                () -> facade.detail(r.id()),
                this::showDetailDialog,
                null
        );
    }

    private void showDetailDialog(BookingDetail d) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết đơn #" + d.id());
        dialog.setHeaderText("Chi tiết đơn vé #" + d.id() + " — FU Cinema");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        addRow(grid, 0, "Mã đơn:", "#" + d.id());
        addRow(grid, 1, "Khách hàng:", d.customerName() + " (" + d.mobile() + ")");
        addRow(grid, 2, "Tên phim:", d.movieTitle());
        addRow(grid, 3, "Phòng chiếu:", d.roomName());
        addRow(grid, 4, "Suất chiếu:", d.startTime() != null ? d.startTime().format(DATE_TIME_FORMATTER) : "—");
        addRow(grid, 5, "Danh sách ghế:", String.join(", ", d.seats()));
        addRow(grid, 6, "Tổng tiền:", MoneyUtil.format(d.total()));
        addRow(grid, 7, "Trạng thái:", d.status().name());
        addRow(grid, 8, "Thời gian đặt:", d.bookingDate() != null ? d.bookingDate().format(DATE_TIME_FORMATTER) : "—");

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void addRow(GridPane grid, int row, String label, String value) {
        Label lblTitle = new Label(label);
        lblTitle.setStyle("-fx-font-weight: bold;");
        Label lblVal = new Label(value != null ? value : "—");
        grid.add(lblTitle, 0, row);
        grid.add(lblVal, 1, row);
    }
}

