package ie.tus.oop2.restaurant.reporting;

import ie.tus.oop2.restaurant.dao.*;
import ie.tus.oop2.restaurant.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ManagerReportsServiceImpl implements ManagerReportsService {

    private static final int MONEY_SCALE = 2;

    private final ReceiptDao receiptDao;
    private final OrderLineDao orderLineDao;
    private final MenuItemDao menuItemDao;

    public ManagerReportsServiceImpl() {
        this.receiptDao = new ReceiptDaoImpl();
        this.orderLineDao = new OrderLineDaoImpl();
        this.menuItemDao = new MenuItemDaoImpl();
    }

    public ManagerReportsServiceImpl(ReceiptDao receiptDao, OrderLineDao orderLineDao, MenuItemDao menuItemDao) {
        this.receiptDao = receiptDao;
        this.orderLineDao = orderLineDao;
        this.menuItemDao = menuItemDao;
    }

    @Override
    public LinkedHashMap<LocalDate, BigDecimal> dailySalesTotals() {
        // Receipts are the source of truth for paid sales
        return receiptDao.findAll().stream()
                .collect(Collectors.groupingBy(
                        r -> r.generatedAt().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Receipt::total, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // sorting requirement
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public LinkedHashMap<String, Long> topSellingItems(int limit) {
        if (limit <= 0) limit = 10;

        // Only count items from PAID orders (i.e., orders that have receipts)
        Set<Long> paidOrderIds = receiptDao.findAll().stream()
                .map(Receipt::orderId)
                .collect(Collectors.toSet());

        Map<String, Long> counts = orderLineDao.findAll().stream()
                .filter(ol -> paidOrderIds.contains(ol.orderId()))
                .filter(ol -> ol.lineStatus() != OrderLineStatus.CANCELLED)
                .collect(Collectors.toMap(
                        OrderLine::itemNameSnapshot,
                        ol -> (long) ol.quantity(),
                        Long::sum
                ));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
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

        Map<Long, Boolean> vegByMenuId = menuItemDao.findAll().stream()
                .collect(Collectors.toMap(
                        MenuItem::menuItemId,
                        MenuItem::vegetarian
                ));

        Set<Long> paidOrderIds = receiptDao.findAll().stream()
                .map(Receipt::orderId)
                .collect(Collectors.toSet());

        Map<Boolean, BigDecimal> raw = orderLineDao.findAll().stream()
                .filter(ol -> paidOrderIds.contains(ol.orderId()))
                .filter(ol -> ol.lineStatus() != OrderLineStatus.CANCELLED)
                .collect(Collectors.partitioningBy(
                        ol -> vegByMenuId.getOrDefault(ol.menuItemId(), false),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                ol -> ol.unitPriceSnapshot().multiply(BigDecimal.valueOf(ol.quantity())),
                                BigDecimal::add
                        )
                ));

        BigDecimal vegTotal = raw.getOrDefault(true, BigDecimal.ZERO)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal nonVegTotal = raw.getOrDefault(false, BigDecimal.ZERO)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        // return a new safe map (no mutation)
        Map<Boolean, BigDecimal> out = new HashMap<>();
        out.put(true, vegTotal);
        out.put(false, nonVegTotal);
        return out;
    }
}