package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.model.payment.CardCtx;
import ie.tus.oop2.restaurant.model.payment.CashCtx;
import ie.tus.oop2.restaurant.model.payment.PaymentValidator;
import ie.tus.oop2.restaurant.model.payment.VoucherCtx;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentValidatorTest {

    private static final BigDecimal TOTAL = new BigDecimal("28.25");

    @Test
    void cash_ok_whenPaidEqualsTotal() {
        assertDoesNotThrow(() ->
                PaymentValidator.validate(
                        new CashCtx(TOTAL, TOTAL)
                )
        );
    }

    @Test
    void cash_shouldFail_whenMismatch() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                PaymentValidator.validate(
                        new CashCtx(new BigDecimal("20.00"), TOTAL)
                )
        );

        assertTrue(ex.getMessage().toLowerCase().contains("cash"));
    }

    @Test
    void card_ok_whenLast4Present() {
        assertDoesNotThrow(() ->
                PaymentValidator.validate(
                        new CardCtx(TOTAL, TOTAL, "1234")
                )
        );
    }

    @Test
    void card_shouldFail_whenLast4Missing() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                PaymentValidator.validate(
                        new CardCtx(TOTAL, TOTAL, null)
                )
        );

        assertTrue(ex.getMessage().toLowerCase().contains("last"));
    }

    @Test
    void voucher_ok_whenCodePresent() {
        assertDoesNotThrow(() ->
                PaymentValidator.validate(
                        new VoucherCtx(TOTAL, TOTAL, "VOUCH10")
                )
        );
    }

    @Test
    void voucher_shouldFail_whenCodeMissing() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                PaymentValidator.validate(
                        new VoucherCtx(TOTAL, TOTAL, null)
                )
        );

        assertTrue(ex.getMessage().toLowerCase().contains("voucher"));
    }
}