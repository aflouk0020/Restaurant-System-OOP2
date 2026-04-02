package ie.tus.oop2.restaurant.service;

import ie.tus.oop2.restaurant.model.MenuItem;
import ie.tus.oop2.restaurant.model.OrderLine;
import ie.tus.oop2.restaurant.model.OrderLineStatus;

import java.math.BigDecimal;
import java.util.List;

public interface OrderLineService {

    List<OrderLine> listAllLines();

    List<OrderLine> listLinesForOrder(long orderId);

    List<MenuItem> listAvailableMenuItems();

    OrderLine addLine(long orderId, long menuItemId, int quantity);

    OrderLine updateLineStatus(long orderLineId, OrderLineStatus newStatus);

    OrderLine cancelLine(long orderLineId);

    BigDecimal calculateOrderTotal(long orderId);
}