package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.component.SeatMapView;
import com.fu.cinema.desktop.facade.CounterSaleFacade;
import com.fu.cinema.desktop.model.CustomerItem;
import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.ShowtimeItem;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.MoneyUtil;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller màn hình bán vé tại quầy (TODO 43).
 * Spring bean prototype, constructor injection, UI bind theo Reactive/Observable.
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class CounterSaleController {

    private final CounterSaleFacade facade;
    private final FxAsync fxAsync;

    @FXML private TextField tfCustomer;
    @FXML private ListView<CustomerItem> lvCustomers;
    @FXML private ComboBox<MovieItem> cbMovie;
    @FXML private ComboBox<ShowtimeItem> cbShowtime;
    @FXML private SeatMapView seatMap;
    @FXML private Label lblSeats;
    @FXML private Label lblTotal;
    @FXML private CheckBox chkPaid;
    @FXML private Button btnSell;

    private final ObjectProperty<BigDecimal> price = new SimpleObjectProperty<>();
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));

    @FXML
    private void initialize() {
        // (1) Tìm & chọn khách hàng (debounce 300ms)
        debounce.setOnFinished(e -> fxAsync.run(
                () -> facade.searchCustomers(tfCustomer.getText().trim()),
                lvCustomers.getItems()::setAll
        ));
        tfCustomer.textProperty().addListener((o, a, b) -> debounce.playFromStart());

        // (2) Chuỗi phụ thuộc phim -> suất chiếu -> sơ đồ ghế
        cbMovie.valueProperty().addListener((o, a, movie) -> {
            cbShowtime.getItems().clear();
            clearSeatMap();
            if (movie != null) {
                fxAsync.run(() -> facade.showtimes(movie.id()), cbShowtime.getItems()::setAll);
            }
        });

        cbShowtime.valueProperty().addListener((o, a, st) -> {
            clearSeatMap();
            if (st != null) {
                reloadSeatMap();
            }
        });

        // (3) Tổng tiền = giá vé x số ghế chọn (binding)
        ObjectBinding<BigDecimal> total = Bindings.createObjectBinding(
                () -> price.get() == null
                        ? BigDecimal.ZERO
                        : price.get().multiply(BigDecimal.valueOf(seatMap.getSelectedSeats().size())),
                price, seatMap.getSelectedSeats()
        );

        lblTotal.textProperty().bind(Bindings.createStringBinding(
                () -> "Tổng: " + MoneyUtil.format(total.get()), total
        ));

        lblSeats.textProperty().bind(Bindings.createStringBinding(
                () -> "Ghế: " + (seatMap.getSelectedSeats().isEmpty()
                        ? "Chưa chọn ghế"
                        : String.join(", ", seatMap.getSelectedSeats().stream().sorted(CounterSaleFacade.SEAT_ORDER).toList())),
                seatMap.getSelectedSeats()
        ));

        // (4) Nút bán chỉ bật khi đủ điều kiện (khách + suất + >=1 ghế + không bận)
        btnSell.disableProperty().bind(
                lvCustomers.getSelectionModel().selectedItemProperty().isNull()
                        .or(cbShowtime.valueProperty().isNull())
                        .or(Bindings.isEmpty(seatMap.getSelectedSeats()))
                        .or(busy)
        );

        // Tải dữ liệu ban đầu
        fxAsync.run(facade::movies, cbMovie.getItems()::setAll);
        fxAsync.run(() -> facade.searchCustomers(""), lvCustomers.getItems()::setAll);
    }

    @FXML
    private void onSell() {
        CustomerItem customer = lvCustomers.getSelectionModel().getSelectedItem();
        ShowtimeItem showtime = cbShowtime.getValue();
        List<String> seats = List.copyOf(seatMap.getSelectedSeats());
        boolean paid = chkPaid.isSelected();

        if (customer == null || showtime == null || seats.isEmpty()) {
            return;
        }

        if (!AlertUtil.confirm("Bán %d vé (%s) cho %s?".formatted(
                seats.size(), String.join(", ", seats), customer.fullName()))) {
            return;
        }

        busy.set(true);
        fxAsync.run(
                () -> facade.sell(customer.id(), showtime.id(), seats, paid),
                result -> {
                    busy.set(false);
                    AlertUtil.info("Bán vé thành công!\nĐơn #%d · Ghế %s · %s · Trạng thái: %s".formatted(
                            result.bookingId(),
                            String.join(", ", result.seats()),
                            MoneyUtil.format(result.total()),
                            result.status()
                    ));
                    reloadSeatMap();
                },
                ex -> {
                    busy.set(false);
                    AlertUtil.showError(ex); // Thông báo lỗi (ví dụ BR-11, tranh chấp ghế...)
                    reloadSeatMap();         // Tải lại sơ đồ ghế để cập nhật ghế vừa bị người khác mua
                },
                null
        );
    }

    private void reloadSeatMap() {
        ShowtimeItem st = cbShowtime.getValue();
        if (st == null) {
            return;
        }
        fxAsync.run(() -> facade.seatMap(st.id()), data -> {
            price.set(data.price());
            seatMap.setData(data);
        });
    }

    private void clearSeatMap() {
        price.set(null);
        seatMap.setData(null);
    }
}

