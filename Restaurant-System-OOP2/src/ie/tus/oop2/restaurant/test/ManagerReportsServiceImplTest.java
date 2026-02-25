package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.model.PaymentType;
import ie.tus.oop2.restaurant.reporting.ManagerReportsService;
import ie.tus.oop2.restaurant.reporting.ManagerReportsServiceImpl;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ManagerReportsServiceImplTest {

    private ManagerReportsService reports;

    @BeforeAll
    static void verifyDbConnection() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        assertNotNull(c);
        assertFalse(c.isClosed());
    }

    @BeforeEach
    void setUp() {
        reports = new ManagerReportsServiceImpl();
        cleanTables();
    }

    @AfterAll
    static void tearDownAll() {
        DatabaseConnection.closeConnection();
    }

    // ------------------------------------------------------------
    // Clean (dependency-safe)
    // ------------------------------------------------------------
    private void cleanTables() {
        try (Connection c = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM receipt")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM payment")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM order_line")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM `orders`")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM table_session")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM menu_item")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM dining_table")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM staff")) { ps.executeUpdate(); }
        } catch (SQLException e) {
            fail("Failed to clean tables: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // Seed helpers
    // ------------------------------------------------------------

    private void ensureDiningTableExists(int tableId) {
        String sql = """
                INSERT INTO dining_table (table_id, label, capacity, active)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE label=VALUES(label), capacity=VALUES(capacity), active=VALUES(active)
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, tableId);
            ps.setString(2, "T" + tableId);
            ps.setInt(3, 4);
            ps.setBoolean(4, true);
            ps.executeUpdate();

        } catch (SQLException e) {
            fail("Failed to seed dining_table: " + e.getMessage());
        }
    }

    private long seedSession(int tableId) {
        ensureDiningTableExists(tableId);

        String sql = """
                INSERT INTO table_session (table_id, reservation_id, opened_at, closed_at, opened_by_staff_id, status)
                VALUES (?, NULL, ?, NULL, NULL, 'OPEN')
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, tableId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().withNano(0)));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }

        } catch (SQLException e) {
            fail("Failed to seed table_session: " + e.getMessage());
            return -1;
        }
    }

    private long seedOrder(long sessionId) {
        String sql = """
                INSERT INTO `orders` (session_id, created_at, status, created_by_staff_id, notes)
                VALUES (?, ?, 'PAID', NULL, ?)
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, sessionId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().withNano(0)));
            ps.setString(3, "Paid order");
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }

        } catch (SQLException e) {
            fail("Failed to seed order: " + e.getMessage());
            return -1;
        }
    }

    private long seedMenuItem(String name, String category, BigDecimal price, boolean vegetarian) {
        String sql = """
                INSERT INTO menu_item (name, category, price, vegetarian, available)
                VALUES (?, ?, ?, ?, true)
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, category);
            ps.setBigDecimal(3, price);
            ps.setBoolean(4, vegetarian);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }

        } catch (SQLException e) {
            fail("Failed to seed menu_item: " + e.getMessage());
            return -1;
        }
    }

    private void seedOrderLine(long orderId, long menuItemId, String snap, BigDecimal unitPrice, int qty, String lineStatus) {
        String sql = """
                INSERT INTO order_line (order_id, menu_item_id, item_name_snapshot, unit_price_snapshot, quantity, line_status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, orderId);
            ps.setLong(2, menuItemId);
            ps.setString(3, snap);
            ps.setBigDecimal(4, unitPrice);
            ps.setInt(5, qty);
            ps.setString(6, lineStatus);
            ps.executeUpdate();

        } catch (SQLException e) {
            fail("Failed to seed order_line: " + e.getMessage());
        }
    }

    // Simple payment seed (default CASH/EUR)
    private long seedPayment(long orderId, BigDecimal amount) {
        return seedPayment(orderId, amount, PaymentType.CASH, "EUR", null, null);
    }

    // Full payment seed (supports CASH/CARD/VOUCHER)
    private long seedPayment(long orderId, BigDecimal amount, PaymentType type,
                             String currency, String cardLast4, String voucherCode) {

        String sql = """
                INSERT INTO payment (order_id, paid_at, payment_type, amount, currency, card_last4, voucher_code)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, orderId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().withNano(0)));
            ps.setString(3, type.name());
            ps.setBigDecimal(4, amount);
            ps.setString(5, currency);

            if (cardLast4 == null) ps.setNull(6, Types.CHAR);
            else ps.setString(6, cardLast4);

            if (voucherCode == null) ps.setNull(7, Types.VARCHAR);
            else ps.setString(7, voucherCode);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }

        } catch (SQLException e) {
            fail("Failed to seed payment: " + e.getMessage());
            return -1;
        }
    }

    // Receipt overload (5 args) -> delegates to 6 args
    private void seedReceipt(long orderId, long paymentId,
                             BigDecimal subtotal, BigDecimal tax, BigDecimal total) {
        seedReceipt(orderId, paymentId, subtotal, tax, total, LocalDateTime.now().withNano(0));
    }

    private long seedReceipt(long orderId, long paymentId,
                             BigDecimal subtotal, BigDecimal tax, BigDecimal total,
                             LocalDateTime generatedAt) {

        String sql = """
                INSERT INTO receipt (order_id, payment_id, subtotal, tax, total, generated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, orderId);
            ps.setLong(2, paymentId);
            ps.setBigDecimal(3, subtotal);
            ps.setBigDecimal(4, tax);
            ps.setBigDecimal(5, total);
            ps.setTimestamp(6, Timestamp.valueOf(generatedAt.withNano(0)));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }

        } catch (SQLException e) {
            fail("Failed to seed receipt: " + e.getMessage());
            return -1;
        }
    }

    // ------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------

    @Test
    @Order(1)
    void dailySalesTotals_shouldGroupByDate_andSumTotals() {
        long session1 = seedSession(1);
        long order1 = seedOrder(session1);

        long menuVeg = seedMenuItem("Veg Pasta", "MAIN", new BigDecimal("10.00"), true);
        seedOrderLine(order1, menuVeg, "Veg Pasta", new BigDecimal("10.00"), 2, "SERVED"); // 20

        BigDecimal subtotal = new BigDecimal("20.00");
        BigDecimal tax = new BigDecimal("2.60");
        BigDecimal total = new BigDecimal("22.60");
        long pay1 = seedPayment(order1, total);

        LocalDateTime day = LocalDateTime.now().minusDays(1).withNano(0);
        seedReceipt(order1, pay1, subtotal, tax, total, day);

        LinkedHashMap<LocalDate, BigDecimal> report = reports.dailySalesTotals();

        assertEquals(1, report.size());
        assertEquals(total, report.get(day.toLocalDate()));
    }

    @Test
    @Order(2)
    void topSellingItems_shouldReturnSortedCounts_desc() {
        long session = seedSession(2);

        long orderA = seedOrder(session);
        long orderB = seedOrder(session);

        long itemBurger = seedMenuItem("Burger", "MAIN", new BigDecimal("12.00"), false);
        long itemSalad  = seedMenuItem("Salad", "STARTER", new BigDecimal("5.00"), true);

        seedOrderLine(orderA, itemBurger, "Burger", new BigDecimal("12.00"), 3, "SERVED");
        seedOrderLine(orderA, itemSalad,  "Salad",  new BigDecimal("5.00"),  1, "SERVED");

        seedOrderLine(orderB, itemBurger, "Burger", new BigDecimal("12.00"), 2, "SERVED");

        long payA = seedPayment(orderA, new BigDecimal("1.00"));
        seedReceipt(orderA, payA, new BigDecimal("0.50"), new BigDecimal("0.50"), new BigDecimal("1.00"));

        long payB = seedPayment(orderB, new BigDecimal("1.00"));
        seedReceipt(orderB, payB, new BigDecimal("0.50"), new BigDecimal("0.50"), new BigDecimal("1.00"));

        LinkedHashMap<String, Long> top = reports.topSellingItems(10);

        assertEquals(2, top.size());
        assertEquals(5L, top.get("Burger"));
        assertEquals(1L, top.get("Salad"));
        assertEquals("Burger", top.keySet().iterator().next());
    }

    @Test
    @Order(3)
    void partitionSalesByVegetarian_shouldPartitionTotals() {
        long session = seedSession(3);
        long order = seedOrder(session);

        long veg = seedMenuItem("Veg Wrap", "MAIN", new BigDecimal("8.00"), true);
        long nonVeg = seedMenuItem("Chicken", "MAIN", new BigDecimal("10.00"), false);

        seedOrderLine(order, veg, "Veg Wrap", new BigDecimal("8.00"), 2, "SERVED");       // 16
        seedOrderLine(order, nonVeg, "Chicken", new BigDecimal("10.00"), 1, "SERVED");   // 10

        long pay = seedPayment(order, new BigDecimal("1.00"));
        seedReceipt(order, pay, new BigDecimal("0.50"), new BigDecimal("0.50"), new BigDecimal("1.00"));

        Map<Boolean, BigDecimal> parts = reports.partitionSalesByVegetarian();

        assertEquals(new BigDecimal("16.00"), parts.get(true));
        assertEquals(new BigDecimal("10.00"), parts.get(false));
    }

    @Test
    @Order(4)
    void revenueByPaymentType_shouldGroupAndSortDesc() {
        long session1 = seedSession(10);
        long order1 = seedOrder(session1);

        long session2 = seedSession(11);
        long order2 = seedOrder(session2);

        long session3 = seedSession(12);
        long order3 = seedOrder(session3);

        long pay1 = seedPayment(order1, new BigDecimal("28.25"), PaymentType.CASH, "EUR", null, null);
        seedReceipt(order1, pay1, new BigDecimal("25.00"), new BigDecimal("3.25"), new BigDecimal("28.25"));

        long pay2 = seedPayment(order2, new BigDecimal("50.00"), PaymentType.CARD, "EUR", "1234", null);
        seedReceipt(order2, pay2, new BigDecimal("44.25"), new BigDecimal("5.75"), new BigDecimal("50.00"));

        long pay3 = seedPayment(order3, new BigDecimal("10.00"), PaymentType.CASH, "EUR", null, null);
        seedReceipt(order3, pay3, new BigDecimal("8.85"), new BigDecimal("1.15"), new BigDecimal("10.00"));

        LinkedHashMap<PaymentType, BigDecimal> map = reports.revenueByPaymentType();

        assertNotNull(map);
        assertEquals(new BigDecimal("38.25"), map.get(PaymentType.CASH)); // 28.25 + 10.00
        assertEquals(new BigDecimal("50.00"), map.get(PaymentType.CARD));
        assertNull(map.get(PaymentType.VOUCHER));

        assertEquals(PaymentType.CARD, map.keySet().iterator().next());
    }
    
    @Test
    @Order(5)
    void peakSalesHour_shouldReturnHourWithHighestRevenue() {
        long s1 = seedSession(20);
        long o1 = seedOrder(s1);

        long s2 = seedSession(21);
        long o2 = seedOrder(s2);

        long s3 = seedSession(22);
        long o3 = seedOrder(s3);

        // Payments (FK path)
        long p1 = seedPayment(o1, new BigDecimal("10.00"));
        long p2 = seedPayment(o2, new BigDecimal("50.00"));
        long p3 = seedPayment(o3, new BigDecimal("20.00"));

        // Receipts at specific hours:
        // hour 14 total = 10 + 20 = 30
        // hour 19 total = 50  -> should win
        seedReceipt(o1, p1, new BigDecimal("8.85"), new BigDecimal("1.15"), new BigDecimal("10.00"),
                LocalDateTime.now().withHour(14).withMinute(0).withSecond(0).withNano(0));

        seedReceipt(o2, p2, new BigDecimal("44.25"), new BigDecimal("5.75"), new BigDecimal("50.00"),
                LocalDateTime.now().withHour(19).withMinute(0).withSecond(0).withNano(0));

        seedReceipt(o3, p3, new BigDecimal("17.70"), new BigDecimal("2.30"), new BigDecimal("20.00"),
                LocalDateTime.now().withHour(14).withMinute(0).withSecond(0).withNano(0));

        int peakHour = reports.peakSalesHour();
        assertEquals(19, peakHour);

        var map = reports.revenueByHour();
        assertEquals(new BigDecimal("50.00"), map.get(19));
        assertEquals(new BigDecimal("30.00"), map.get(14));
    }
    
    @Test
    @Order(6)
    void topRevenueDays_shouldReturnTopNDaysSortedDesc() {
        // 3 receipts on 3 different days with different totals
        long s1 = seedSession(30);
        long o1 = seedOrder(s1);
        long p1 = seedPayment(o1, new BigDecimal("1.00"));
        seedReceipt(o1, p1,
                new BigDecimal("0.50"), new BigDecimal("0.50"), new BigDecimal("50.00"),
                LocalDateTime.now().minusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0));

        long s2 = seedSession(31);
        long o2 = seedOrder(s2);
        long p2 = seedPayment(o2, new BigDecimal("1.00"));
        seedReceipt(o2, p2,
                new BigDecimal("0.50"), new BigDecimal("0.50"), new BigDecimal("10.00"),
                LocalDateTime.now().minusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0));

        long s3 = seedSession(32);
        long o3 = seedOrder(s3);
        long p3 = seedPayment(o3, new BigDecimal("1.00"));
        seedReceipt(o3, p3,
                new BigDecimal("0.50"), new BigDecimal("0.50"), new BigDecimal("30.00"),
                LocalDateTime.now().minusDays(2).withHour(18).withMinute(0).withSecond(0).withNano(0));

        LinkedHashMap<LocalDate, BigDecimal> top2 = reports.topRevenueDays(2);

        assertEquals(2, top2.size());

        // Should be sorted desc: 50 then 30
        BigDecimal first = top2.entrySet().iterator().next().getValue();
        assertEquals(new BigDecimal("50.00"), first);

        BigDecimal second = top2.values().stream().skip(1).findFirst().orElseThrow();
        assertEquals(new BigDecimal("30.00"), second);
    }
}