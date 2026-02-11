package ie.tus.oop2.restaurant.model;

import java.math.BigDecimal;

public record OrderLine(
        long orderLineId,
        long orderId,
        long menuItemId,
        String itemNameSnapshot,
        BigDecimal unitPriceSnapshot,
        int quantity,
        OrderLineStatus lineStatus
) {}
