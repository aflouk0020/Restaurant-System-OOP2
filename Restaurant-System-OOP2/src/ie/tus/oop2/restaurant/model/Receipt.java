package ie.tus.oop2.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Receipt(
        long receiptId,
        long orderId,
        long paymentId,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        LocalDateTime generatedAt
) {}
