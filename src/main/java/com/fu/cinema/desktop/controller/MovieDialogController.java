package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.model.GenreItem;
import com.fu.cinema.desktop.util.FxValidator;
import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.enums.AgeRating;
import com.fu.cinema.enums.MovieStatus;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MovieDialogController {

    private final FxValidator validator;

    @FXML private GridPane root;
    @FXML private TextField tfTitle;
    @FXML private TextField tfDirector;
    @FXML private TextField tfPoster;
    @FXML private Spinner<Integer> spDuration;
    @FXML private DatePicker dpRelease;
    @FXML private ComboBox<AgeRating> cbAgeRating;
    @FXML private ComboBox<GenreItem> cbGenre;
    @FXML private ComboBox<MovieStatus> cbStatus;
    @FXML private TextArea taDescription;

    private Long movieId;

    @FXML
    private void initialize() {
        spDuration.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 120));
        spDuration.setEditable(true);
        spDuration.focusedProperty().addListener((o, was, is) -> {
            if (!is) commitSpinner();
        });

        cbAgeRating.getItems().setAll(AgeRating.values());
        cbAgeRating.setConverter(new StringConverter<>() {
            @Override public String toString(AgeRating a) { return a == null ? "" : a.name() + " (" + a.getMinAge() + "+)"; }
            @Override public AgeRating fromString(String string) { return null; }
        });

        cbStatus.getItems().setAll(MovieStatus.values());
        cbStatus.setConverter(new StringConverter<>() {
            @Override public String toString(MovieStatus s) { return MovieController.formatMovieStatus(s); }
            @Override public MovieStatus fromString(String string) { return null; }
        });

        cbGenre.setConverter(new StringConverter<>() {
            @Override public String toString(GenreItem g) { return g == null ? "" : g.name(); }
            @Override public GenreItem fromString(String string) { return null; }
        });
    }

    public Optional<MovieForm> open(Window owner, MovieForm form, List<GenreItem> genres) {
        cbGenre.getItems().setAll(genres);
        fill(form);

        Dialog<MovieForm> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(form.getMovieId() == null ? "Thêm phim mới" : "Chỉnh sửa phim");
        dialog.getDialogPane().setContent(root);

        ButtonType save = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        // Chặn đóng dialog bằng event filter khi form còn lỗi validate
        Button btnSaveNode = (Button) dialog.getDialogPane().lookupButton(save);
        btnSaveNode.addEventFilter(ActionEvent.ACTION, e -> {
            if (!validate()) {
                e.consume();
            }
        });

        dialog.setResultConverter(bt -> bt == save ? toForm() : null);
        return dialog.showAndWait();
    }

    private boolean validate() {
        commitSpinner();
        MovieForm f = toForm();
        Map<String, Control> fields = new HashMap<>();
        fields.put("title", tfTitle);
        fields.put("director", tfDirector);
        fields.put("duration", spDuration);
        fields.put("releaseDate", dpRelease);
        fields.put("ageRating", cbAgeRating);
        fields.put("genreId", cbGenre);
        fields.put("status", cbStatus);
        fields.put("posterUrl", tfPoster);
        fields.put("description", taDescription);
        return validator.mark(validator.validate(f), fields);
    }

    private MovieForm toForm() {
        MovieForm f = new MovieForm();
        f.setMovieId(movieId);
        f.setTitle(tfTitle.getText() == null ? "" : tfTitle.getText().trim());
        f.setDirector(tfDirector.getText() == null ? "" : tfDirector.getText().trim());
        f.setDuration(spDuration.getValue());
        f.setReleaseDate(dpRelease.getValue());
        f.setAgeRating(cbAgeRating.getValue());
        f.setGenreId(cbGenre.getValue() == null ? null : cbGenre.getValue().id());
        f.setStatus(cbStatus.getValue());
        f.setPosterUrl(tfPoster.getText() == null ? "" : tfPoster.getText().trim());
        f.setDescription(taDescription.getText() == null ? "" : taDescription.getText().trim());
        return f;
    }

    private void fill(MovieForm f) {
        movieId = f.getMovieId();
        tfTitle.setText(f.getTitle() == null ? "" : f.getTitle());
        tfDirector.setText(f.getDirector() == null ? "" : f.getDirector());
        if (f.getDuration() != null && f.getDuration() > 0) {
            spDuration.getValueFactory().setValue(f.getDuration());
        } else {
            spDuration.getValueFactory().setValue(120);
        }
        dpRelease.setValue(f.getReleaseDate());
        cbAgeRating.setValue(f.getAgeRating() == null ? AgeRating.P : f.getAgeRating());
        cbStatus.setValue(f.getStatus() == null ? MovieStatus.COMING_SOON : f.getStatus());
        tfPoster.setText(f.getPosterUrl() == null ? "" : f.getPosterUrl());
        taDescription.setText(f.getDescription() == null ? "" : f.getDescription());

        if (f.getGenreId() != null) {
            for (GenreItem item : cbGenre.getItems()) {
                if (item.id().equals(f.getGenreId())) {
                    cbGenre.setValue(item);
                    break;
                }
            }
        } else if (!cbGenre.getItems().isEmpty()) {
            cbGenre.setValue(cbGenre.getItems().get(0));
        }
    }

    private void commitSpinner() {
        try {
            spDuration.getValueFactory().setValue(Integer.parseInt(spDuration.getEditor().getText().trim()));
        } catch (Exception ignored) {
            spDuration.getEditor().setText(String.valueOf(spDuration.getValue()));
        }
    }
}
