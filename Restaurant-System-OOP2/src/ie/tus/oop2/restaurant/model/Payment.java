package ie.tus.oop2.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps to MySQL table: payment
 *
 * Columns:
 *  payment_id, order_id, paid_at, payment_type, amount, currency,
 *  card_last4, voucher_code
 */
public record Payment(
        long paymentId,
        long orderId,
        LocalDateTime paidAt,
        PaymentType paymentType,
        BigDecimal amount,
        String currency,
        String cardLast4,     // nullable (only for CARD)
        String voucherCode    // nullable (only for VOUCHER)
) {}