package ie.tus.oop2.restaurant.reporting;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.dao.*;
import ie.tus.oop2.restaurant.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    
    
    
    
    private record PaymentSaleRow(PaymentType type, BigDecimal total) {}

    @Override
    public LinkedHashMap<PaymentType, BigDecimal> revenueByPaymentType() {

        List<PaymentSaleRow> rows = fetchReceiptTotalsWithPaymentType();

        // group + sum using Collectors.reducing (BigDecimal-safe)
        Map<PaymentType, BigDecimal> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        PaymentSaleRow::type,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                PaymentSaleRow::total,
                                BigDecimal::add
                        )
                ));

        // sort desc by revenue and return LinkedHashMap
        return grouped.entrySet().stream()
                .sorted(Map.Entry.<PaymentType, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private List<PaymentSaleRow> fetchReceiptTotalsWithPaymentType() {
        String sql = """
                SELECT p.payment_type, r.total
                FROM receipt r
                JOIN payment p ON p.payment_id = r.payment_id
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<PaymentSaleRow> list = new ArrayList<>();
            while (rs.next()) {
                PaymentType type = PaymentType.valueOf(rs.getString("payment_type"));
                BigDecimal total = rs.getBigDecimal("total");
                list.add(new PaymentSaleRow(type, total));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to build revenueByPaymentType report", e);
        }
    }
    
    
    
    @Override
    public LinkedHashMap<Integer, BigDecimal> revenueByHour() {

        record ReceiptRow(LocalDateTime generatedAt, BigDecimal total) {}

        List<ReceiptRow> rows = new ArrayList<>();

        String sql = "SELECT generated_at, total FROM receipt";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("generated_at");
                BigDecimal total = rs.getBigDecimal("total");
                if (ts != null && total != null) {
                    rows.add(new ReceiptRow(ts.toLocalDateTime(), total));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to build revenueByHour report: " + e.getMessage(), e);
        }

        // group by hour, sum totals, sort desc by revenue
        return rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.generatedAt().getHour(),
                        Collectors.reducing(BigDecimal.ZERO, ReceiptRow::total, BigDecimal::add)
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<Integer, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public int peakSalesHour() {
        return revenueByHour()
                .keySet()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No receipts found. Cannot compute peakSalesHour."));
    }
    
    @Override
    public LinkedHashMap<LocalDate, BigDecimal> topRevenueDays(int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be > 0");

        // Reuse dailySalesTotals() then sort + limit
        return dailySalesTotals().entrySet().stream()
                .sorted(Map.Entry.<LocalDate, BigDecimal>comparingByValue().reversed())
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
    
}