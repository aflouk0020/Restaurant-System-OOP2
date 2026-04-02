package ie.tus.oop2.restaurant.service;

import ie.tus.oop2.restaurant.dao.OrderDao;
import ie.tus.oop2.restaurant.dao.OrderDaoImpl;
import ie.tus.oop2.restaurant.model.Order;
import ie.tus.oop2.restaurant.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public class OrderServiceImpl implements OrderService {

    private final OrderDao orderDao;

    public OrderServiceImpl() {
        this.orderDao = new OrderDaoImpl();
    }

    public OrderServiceImpl(OrderDao orderDao) {
        this.orderDao = orderDao;
    }

    @Override
    public Order createOrder(long sessionId, Long createdByStaffId, String notes) {
        Order newOrder = new Order(
                0,
                sessionId,
                LocalDateTime.now().withNano(0),
                OrderStatus.CREATED,
                createdByStaffId,
                notes
        );
        return orderDao.insert(newOrder);
    }

    @Override
    public Order submitOrder(long orderId) {
        Order existing = mustExist(orderId);

        if (existing.status() != OrderStatus.CREATED) {
            throw new IllegalStateException("Only CREATED orders can be submitted. Current: " + existing.status());
        }

        Order updated = new Order(
                existing.orderId(),
                existing.sessionId(),
                existing.createdAt(),
                OrderStatus.SUBMITTED,
                existing.createdByStaffId(),
                existing.notes()
        );

        boolean ok = orderDao.update(updated);
        if (!ok) throw new IllegalStateException("Failed to submit order id " + orderId);

        return orderDao.findById(orderId);
    }

    @Override
    public Order cancelOrder(long orderId) {
        Order existing = mustExist(orderId);

        if (existing.status() == OrderStatus.PAID) {
            throw new IllegalStateException("Cannot cancel a PAID order.");
        }

        Order updated = new Order(
                existing.orderId(),
                existing.sessionId(),
                existing.createdAt(),
                OrderStatus.CANCELLED,
                existing.createdByStaffId(),
                existing.notes()
        );

        boolean ok = orderDao.update(updated);
        if (!ok) throw new IllegalStateException("Failed to cancel order id " + orderId);

        return orderDao.findById(orderId);
    }

    private Order mustExist(long orderId) {
        Order o = orderDao.findById(orderId);
        if (o == null) throw new IllegalArgumentException("Order not found: " + orderId);
        return o;
    }
    @Override
    public List<Order> listOrders() {
        return orderDao.findAll();
    }
}