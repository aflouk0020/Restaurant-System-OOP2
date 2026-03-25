package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.model.Order;

import java.util.List;

public interface OrderDao {
    Order findById(long orderId);
    List<Order> findAll();
    List<Order> findBySessionId(long sessionId);

    Order insert(Order order);
    boolean update(Order order);
    boolean delete(long orderId);
}