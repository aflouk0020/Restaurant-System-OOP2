package ie.tus.oop2.restaurant.model.payment;

import java.math.BigDecimal;

public final class PaymentValidator {

    private PaymentValidator() { }

    public static void validate(PaymentValidationContext ctx) {

        switch (ctx) {

            case CashCtx c -> {
                if (c.paid().compareTo(c.total()) != 0) {
                    throw new IllegalStateException(
                            "Cash payment amount mismatch. paid=" + c.paid() + " total=" + c.total()
                    );
                }
            }

            case CardCtx c -> {
                if (c.cardLast4() == null || c.cardLast4().isBlank()) {
                    throw new IllegalStateException("Card payment requires last 4 digits.");
                }
                if (c.paid().compareTo(c.total()) != 0) {
                    throw new IllegalStateException(
                            "Card payment amount mismatch. paid=" + c.paid() + " total=" + c.total()
                    );
                }
            }

            case VoucherCtx c -> {
                if (c.voucherCode() == null || c.voucherCode().isBlank()) {
                    throw new IllegalStateException("Voucher payment requires voucher code.");
                }
                if (c.paid().compareTo(c.total()) != 0) {
                    throw new IllegalStateException(
                            "Voucher payment amount mismatch. paid=" + c.paid() + " total=" + c.total()
                    );
                }
            }
        }
    }
}