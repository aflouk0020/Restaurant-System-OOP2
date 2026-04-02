package ie.tus.oop2.restaurant.service;

import java.util.List;

import ie.tus.oop2.restaurant.model.Order;

public interface OrderService {
    Order createOrder(long sessionId, Long createdByStaffId, String notes);
    Order submitOrder(long orderId);
    Order cancelOrder(long orderId);
    List<Order> listOrders();
}