package ie.tus.oop2.restaurant.reporting;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.dao.MenuItemDao;
import ie.tus.oop2.restaurant.dao.MenuItemDaoImpl;
import ie.tus.oop2.restaurant.dao.OrderLineDao;
import ie.tus.oop2.restaurant.dao.OrderLineDaoImpl;
import ie.tus.oop2.restaurant.dao.ReceiptDao;
import ie.tus.oop2.restaurant.dao.ReceiptDaoImpl;
import ie.tus.oop2.restaurant.model.MenuItem;
import ie.tus.oop2.restaurant.model.OrderLine;
import ie.tus.oop2.restaurant.model.OrderLineStatus;
import ie.tus.oop2.restaurant.model.PaymentType;
import ie.tus.oop2.restaurant.model.Receipt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
    public BigDecimal averageTableSpend() {

        List<Receipt> receipts = receiptDao.findAll();

        if (receipts.isEmpty()) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal totalRevenue = receipts.stream()
                .map(Receipt::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = totalRevenue.divide(
                BigDecimal.valueOf(receipts.size()),
                MONEY_SCALE,
                RoundingMode.HALF_UP
        );

        return average;
    }

    @Override
    public LinkedHashMap<LocalDate, BigDecimal> dailySalesTotals() {
        return receiptDao.findAll().stream()
                .collect(Collectors.groupingBy(
                        r -> r.generatedAt().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Receipt::total, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
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

        Map<Boolean, BigDecimal> out = new HashMap<>();
        out.put(true, vegTotal);
        out.put(false, nonVegTotal);
        return out;
    }

    private record PaymentSaleRow(PaymentType type, BigDecimal total) {}

    @Override
    public LinkedHashMap<PaymentType, BigDecimal> revenueByPaymentType() {

        List<PaymentSaleRow> rows = fetchReceiptTotalsWithPaymentType();

        Map<PaymentType, BigDecimal> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        PaymentSaleRow::type,
                        Collectors.reducing(BigDecimal.ZERO, PaymentSaleRow::total, BigDecimal::add)
                ));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.<PaymentType, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().setScale(MONEY_SCALE, RoundingMode.HALF_UP),
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

        return rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.generatedAt().getHour(),
                        Collectors.reducing(BigDecimal.ZERO, ReceiptRow::total, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<Integer, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public int peakSalesHour() {
        return revenueByHour().keySet().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No receipts found. Cannot compute peakSalesHour."));
    }

    @Override
    public LinkedHashMap<LocalDate, BigDecimal> topRevenueDays(int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be > 0");

        return dailySalesTotals().entrySet().stream()
                .sorted(Map.Entry.<LocalDate, BigDecimal>comparingByValue().reversed())
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

        record Row(String staffName, BigDecimal total) {}

        String sql = """
                SELECT 
                    COALESCE(s.full_name, CONCAT('STAFF_', o.created_by_staff_id)) AS staff_name,
                    r.total AS total
                FROM receipt r
                JOIN `orders` o ON o.order_id = r.order_id
                LEFT JOIN staff s ON s.staff_id = o.created_by_staff_id
                WHERE o.created_by_staff_id IS NOT NULL
                """;

        List<Row> rows = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("staff_name");
                BigDecimal total = rs.getBigDecimal("total");
                if (name != null && total != null) {
                    rows.add(new Row(name, total));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to build revenueByStaff report: " + e.getMessage(), e);
        }

        Map<String, BigDecimal> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        Row::staffName,
                        Collectors.reducing(BigDecimal.ZERO, Row::total, BigDecimal::add)
                ));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
    
    
    @Override
    public RevenueStats overallRevenueStats() {

        // Receipts are the source of truth (paid sales)
        var receipts = receiptDao.findAll();

        int count = receipts.size();
        if (count == 0) {
            return new RevenueStats(
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    0
            );
        }

        BigDecimal totalRevenue = receipts.stream()
                .map(Receipt::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal averageSpend = totalRevenue
                .divide(BigDecimal.valueOf(count), MONEY_SCALE, RoundingMode.HALF_UP);

        return new RevenueStats(totalRevenue, averageSpend, count);
    }
}