package ie.tus.oop2.restaurant.service;

import java.math.BigDecimal;

/**
 * Sealed validation context for payment rules.
 * Demonstrates:
 * - sealed interface
 * - records
 */
public sealed interface PaymentValidationContext
        permits CashCtx, CardCtx, VoucherCtx {

    BigDecimal paid();
    BigDecimal total();
}

record CashCtx(BigDecimal paid, BigDecimal total) implements PaymentValidationContext {}

record CardCtx(BigDecimal paid, BigDecimal total, String cardLast4) implements PaymentValidationContext {}

record VoucherCtx(BigDecimal paid, BigDecimal total, String voucherCode) implements PaymentValidationContext {}