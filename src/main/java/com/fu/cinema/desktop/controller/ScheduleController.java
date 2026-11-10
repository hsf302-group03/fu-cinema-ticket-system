package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.ScheduleFacade;
import com.fu.cinema.desktop.model.*;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.FxValidator;
import com.fu.cinema.desktop.util.MoneyUtil;
import com.fu.cinema.dto.RoomForm;
import com.fu.cinema.dto.ShowtimeForm;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;
import com.fu.cinema.enums.ShowtimeStatus;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ScheduleController {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final ScheduleFacade facade;
    private final FxAsync fxAsync;
    private final FxValidator validator;

    // ===== Tab Phòng chiếu =====
    @FXML private TableView<RoomRow> tableRooms;
    @FXML private TableColumn<RoomRow, String> colRoomName;
    @FXML private TableColumn<RoomRow, String> colRoomType;
    @FXML private TableColumn<RoomRow, Integer> colRows;
    @FXML private TableColumn<RoomRow, Integer> colSeatsPerRow;
    @FXML private TableColumn<RoomRow, Integer> colCapacity;
    @FXML private TableColumn<RoomRow, String> colRoomStatus;

    @FXML private VBox formBoxRoom;
    @FXML private TextField tfRoomName;
    @FXML private ComboBox<RoomType> cbRoomType;
    @FXML private Spinner<Integer> spTotalRows;
    @FXML private Spinner<Integer> spSeatsPerRow;
    @FXML private ComboBox<RoomStatus> cbRoomStatus;
    @FXML private Button btnSaveRoom;
    @FXML private Button btnDeleteRoom;

    private final ObservableList<RoomRow> roomRows = FXCollections.observableArrayList();
    private Long editingRoomId;

    // ===== Tab Suất chiếu =====
    @FXML private DatePicker dpFilterDate;
    @FXML private ComboBox<MovieItem> cbFilterMovie;
    @FXML private ComboBox<RoomItem> cbFilterRoom;

    @FXML private TableView<ShowtimeRow> tableShowtimes;
    @FXML private TableColumn<ShowtimeRow, String> colShowMovie;
    @FXML private TableColumn<ShowtimeRow, String> colShowRoom;
    @FXML private TableColumn<ShowtimeRow, LocalDateTime> colShowStart;
    @FXML private TableColumn<ShowtimeRow, LocalDateTime> colShowEnd;
    @FXML private TableColumn<ShowtimeRow, BigDecimal> colShowPrice;
    @FXML private TableColumn<ShowtimeRow, String> colShowStatus;
    @FXML private Pagination paginationShowtimes;

    @FXML private VBox formBoxShowtime;
    @FXML private ComboBox<MovieItem> cbMovie;
    @FXML private ComboBox<RoomItem> cbRoom;
    @FXML private DatePicker dpDate;
    @FXML private Spinner<Integer> spHour;
    @FXML private Spinner<Integer> spMinute;
    @FXML private TextField tfPrice;
    @FXML private Label lblEnd;
    @FXML private Button btnSaveShowtime;
    @FXML private Button btnDeleteShowtime;

    private final ObservableList<ShowtimeRow> showtimeRows = FXCollections.observableArrayList();
    private Long editingShowtimeId;

    @FXML
    private void initialize() {
        initRoomTab();
        initShowtimeTab();
    }

    // ================= PHÒNG CHIẾU =================
    private void initRoomTab() {
        colRoomName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
        colRoomType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().type().getLabel()));
        colRows.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().rows()));
        colSeatsPerRow.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().seatsPerRow()));
        colCapacity.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().capacity()));
        colRoomStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().status() == RoomStatus.ACTIVE ? "Hoạt động" : "Bảo trì"));

        tableRooms.setItems(roomRows);
        tableRooms.getSelectionModel().selectedItemProperty().addListener((o, a, row) -> fillRoom(row));

        btnDeleteRoom.disableProperty().bind(tableRooms.getSelectionModel().selectedItemProperty().isNull());
        btnSaveRoom.disableProperty().bind(tfRoomName.textProperty().isEmpty());

        cbRoomType.getItems().setAll(RoomType.values());
        cbRoomType.setConverter(new StringConverter<>() {
            @Override public String toString(RoomType t) { return t == null ? "" : t.getLabel(); }
            @Override public RoomType fromString(String string) { return null; }
        });
        cbRoomType.setValue(RoomType.TWO_D);

        cbRoomStatus.getItems().setAll(RoomStatus.values());
        cbRoomStatus.setConverter(new StringConverter<>() {
            @Override public String toString(RoomStatus s) { return s == null ? "" : (s == RoomStatus.ACTIVE ? "Hoạt động" : "Bảo trì"); }
            @Override public RoomStatus fromString(String string) { return null; }
        });
        cbRoomStatus.setValue(RoomStatus.ACTIVE);

        spTotalRows.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 26, 10));
        spTotalRows.setEditable(true);
        spSeatsPerRow.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 10));
        spSeatsPerRow.setEditable(true);

        reloadRooms();
    }

    private void fillRoom(RoomRow row) {
        if (row == null) {
            onNewRoom();
            return;
        }
        editingRoomId = row.id();
        tfRoomName.setText(row.name());
        cbRoomType.setValue(row.type());
        spTotalRows.getValueFactory().setValue(row.rows());
        spSeatsPerRow.getValueFactory().setValue(row.seatsPerRow());
        cbRoomStatus.setValue(row.status());
    }

    private void reloadRooms() {
        fxAsync.run(facade::findAllRooms, roomRows::setAll);
    }

    @FXML
    private void onNewRoom() {
        editingRoomId = null;
        tfRoomName.clear();
        cbRoomType.setValue(RoomType.TWO_D);
        spTotalRows.getValueFactory().setValue(10);
        spSeatsPerRow.getValueFactory().setValue(10);
        cbRoomStatus.setValue(RoomStatus.ACTIVE);
        tableRooms.getSelectionModel().clearSelection();
    }

    @FXML
    private void onSaveRoom() {
        RoomForm form = new RoomForm();
        form.setRoomId(editingRoomId);
        form.setRoomName(tfRoomName.getText() == null ? "" : tfRoomName.getText().trim());
        form.setRoomType(cbRoomType.getValue());
        form.setTotalRows(spTotalRows.getValue());
        form.setSeatsPerRow(spSeatsPerRow.getValue());
        form.setStatus(cbRoomStatus.getValue());

        Map<String, Control> fields = new HashMap<>();
        fields.put("roomName", tfRoomName);
        fields.put("roomType", cbRoomType);
        fields.put("totalRows", spTotalRows);
        fields.put("seatsPerRow", spSeatsPerRow);
        fields.put("status", cbRoomStatus);

        if (!validator.mark(validator.validate(form), fields)) return;

        fxAsync.runVoid(() -> facade.saveRoom(form), () -> {
            AlertUtil.info("Đã lưu thông tin phòng chiếu");
            onNewRoom();
            reloadRooms();
            reloadShowtimeOptions();
        }, formBoxRoom);
    }

    @FXML
    private void onDeleteRoom() {
        RoomRow row = tableRooms.getSelectionModel().getSelectedItem();
        if (row == null) return;
        if (AlertUtil.confirm("Xoá phòng chiếu \"" + row.name() + "\"?")) {
            fxAsync.run(() -> facade.deleteRoom(row.id()), deleted -> {
                AlertUtil.info(deleted ? "Đã xoá phòng chiếu" : "Phòng đã có suất chiếu → Đã chuyển sang trạng thái BẢO TRÌ");
                onNewRoom();
                reloadRooms();
                reloadShowtimeOptions();
            }, formBoxRoom);
        }
    }

    // ================= SUẤT CHIẾU =================
    private void initShowtimeTab() {
        colShowMovie.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().movieTitle()));
        colShowRoom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().roomName()));

        colShowStart.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().start()));
        colShowStart.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.format(DATETIME_FMT));
            }
        });

        colShowEnd.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().end()));
        colShowEnd.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.format(DATETIME_FMT));
            }
        });

        colShowPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().price()));
        colShowPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : MoneyUtil.format(v));
            }
        });

        colShowStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().status() == ShowtimeStatus.OPEN ? "Mở bán" : "Đã huỷ"));

        tableShowtimes.setItems(showtimeRows);
        tableShowtimes.getSelectionModel().selectedItemProperty().addListener((o, a, row) -> fillShowtime(row));

        btnDeleteShowtime.disableProperty().bind(tableShowtimes.getSelectionModel().selectedItemProperty().isNull());

        // Gợi ý EndTime = Start + Duration + 15'
        spHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 19));
        spHour.setEditable(true);
        spMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        spMinute.setEditable(true);

        dpDate.setValue(LocalDate.now());
        dpDate.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || d.isBefore(LocalDate.now()));
            }
        });

        tfPrice.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,9}") ? c : null));
        tfPrice.setText("75000");

        lblEnd.textProperty().bind(Bindings.createStringBinding(() -> {
            LocalDateTime end = suggestedEnd();
            return end == null ? "" : "Kết thúc (gợi ý): " + end.format(DATETIME_FMT);
        }, cbMovie.valueProperty(), dpDate.valueProperty(), spHour.valueProperty(), spMinute.valueProperty()));

        // Filter events
        dpFilterDate.valueProperty().addListener((o, a, b) -> searchShowtimes());
        cbFilterMovie.valueProperty().addListener((o, a, b) -> searchShowtimes());
        cbFilterRoom.valueProperty().addListener((o, a, b) -> searchShowtimes());
        paginationShowtimes.currentPageIndexProperty().addListener((o, a, b) -> loadShowtimes(b.intValue()));

        reloadShowtimeOptions();
        loadShowtimes(0);
    }

    private LocalDateTime startTime() {
        if (dpDate.getValue() == null) return null;
        return LocalDateTime.of(dpDate.getValue(), LocalTime.of(spHour.getValue(), spMinute.getValue()));
    }

    private LocalDateTime suggestedEnd() {
        LocalDateTime start = startTime();
        if (start == null || cbMovie.getValue() == null) return null;
        return start.plusMinutes(cbMovie.getValue().duration() + 15L);
    }

    private void reloadShowtimeOptions() {
        fxAsync.run(() -> new Object[]{ facade.schedulableMovies(), facade.activeRooms() }, data -> {
            @SuppressWarnings("unchecked") List<MovieItem> movies = (List<MovieItem>) data[0];
            @SuppressWarnings("unchecked") List<RoomItem> rooms = (List<RoomItem>) data[1];

            cbMovie.getItems().setAll(movies);
            if (!movies.isEmpty() && cbMovie.getValue() == null) cbMovie.setValue(movies.get(0));

            cbRoom.getItems().setAll(rooms);
            if (!rooms.isEmpty() && cbRoom.getValue() == null) cbRoom.setValue(rooms.get(0));

            cbFilterMovie.getItems().setAll(Arrays.asList((MovieItem) null));
            cbFilterMovie.getItems().addAll(movies);

            cbFilterRoom.getItems().setAll(Arrays.asList((RoomItem) null));
            cbFilterRoom.getItems().addAll(rooms);
        });
    }

    private void searchShowtimes() {
        if (paginationShowtimes.getCurrentPageIndex() != 0) {
            paginationShowtimes.setCurrentPageIndex(0);
        } else {
            loadShowtimes(0);
        }
    }

    private void loadShowtimes(int page) {
        LocalDate date = dpFilterDate.getValue();
        Long movieId = cbFilterMovie.getValue() == null ? null : cbFilterMovie.getValue().id();
        Long roomId = cbFilterRoom.getValue() == null ? null : cbFilterRoom.getValue().id();

        fxAsync.run(() -> facade.searchShowtimes(movieId, roomId, date, page), result -> {
            showtimeRows.setAll(result.items());
            paginationShowtimes.setPageCount(Math.max(1, result.totalPages()));
        });
    }

    private void fillShowtime(ShowtimeRow row) {
        if (row == null) {
            onNewShowtime();
            return;
        }
        editingShowtimeId = row.id();
        for (MovieItem m : cbMovie.getItems()) {
            if (m.title().equals(row.movieTitle())) {
                cbMovie.setValue(m);
                break;
            }
        }
        for (RoomItem r : cbRoom.getItems()) {
            if (r.name().equals(row.roomName())) {
                cbRoom.setValue(r);
                break;
            }
        }
        dpDate.setValue(row.start().toLocalDate());
        spHour.getValueFactory().setValue(row.start().getHour());
        spMinute.getValueFactory().setValue(row.start().getMinute());
        tfPrice.setText(row.price() == null ? "75000" : row.price().toPlainString());
    }

    @FXML
    private void onNewShowtime() {
        editingShowtimeId = null;
        dpDate.setValue(LocalDate.now());
        spHour.getValueFactory().setValue(19);
        spMinute.getValueFactory().setValue(0);
        tfPrice.setText("75000");
        tableShowtimes.getSelectionModel().clearSelection();
    }

    @FXML
    private void onResetFilter() {
        dpFilterDate.setValue(null);
        cbFilterMovie.setValue(null);
        cbFilterRoom.setValue(null);
        searchShowtimes();
    }

    @FXML
    private void onSaveShowtime() {
        ShowtimeForm f = new ShowtimeForm();
        f.setShowtimeId(editingShowtimeId);
        f.setMovieId(cbMovie.getValue() == null ? null : cbMovie.getValue().id());
        f.setRoomId(cbRoom.getValue() == null ? null : cbRoom.getValue().id());
        f.setStartTime(startTime());
        f.setEndTime(suggestedEnd());
        try {
            f.setTicketPrice(tfPrice.getText().isBlank() ? null : new BigDecimal(tfPrice.getText().trim()));
        } catch (Exception e) {
            f.setTicketPrice(null);
        }

        Map<String, Control> fields = new HashMap<>();
        fields.put("movieId", cbMovie);
        fields.put("roomId", cbRoom);
        fields.put("startTime", dpDate);
        fields.put("ticketPrice", tfPrice);

        if (!validator.mark(validator.validate(f), fields)) return;

        fxAsync.runVoid(() -> facade.saveShowtime(f), () -> {
            AlertUtil.info("Đã lưu suất chiếu");
            onNewShowtime();
            loadShowtimes(paginationShowtimes.getCurrentPageIndex());
        }, formBoxShowtime);
    }

    @FXML
    private void onDeleteShowtime() {
        ShowtimeRow s = tableShowtimes.getSelectionModel().getSelectedItem();
        if (s == null) return;
        if (AlertUtil.confirm("Xoá suất chiếu phim \"" + s.movieTitle() + "\" lúc " + s.start().format(DATETIME_FMT) + "?")) {
            fxAsync.run(() -> facade.deleteShowtime(s.id()), deleted -> {
                AlertUtil.info(deleted ? "Đã xoá suất chiếu" : "Suất chiếu đã có đơn đặt → Đã huỷ suất và toàn bộ các đơn liên quan");
                onNewShowtime();
                loadShowtimes(paginationShowtimes.getCurrentPageIndex());
            }, formBoxShowtime);
        }
    }
}
