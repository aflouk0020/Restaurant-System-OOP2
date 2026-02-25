package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.model.PaymentType;
import ie.tus.oop2.restaurant.model.payment.PaymentValidator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentValidatorTest {

    private static final BigDecimal TOTAL = new BigDecimal("28.25");

    @Test
    void cash_ok_whenPaidEqualsTotal() {
        assertDoesNotThrow(() ->
                PaymentValidator.validate(PaymentType.CASH, TOTAL, TOTAL, null, null)
        );
    }

    @Test
    void cash_shouldFailWhenMismatch() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                PaymentValidator.validate(PaymentType.CASH, new BigDecimal("10.00"), TOTAL, null, null)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("mismatch"));
    }

    @Test
    void card_ok_whenLast4Present_andPaidMatches() {
        assertDoesNotThrow(() ->
                PaymentValidator.validate(PaymentType.CARD, TOTAL, TOTAL, "1234", null)
        );
    }

    @Test
    void card_shouldFailWhenLast4Missing() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                PaymentValidator.validate(PaymentType.CARD, TOTAL, TOTAL, null, null)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("last 4"));
    }

    @Test
    void card_shouldFailWhenMismatch() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                PaymentValidator.validate(PaymentType.CARD, new BigDecimal("28.24"), TOTAL, "1234", null)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("mismatch"));
    }

    @Test
    void voucher_ok_whenCodePresent_andPaidMatches() {
        assertDoesNotThrow(() ->
                PaymentValidator.validate(PaymentType.VOUCHER, TOTAL, TOTAL, null, "VCH-001")
        );
    }

    @Test
    void voucher_shouldFailWhenCodeMissing() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                PaymentValidator.validate(PaymentType.VOUCHER, TOTAL, TOTAL, null, "   ")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("voucher"));
    }

    @Test
    void voucher_shouldFailWhenMismatch() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                PaymentValidator.validate(PaymentType.VOUCHER, new BigDecimal("1.00"), TOTAL, null, "VCH-001")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("mismatch"));
    }
}