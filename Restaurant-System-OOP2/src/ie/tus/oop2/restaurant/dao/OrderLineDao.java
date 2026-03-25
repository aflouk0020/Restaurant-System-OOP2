package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.model.OrderLine;

import java.util.List;

public interface OrderLineDao {
    OrderLine findById(long orderLineId);
    List<OrderLine> findAll();
    List<OrderLine> findByOrderId(long orderId);

    OrderLine insert(OrderLine line);
    boolean update(OrderLine line);
    boolean delete(long orderLineId);

    // Optional but super useful for the UNIQUE(order_id, menu_item_id) rule
    OrderLine findByOrderIdAndMenuItemId(long orderId, long menuItemId);
}
