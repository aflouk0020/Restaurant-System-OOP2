package ie.tus.oop2.restaurant.reporting;

import ie.tus.oop2.restaurant.dao.MenuItemDao;
import ie.tus.oop2.restaurant.dao.MenuItemDaoImpl;
import ie.tus.oop2.restaurant.dao.OrderDao;
import ie.tus.oop2.restaurant.dao.OrderDaoImpl;
import ie.tus.oop2.restaurant.dao.OrderLineDao;
import ie.tus.oop2.restaurant.dao.OrderLineDaoImpl;
import ie.tus.oop2.restaurant.dao.PaymentDao;
import ie.tus.oop2.restaurant.dao.PaymentDaoImpl;
import ie.tus.oop2.restaurant.model.MenuItem;
import ie.tus.oop2.restaurant.model.Order;
import ie.tus.oop2.restaurant.model.OrderLine;
import ie.tus.oop2.restaurant.model.OrderLineStatus;
import ie.tus.oop2.restaurant.model.Payment;
import ie.tus.oop2.restaurant.model.PaymentType;
import ie.tus.oop2.restaurant.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ManagerReportsServiceImpl implements ManagerReportsService {

    private final PaymentDao paymentDao = new PaymentDaoImpl();
    private final OrderLineDao orderLineDao = new OrderLineDaoImpl();
    private final MenuItemDao menuItemDao = new MenuItemDaoImpl();
    private final OrderDao orderDao = new OrderDaoImpl();

    @Override
    public LinkedHashMap<LocalDate, BigDecimal> dailySalesTotals() {
        return paymentDao.findAll().stream()
                .collect(Collectors.groupingBy(
                        p -> p.paidAt().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Payment::amount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> MoneyUtil.scale(e.getValue()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public LinkedHashMap<String, Long> topSellingItems(int topN) {
        int limit = topN > 0 ? topN : 5;

        return activeLines().stream()
                .collect(Collectors.groupingBy(
                        OrderLine::itemNameSnapshot,
                        Collectors.summingLong(OrderLine::quantity)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<Boolean, BigDecimal> partitionSalesByVegetarian() {
        Map<Long, Boolean> vegetarianByMenuItemId = menuItemDao.findAll().stream()
                .collect(Collectors.toMap(MenuItem::menuItemId, MenuItem::vegetarian));

        BigDecimal vegetarianTotal = activeLines().stream()
                .filter(line -> vegetarianByMenuItemId.getOrDefault(line.menuItemId(), false))
                .map(this::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal nonVegetarianTotal = activeLines().stream()
                .filter(line -> !vegetarianByMenuItemId.getOrDefault(line.menuItemId(), false))
                .map(this::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Boolean, BigDecimal> result = new LinkedHashMap<>();
        result.put(true, MoneyUtil.scale(vegetarianTotal));
        result.put(false, MoneyUtil.scale(nonVegetarianTotal));
        return result;
    }

    @Override
    public LinkedHashMap<PaymentType, BigDecimal> revenueByPaymentType() {
        return paymentDao.findAll().stream()
                .collect(Collectors.groupingBy(
                        Payment::paymentType,
                        Collectors.reducing(BigDecimal.ZERO, Payment::amount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<PaymentType, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> MoneyUtil.scale(e.getValue()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public LinkedHashMap<Integer, BigDecimal> revenueByHour() {
        return paymentDao.findAll().stream()
                .collect(Collectors.groupingBy(
                        p -> p.paidAt().getHour(),
                        Collectors.reducing(BigDecimal.ZERO, Payment::amount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> MoneyUtil.scale(e.getValue()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public int peakSalesHour() {
        return revenueByHour().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);
    }

    @Override
    public LinkedHashMap<LocalDate, BigDecimal> topRevenueDays(int topN) {
        int limit = topN > 0 ? topN : 5;

        return dailySalesTotals().entrySet().stream()
                .sorted(Map.Entry.<LocalDate, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public LinkedHashMap<String, BigDecimal> revenueByStaff() {
        Map<Long, Order> ordersById = orderDao.findAll().stream()
                .collect(Collectors.toMap(Order::orderId, o -> o));

        return paymentDao.findAll().stream()
                .collect(Collectors.groupingBy(
                        payment -> {
                            Order order = ordersById.get(payment.orderId());
                            if (order == null || order.createdByStaffId() == null) {
                                return "Unassigned";
                            }
                            return "Staff #" + order.createdByStaffId();
                        },
                        Collectors.reducing(BigDecimal.ZERO, Payment::amount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> MoneyUtil.scale(e.getValue()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public BigDecimal averageTableSpend() {
        Map<Long, Order> ordersById = orderDao.findAll().stream()
                .collect(Collectors.toMap(Order::orderId, o -> o));

        Map<Long, BigDecimal> totalBySession = paymentDao.findAll().stream()
                .collect(Collectors.groupingBy(
                        payment -> {
                            Order order = ordersById.get(payment.orderId());
                            return order == null ? -1L : order.sessionId();
                        },
                        Collectors.reducing(BigDecimal.ZERO, Payment::amount, BigDecimal::add)
                ));

        totalBySession.remove(-1L);

        if (totalBySession.isEmpty()) {
            return MoneyUtil.zero();
        }

        BigDecimal total = totalBySession.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return MoneyUtil.scale(
                total.divide(BigDecimal.valueOf(totalBySession.size()), 2, java.math.RoundingMode.HALF_UP)
        );
    }

    @Override
    public RevenueStats overallRevenueStats() {
        List<Payment> payments = paymentDao.findAll();

        BigDecimal totalRevenue = payments.stream()
                .map(Payment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalPayments = payments.size();

        BigDecimal averageSpend = totalPayments == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(totalPayments), 2, java.math.RoundingMode.HALF_UP);

        return new RevenueStats(
                MoneyUtil.scale(totalRevenue),
                MoneyUtil.scale(averageSpend),
                (int) totalPayments
        );
    }

    private List<OrderLine> activeLines() {
        return orderLineDao.findAll().stream()
                .filter(line -> line.lineStatus() != OrderLineStatus.CANCELLED)
                .toList();
    }

    private BigDecimal lineTotal(OrderLine line) {
        return MoneyUtil.scale(
                line.unitPriceSnapshot().multiply(BigDecimal.valueOf(line.quantity()))
        );
    }
}