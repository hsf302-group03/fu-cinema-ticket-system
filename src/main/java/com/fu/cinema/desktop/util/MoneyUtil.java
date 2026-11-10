package com.fu.cinema.desktop.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Tiện ích định dạng tiền tệ VNĐ (TODO 35).
 */
public final class MoneyUtil {

    private static final NumberFormat VND = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));

    private MoneyUtil() {
    }

    public static String format(BigDecimal v) {
        return v == null ? "0 đ" : VND.format(v) + " đ";
    }
}

