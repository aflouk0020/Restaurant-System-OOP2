package ie.tus.oop2.restaurant.model.payment;

import java.math.BigDecimal;

public record VoucherCtx(BigDecimal paid, BigDecimal total, String voucherCode)
        implements PaymentValidationContext { }