package ie.tus.oop2.restaurant.model.payment;

import java.math.BigDecimal;

public record CashCtx(BigDecimal paid, BigDecimal total) implements PaymentValidationContext { }