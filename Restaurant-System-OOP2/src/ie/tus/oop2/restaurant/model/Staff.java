package ie.tus.oop2.restaurant.model;

import java.time.LocalDateTime;

public record Staff(
        long staffId,
        String fullName,
        StaffRole role,
        String email,
        boolean active,
        LocalDateTime createdAt
) {}
