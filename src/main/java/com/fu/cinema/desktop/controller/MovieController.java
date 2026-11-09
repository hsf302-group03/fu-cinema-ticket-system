package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.MovieFacade;
import com.fu.cinema.desktop.model.GenreItem;
import com.fu.cinema.desktop.model.MovieRow;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.enums.AgeRating;
import com.fu.cinema.enums.MovieStatus;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MovieController {

    private static final PseudoClass STOPPED = PseudoClass.getPseudoClass("stopped");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MovieFacade facade;
    private final FxAsync fxAsync;

    @FXML private TextField tfSearch;
    @FXML private ComboBox<GenreItem> cbGenre;
    @FXML private ComboBox<MovieStatus> cbStatus;
    @FXML private ComboBox<String> cbSort;
    @FXML private Label lblCount;
    @FXML private TableView<MovieRow> table;
    @FXML private TableColumn<MovieRow, String> colTitle;
    @FXML private TableColumn<MovieRow, String> colGenre;
    @FXML private TableColumn<MovieRow, Integer> colDuration;
    @FXML private TableColumn<MovieRow, LocalDate> colRelease;
    @FXML private TableColumn<MovieRow, AgeRating> colAge;
    @FXML private TableColumn<MovieRow, MovieStatus> colStatus;
    @FXML private Pagination pagination;

    private final ObservableList<MovieRow> rows = FXCollections.observableArrayList();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));

    public static String formatMovieStatus(MovieStatus s) {
        if (s == null) return "Tất cả trạng thái";
        return switch (s) {
            case COMING_SOON -> "Sắp chiếu";
            case SHOWING -> "Đang chiếu";
            case STOPPED -> "Ngừng chiếu";
        };
    }

    @FXML
    private void initialize() {
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().title()));
        colGenre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().genreName()));

        colDuration.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().duration()));
        colDuration.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v + " phút");
            }
        });

        colRelease.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().releaseDate()));
        colRelease.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.format(DATE_FMT));
            }
        });

        colAge.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().ageRating()));
        colAge.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(AgeRating v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.name());
            }
        });

        colStatus.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().status()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(MovieStatus v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : formatMovieStatus(v));
            }
        });

        table.setRowFactory(tv -> {
            TableRow<MovieRow> row = new TableRow<>() {
                @Override
                protected void updateItem(MovieRow m, boolean empty) {
                    super.updateItem(m, empty);
                    pseudoClassStateChanged(STOPPED, !empty && m != null && m.status() == MovieStatus.STOPPED);
                }
            };

            ContextMenu menu = new ContextMenu();
            MenuItem itemEdit = new MenuItem("Sửa phim");
            itemEdit.setOnAction(e -> {
                if (!row.isEmpty()) openDialog(row.getItem().id());
            });
            MenuItem itemDelete = new MenuItem("Xoá / Dừng chiếu");
            itemDelete.setOnAction(e -> {
                if (!row.isEmpty()) onDeleteItem(row.getItem());
            });
            menu.getItems().addAll(itemEdit, itemDelete);

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(menu)
            );

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openDialog(row.getItem().id());
                }
            });
            return row;
        });

        table.setItems(rows);

        cbStatus.setConverter(new StringConverter<>() {
            @Override public String toString(MovieStatus s) { return formatMovieStatus(s); }
            @Override public MovieStatus fromString(String string) { return null; }
        });
        cbStatus.getItems().setAll(Arrays.asList((MovieStatus) null));
        cbStatus.getItems().addAll(MovieStatus.values());
        cbStatus.setValue(null);

        cbGenre.setConverter(new StringConverter<>() {
            @Override public String toString(GenreItem g) { return g == null ? "Tất cả thể loại" : g.name(); }
            @Override public GenreItem fromString(String string) { return null; }
        });

        cbSort.setConverter(new StringConverter<>() {
            @Override public String toString(String s) {
                if ("title".equals(s)) return "Tên phim";
                return "Ngày khởi chiếu";
            }
            @Override public String fromString(String string) { return null; }
        });
        cbSort.getItems().setAll("releaseDate", "title");
        cbSort.setValue("releaseDate");

        debounce.setOnFinished(e -> search());
        tfSearch.textProperty().addListener((o, a, b) -> debounce.playFromStart());
        cbGenre.valueProperty().addListener((o, a, b) -> search());
        cbStatus.valueProperty().addListener((o, a, b) -> search());
        cbSort.valueProperty().addListener((o, a, b) -> search());
        pagination.currentPageIndexProperty().addListener((o, a, b) -> load(b.intValue()));

        fxAsync.run(facade::genres, g -> {
            cbGenre.getItems().setAll(Arrays.asList((GenreItem) null));
            cbGenre.getItems().addAll(g);
            cbGenre.setValue(null);
            load(0);
        });
    }

    private void search() {
        if (pagination.getCurrentPageIndex() != 0) {
            pagination.setCurrentPageIndex(0);
        } else {
            load(0);
        }
    }

    private void load(int page) {
        String kw = tfSearch.getText() == null ? null : tfSearch.getText().trim();
        Long genreId = cbGenre.getValue() == null ? null : cbGenre.getValue().id();
        MovieStatus status = cbStatus.getValue();
        String sort = cbSort.getValue() == null ? "releaseDate" : cbSort.getValue();

        fxAsync.run(() -> facade.search(kw, genreId, status, sort, page), result -> {
            rows.setAll(result.items());
            pagination.setPageCount(Math.max(1, result.totalPages()));
            lblCount.setText(result.totalElements() + " phim");
        });
    }

    @FXML
    private void onAdd() {
        openDialog(null);
    }

    protected void openDialog(Long movieId) {
        AlertUtil.info("Mở dialog phim (TODO 40)");
    }

    protected void onDeleteItem(MovieRow m) {
        if (m == null || !AlertUtil.confirm("Xoá phim \"" + m.title() + "\"?")) return;
        fxAsync.run(() -> facade.delete(m.id()), deleted -> {
            AlertUtil.info(deleted ? "Đã xoá phim" : "Phim đã có suất chiếu → Đã chuyển sang STOPPED");
            load(pagination.getCurrentPageIndex());
        });
    }
}
