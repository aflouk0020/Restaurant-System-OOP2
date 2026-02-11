package ie.tus.oop2.restaurant.model;

import java.time.LocalDateTime;

public record TableSession(
        long sessionId,
        int tableId,
        Long reservationId,       // nullable
        LocalDateTime openedAt,
        LocalDateTime closedAt,   // nullable
        Long openedByStaffId,     // nullable
        SessionStatus status
) {}
