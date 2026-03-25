package ie.tus.oop2.restaurant.model.payment;

import java.math.BigDecimal;

public sealed interface PaymentValidationContext
        permits CashCtx, CardCtx, VoucherCtx {

    BigDecimal paid();
    BigDecimal total();
}