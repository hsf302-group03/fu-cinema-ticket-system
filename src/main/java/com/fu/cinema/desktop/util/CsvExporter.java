package com.fu.cinema.desktop.util;

import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.model.ReportData;
import com.fu.cinema.desktop.model.RevenueRow;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/**
 * Tiện ích xuất báo cáo doanh thu ra file CSV chuẩn UTF-8 có BOM (TODO 45).
 * Đảm bảo mở trực tiếp bằng Microsoft Excel hiển thị đúng tiếng Việt có dấu.
 */
public final class CsvExporter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private CsvExporter() {
    }

    public static void write(Path path, ReportData d) {
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            // Ghi BOM để Excel đọc đúng tiếng Việt UTF-8
            w.write('\ufeff');

            // 1. Tiêu đề kỳ báo cáo
            w.write("BÁO CÁO DOANH THU RẠP PHIM FU CINEMA");
            w.newLine();
            w.write("Từ ngày," + d.start() + ",Đến ngày," + d.end());
            w.newLine();
            w.newLine();

            // 2. Bảng doanh thu theo từng phim
            w.write("--- DOANH THU THEO PHIM ---");
            w.newLine();
            w.write("Tên phim,Số vé bán được,Doanh thu (VNĐ)");
            w.newLine();
            for (RevenueRow r : d.revenue()) {
                w.write(String.join(",",
                        escapeCsv(r.title()),
                        String.valueOf(r.tickets()),
                        r.revenue() != null ? r.revenue().toPlainString() : "0"
                ));
                w.newLine();
            }
            w.newLine();

            // 3. Bảng danh sách các đơn đặt vé CONFIRMED
            w.write("--- DANH SÁCH ĐƠN ĐẶT VÉ (CONFIRMED) ---");
            w.newLine();
            w.write("Mã đơn,Thời gian đặt,Khách hàng,Phim,Tổng tiền (VNĐ)");
            w.newLine();
            for (ReportBookingRow b : d.bookings()) {
                w.write(String.join(",",
                        "#" + b.id(),
                        b.bookingDate() != null ? escapeCsv(b.bookingDate().format(DATE_TIME_FORMATTER)) : "",
                        escapeCsv(b.customerName()),
                        escapeCsv(b.movieTitle()),
                        b.total() != null ? b.total().toPlainString() : "0"
                ));
                w.newLine();
            }
            w.newLine();

            // 4. Dòng tổng kết
            w.write("TỔNG CỘNG,%d đơn,%d vé,,%s".formatted(
                    d.totalBookings(),
                    d.totalTickets(),
                    d.totalRevenue() != null ? d.totalRevenue().toPlainString() : "0"
            ));
            w.newLine();
        } catch (IOException e) {
            throw new UncheckedIOException("Lỗi khi ghi file CSV: " + path, e);
        }
    }

    private static String escapeCsv(String v) {
        if (v == null) {
            return "";
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}

