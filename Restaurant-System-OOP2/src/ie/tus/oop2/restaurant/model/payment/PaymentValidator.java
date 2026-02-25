package ie.tus.oop2.restaurant.model.payment;

import ie.tus.oop2.restaurant.model.PaymentType;
import ie.tus.oop2.restaurant.model.payment.*;

import java.math.BigDecimal;
import java.util.Objects;

public final class PaymentValidator {

    private PaymentValidator() { }

    public static void validate(PaymentType type,
                                BigDecimal paid,
                                BigDecimal total,
                                String cardLast4,
                                String voucherCode) {

        Objects.requireNonNull(type, "payment type");
        Objects.requireNonNull(paid, "paid");
        Objects.requireNonNull(total, "total");

        // ✅ switch expression on enum -> creates a sealed-context instance
        PaymentValidationContext ctx = switch (type) {
            case CASH -> new CashCtx(paid, total);
            case CARD -> new CardCtx(paid, total, cardLast4);
            case VOUCHER -> new VoucherCtx(paid, total, voucherCode);
        };

        // ✅ pattern matching switch on sealed interface
        switch (ctx) {
            case CashCtx c -> requireExactPaid(c.paid(), c.total(), "Cash");

            case CardCtx c -> {
                requireNotBlank(c.cardLast4(), "Card payment requires last 4 digits.");
                requireExactPaid(c.paid(), c.total(), "Card");
            }

            case VoucherCtx c -> {
                requireNotBlank(c.voucherCode(), "Voucher payment requires voucher code.");
                requireExactPaid(c.paid(), c.total(), "Voucher");
            }
        }
    }

    private static void requireExactPaid(BigDecimal paid, BigDecimal total, String label) {
        if (paid.compareTo(total) != 0) {
            throw new IllegalStateException(
                    label + " payment amount mismatch. paid=" + paid + " total=" + total
            );
        }
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}