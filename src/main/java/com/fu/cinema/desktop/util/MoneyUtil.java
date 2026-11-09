package com.fu.cinema.desktop.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtil {

    private static final NumberFormat VND = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));

    private MoneyUtil() {
    }

    public static String format(BigDecimal v) {
        return v == null ? "" : VND.format(v) + " đ";
    }

    public static String format(long v) {
        return VND.format(v) + " đ";
    }
}