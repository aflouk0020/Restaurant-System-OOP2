package ie.tus.oop2.restaurant.model;

public record DiningTable(
        int tableId,
        String label,
        int capacity,
        boolean active
) {}
