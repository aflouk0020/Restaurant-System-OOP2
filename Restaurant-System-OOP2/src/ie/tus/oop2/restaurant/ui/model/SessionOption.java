package ie.tus.oop2.restaurant.ui.model;

public record SessionOption(long sessionId, int tableId) {

    @Override
    public String toString() {
        return "Session #" + sessionId + " - Table " + tableId;
    }
}