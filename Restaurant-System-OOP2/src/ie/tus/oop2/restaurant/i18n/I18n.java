package ie.tus.oop2.restaurant.i18n;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;
import java.util.ResourceBundle;

public final class I18n {

    private static Locale locale = Locale.UK; // default
    private static ResourceBundle bundle = ResourceBundle.getBundle("ie.tus.oop2.restaurant.i18n.messages", locale);

    private I18n() {}

    public static void setLocale(Locale newLocale) {
        locale = newLocale;
        bundle = ResourceBundle.getBundle("ie.tus.oop2.restaurant.i18n.messages", locale);
    }

    public static Locale getLocale() {
        return locale;
    }

    public static String t(String key) {
        return bundle.getString(key);
    }

    public static String money(java.math.BigDecimal value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
        nf.setCurrency(Currency.getInstance(t("money.currency")));
        return nf.format(value);
    }

    public static String date(LocalDate date) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(t("date.format"), locale);
        return date.format(fmt);
    }
}