package ie.tus.oop2.restaurant.model;

import java.time.LocalDateTime;

public record Order(
        long orderId,
        long sessionId,
        LocalDateTime createdAt,
        OrderStatus status,
        Long createdByStaffId,    // nullable
        String notes
) {}
