package ie.tus.oop2.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MenuItem(
        long menuItemId,
        String name,
        MenuCategory category,
        BigDecimal price,
        boolean vegetarian,
        boolean available,
        LocalDateTime createdAt
) {}
