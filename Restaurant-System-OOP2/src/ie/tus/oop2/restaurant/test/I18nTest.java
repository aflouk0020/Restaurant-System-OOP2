package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.i18n.I18n;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class I18nTest {

    @Test
    void shouldSwitchLocaleAndTranslate() {
        I18n.setLocale(Locale.UK);
        assertEquals("Restaurant System", I18n.t("app.title"));

        I18n.setLocale(Locale.FRANCE);
        assertTrue(I18n.t("app.title").contains("Restaurant") || I18n.t("app.title").contains("Syst"));
    }

    @Test
    void shouldFormatMoneyByLocale() {
        I18n.setLocale(Locale.UK);
        String uk = I18n.money(new BigDecimal("12.50"));
        assertNotNull(uk);

        I18n.setLocale(Locale.FRANCE);
        String fr = I18n.money(new BigDecimal("12.50"));
        assertNotNull(fr);

        assertNotEquals(uk, fr); // format differs by locale
    }
}