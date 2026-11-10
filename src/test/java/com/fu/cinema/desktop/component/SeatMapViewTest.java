package com.fu.cinema.desktop.component;

import com.fu.cinema.desktop.model.SeatMapData;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SeatMapViewTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Toolkit đã được khởi tạo trước đó
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private void runOnFxThread(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout running on FX thread");
        if (error[0] != null) {
            if (error[0] instanceof AssertionError ae) throw ae;
            throw new RuntimeException(error[0]);
        }
    }

    @Test
    @DisplayName("AC1: Phòng 8x12 dựng đủ 96 ghế + nhãn hàng, ghế đã bán bị disable")
    void testRenderSeatMap_8x12() throws Exception {
        runOnFxThread(() -> {
            SeatMapView seatMapView = new SeatMapView();
            List<String> rows = List.of("A", "B", "C", "D", "E", "F", "G", "H"); // 8 hàng
            int seatsPerRow = 12; // 12 ghế/hàng -> tổng 96 ghế
            Set<String> soldSeats = Set.of("A1", "A2", "B5", "H12");

            SeatMapData data = new SeatMapData(
                    1L,
                    "Phim Test",
                    "Phòng 1",
                    LocalDateTime.now().plusDays(1),
                    BigDecimal.valueOf(80000),
                    rows,
                    seatsPerRow,
                    soldSeats
            );

            seatMapView.setData(data);

            // Tìm GridPane trong children của SeatMapView
            GridPane grid = null;
            for (Node child : seatMapView.getChildren()) {
                if (child instanceof GridPane gp) {
                    grid = gp;
                    break;
                }
            }
            assertNotNull(grid, "GridPane sơ đồ ghế phải được tạo");

            int toggleButtonCount = 0;
            int labelCount = 0;
            int disabledCount = 0;

            for (Node node : grid.getChildren()) {
                if (node instanceof ToggleButton tb) {
                    toggleButtonCount++;
                    if (tb.isDisable()) {
                        disabledCount++;
                    }
                } else if (node instanceof Label) {
                    labelCount++;
                }
            }

            assertEquals(96, toggleButtonCount, "Phòng 8x12 phải tạo đúng 96 ToggleButton ghế");
            assertEquals(8, labelCount, "Phải có 8 Label cho 8 hàng A->H");
            assertEquals(4, disabledCount, "4 ghế đã bán phải bị disable");
        });
    }

    @Test
    @DisplayName("AC2 & AC3 & AC4: Giới hạn tối đa 8 ghế, danh sách getSelectedSeats cập nhật")
    void testMaxSeatsSelection() throws Exception {
        runOnFxThread(() -> {
            SeatMapView seatMapView = new SeatMapView();
            List<String> rows = List.of("A", "B");
            int seatsPerRow = 10;
            SeatMapData data = new SeatMapData(
                    1L, "Phim", "Phòng", LocalDateTime.now(), BigDecimal.valueOf(50000),
                    rows, seatsPerRow, Set.of()
            );

            seatMapView.setData(data);

            GridPane grid = (GridPane) seatMapView.getChildren().stream()
                    .filter(n -> n instanceof GridPane).findFirst().orElseThrow();

            List<ToggleButton> seatButtons = new ArrayList<>();
            for (Node node : grid.getChildren()) {
                if (node instanceof ToggleButton tb) {
                    seatButtons.add(tb);
                }
            }

            // Chọn 8 ghế đầu tiên
            for (int i = 0; i < 8; i++) {
                seatButtons.get(i).setSelected(true);
            }

            assertEquals(8, seatMapView.getSelectedSeats().size(), "Đã chọn đủ 8 ghế");

            // Chọn ghế thứ 9
            seatButtons.get(8).setSelected(true);

            // Ghế thứ 9 phải bị huỷ chọn và danh sách vẫn chỉ là 8
            assertFalse(seatButtons.get(8).isSelected(), "Ghế thứ 9 phải tự bỏ chọn");
            assertEquals(8, seatMapView.getSelectedSeats().size(), "Danh sách ghế vẫn chỉ tối đa 8 ghế");
        });
    }
}

