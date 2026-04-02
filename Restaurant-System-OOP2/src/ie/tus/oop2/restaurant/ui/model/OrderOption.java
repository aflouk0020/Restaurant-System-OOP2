package ie.tus.oop2.restaurant.ui.model;

import ie.tus.oop2.restaurant.model.OrderStatus;

public record OrderOption(long orderId, long sessionId, OrderStatus status) {

    @Override
    public String toString() {
        return "Order #" + orderId + " - Session " + sessionId + " - " + status;
    }
}