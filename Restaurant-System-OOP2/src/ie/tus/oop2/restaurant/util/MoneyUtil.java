package ie.tus.oop2.restaurant.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtil {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private static final NumberFormat CURRENCY_FORMAT =
            NumberFormat.getCurrencyInstance(Locale.UK); // € / £ depending on locale

    private MoneyUtil() { }

    // ----------------------------
    // Core operations
    // ----------------------------

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal scale(BigDecimal value) {
        if (value == null) return zero();
        return value.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return scale(nullSafe(a).add(nullSafe(b)));
    }

    public static BigDecimal divide(BigDecimal numerator, long denominator) {
        if (denominator <= 0) return zero();
        return numerator.divide(BigDecimal.valueOf(denominator), SCALE, ROUNDING);
    }

    public static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // ----------------------------
    // UI / Display helpers (VERY IMPORTANT)
    // ----------------------------

    public static String format(BigDecimal value) {
        return CURRENCY_FORMAT.format(scale(value));
    }

    public static String formatPlain(BigDecimal value) {
        return scale(value).toPlainString();
    }
}