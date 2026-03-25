package ie.tus.oop2.restaurant.model.payment;

import java.math.BigDecimal;

public record CardCtx(BigDecimal paid, BigDecimal total, String cardLast4)
        implements PaymentValidationContext { }