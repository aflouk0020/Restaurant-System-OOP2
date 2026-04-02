package ie.tus.oop2.restaurant.service;

import ie.tus.oop2.restaurant.dao.MenuItemDao;
import ie.tus.oop2.restaurant.dao.MenuItemDaoImpl;
import ie.tus.oop2.restaurant.dao.OrderDao;
import ie.tus.oop2.restaurant.dao.OrderDaoImpl;
import ie.tus.oop2.restaurant.dao.OrderLineDao;
import ie.tus.oop2.restaurant.dao.OrderLineDaoImpl;
import ie.tus.oop2.restaurant.model.MenuItem;
import ie.tus.oop2.restaurant.model.Order;
import ie.tus.oop2.restaurant.model.OrderLine;
import ie.tus.oop2.restaurant.model.OrderLineStatus;
import ie.tus.oop2.restaurant.model.OrderStatus;

import java.util.List;

public class OrderLineServiceImpl implements OrderLineService {

    private final OrderLineDao orderLineDao;
    private final MenuItemDao menuItemDao;
    private final OrderDao orderDao;

    public OrderLineServiceImpl() {
        this.orderLineDao = new OrderLineDaoImpl();
        this.menuItemDao = new MenuItemDaoImpl();
        this.orderDao = new OrderDaoImpl();
    }

    public OrderLineServiceImpl(OrderLineDao orderLineDao, MenuItemDao menuItemDao, OrderDao orderDao) {
        this.orderLineDao = orderLineDao;
        this.menuItemDao = menuItemDao;
        this.orderDao = orderDao;
    }

    @Override
    public List<OrderLine> listAllLines() {
        return orderLineDao.findAll();
    }

    @Override
    public List<OrderLine> listLinesForOrder(long orderId) {
        return orderLineDao.findByOrderId(orderId);
    }

    @Override
    public List<MenuItem> listAvailableMenuItems() {
        return menuItemDao.findAvailableOnly();
    }

    @Override
    public OrderLine addLine(long orderId, long menuItemId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Order order = orderDao.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        if (order.status() == OrderStatus.CANCELLED || order.status() == OrderStatus.PAID) {
            throw new IllegalStateException("Cannot add lines to order in status: " + order.status());
        }

        MenuItem menuItem = menuItemDao.findById(menuItemId);
        if (menuItem == null) {
            throw new IllegalArgumentException("Menu item not found: " + menuItemId);
        }

        if (!menuItem.available()) {
            throw new IllegalStateException("Menu item is not available: " + menuItem.name());
        }

        OrderLine existing = orderLineDao.findByOrderIdAndMenuItemId(orderId, menuItemId);

        if (existing != null) {
            OrderLine merged = new OrderLine(
                    existing.orderLineId(),
                    existing.orderId(),
                    existing.menuItemId(),
                    existing.itemNameSnapshot(),
                    existing.unitPriceSnapshot(),
                    existing.quantity() + quantity,
                    OrderLineStatus.NEW
            );

            boolean ok = orderLineDao.update(merged);
            if (!ok) {
                throw new IllegalStateException("Failed to update existing order line");
            }

            return orderLineDao.findById(existing.orderLineId());
        }

        OrderLine newLine = new OrderLine(
                0,
                orderId,
                menuItem.menuItemId(),
                menuItem.name(),
                menuItem.price(),
                quantity,
                OrderLineStatus.NEW
        );

        return orderLineDao.insert(newLine);
    }

    @Override
    public OrderLine updateLineStatus(long orderLineId, OrderLineStatus newStatus) {
        OrderLine existing = orderLineDao.findById(orderLineId);
        if (existing == null) {
            throw new IllegalArgumentException("Order line not found: " + orderLineId);
        }

        validateTransition(existing.lineStatus(), newStatus);

        OrderLine updated = new OrderLine(
                existing.orderLineId(),
                existing.orderId(),
                existing.menuItemId(),
                existing.itemNameSnapshot(),
                existing.unitPriceSnapshot(),
                existing.quantity(),
                newStatus
        );

        boolean ok = orderLineDao.update(updated);
        if (!ok) {
            throw new IllegalStateException("Failed to update line status");
        }

        return orderLineDao.findById(orderLineId);
    }

    @Override
    public OrderLine cancelLine(long orderLineId) {
        return updateLineStatus(orderLineId, OrderLineStatus.CANCELLED);
    }

    private void validateTransition(OrderLineStatus current, OrderLineStatus next) {
        if (current == next) return;

        switch (current) {
            case NEW -> {
                if (next != OrderLineStatus.IN_KITCHEN && next != OrderLineStatus.CANCELLED) {
                    throw new IllegalStateException("Invalid transition: " + current + " -> " + next);
                }
            }
            case IN_KITCHEN -> {
                if (next != OrderLineStatus.READY && next != OrderLineStatus.CANCELLED) {
                    throw new IllegalStateException("Invalid transition: " + current + " -> " + next);
                }
            }
            case READY -> {
                if (next != OrderLineStatus.SERVED && next != OrderLineStatus.CANCELLED) {
                    throw new IllegalStateException("Invalid transition: " + current + " -> " + next);
                }
            }
            case SERVED -> throw new IllegalStateException("Served line cannot be changed");
            case CANCELLED -> throw new IllegalStateException("Cancelled line cannot be changed");
        }
    }
    
    
    @Override
    public java.math.BigDecimal calculateOrderTotal(long orderId) {
        return orderLineDao.findByOrderId(orderId).stream()
                .filter(line -> line.lineStatus() != OrderLineStatus.CANCELLED)
                .map(line -> line.unitPriceSnapshot().multiply(java.math.BigDecimal.valueOf(line.quantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
}