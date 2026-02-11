package ie.tus.oop2.restaurant.model;

import java.time.LocalDateTime;

public record Reservation(
        long reservationId,
        String customerName,
        String phone,
        int partySize,
        LocalDateTime reservedFor,
        Integer tableId,          // nullable → Integer
        String notes,
        ReservationStatus status,
        LocalDateTime createdAt
) {}
