package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.model.OrderLineStatus;
import ie.tus.oop2.restaurant.model.PaymentType;
import ie.tus.oop2.restaurant.model.Receipt;
import ie.tus.oop2.restaurant.service.ReceiptService;
import ie.tus.oop2.restaurant.service.ReceiptServiceImpl;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReceiptServiceImplTest {

    private ReceiptService service;

    @BeforeAll
    static void verifyDbConnection() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        assertNotNull(c);
        assertFalse(c.isClosed());
    }

    @BeforeEach
    void setUp() {
        service = new ReceiptServiceImpl();
        cleanTables();
        cleanExportDir(Path.of("exports/receipts"));
    }

    @AfterAll
    static void tearDownAll() {
        DatabaseConnection.closeConnection();
    }

    // ------------------------------------------------------------
    // Clean in dependency-safe order
    // ------------------------------------------------------------
    private void cleanTables() {
        try (Connection c = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM receipt")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM payment")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM order_line")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM `orders`")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM table_session")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM reservation")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM menu_item")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM dining_table")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM staff")) { ps.executeUpdate(); }
        } catch (SQLException e) {
            fail("Failed to clean tables: " + e.getMessage());
        }
    }

    private void cleanExportDir(Path exportDir) {
        try {
            if (Files.exists(exportDir)) {
                Files.walk(exportDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (Exception ignored) { }
                        });
            }
        } catch (Exception e) {
            // not fatal for DB tests, but nice to know
            System.out.println("Warning: could not fully clean export dir: " + e.getMessage());
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

    private void ensureMenuItemExists(long menuItemId, String name, BigDecimal price) {
        // menu_item_id is AUTO_INCREMENT but MySQL allows inserting explicit IDs (fine for tests)
        String sql = """
                INSERT INTO menu_item (menu_item_id, name, category, price, vegetarian, available)
                VALUES (?, ?, 'MAIN', ?, false, true)
                ON DUPLICATE KEY UPDATE name=VALUES(name), price=VALUES(price), available=VALUES(available)
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, menuItemId);
            ps.setString(2, name);
            ps.setBigDecimal(3, price);
            ps.executeUpdate();

        } catch (SQLException e) {
            fail("Failed to seed menu_item(" + menuItemId + "): " + e.getMessage());
        }
    }

    private long seedOpenSession(int tableId) {
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
                VALUES (?, ?, 'CREATED', NULL, ?)
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, sessionId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().withNano(0)));
            ps.setString(3, "Test order");
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

    private void seedOrderLine(long orderId,
                              long menuItemId,
                              String nameSnap,
                              BigDecimal unitPrice,
                              int qty,
                              OrderLineStatus status) {

        // FK requirement: menu_item must exist first
        ensureMenuItemExists(menuItemId, nameSnap, unitPrice);

        String sql = """
                INSERT INTO order_line (order_id, menu_item_id, item_name_snapshot, unit_price_snapshot, quantity, line_status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, orderId);
            ps.setLong(2, menuItemId);
            ps.setString(3, nameSnap);
            ps.setBigDecimal(4, unitPrice);
            ps.setInt(5, qty);
            ps.setString(6, status.name());
            ps.executeUpdate();

        } catch (SQLException e) {
            fail("Failed to seed order_line: " + e.getMessage());
        }
    }

    private long seedPayment(long orderId,
                             BigDecimal amount,
                             PaymentType type,
                             String currency,
                             String cardLast4,
                             String voucherCode) {

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

    // ------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------

    @Test
    @Order(1)
    void generateReceiptForOrder_shouldInsertReceipt_andExportFile() throws Exception {
        long sessionId = seedOpenSession(1);
        long orderId = seedOrder(sessionId);

        // Order lines:
        // subtotal = 10.00*2 + 5.00*1 = 25.00
        seedOrderLine(orderId, 1001, "Burger", new BigDecimal("10.00"), 2, OrderLineStatus.NEW);
        seedOrderLine(orderId, 1002, "Drink",  new BigDecimal("5.00"),  1, OrderLineStatus.NEW);

        BigDecimal subtotal = new BigDecimal("25.00");
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.13"))
                .setScale(2, java.math.RoundingMode.HALF_UP); // 3.25
        BigDecimal total = subtotal.add(tax)
                .setScale(2, java.math.RoundingMode.HALF_UP); // 28.25

        seedPayment(orderId, total, PaymentType.CASH, "EUR", null, null);

        Path exportDir = Path.of("exports/receipts");

        Receipt receipt = service.generateReceiptForOrder(orderId, exportDir);

        assertNotNull(receipt);
        assertTrue(receipt.receiptId() > 0);
        assertEquals(orderId, receipt.orderId());
        assertEquals(subtotal, receipt.subtotal());
        assertEquals(tax, receipt.tax());
        assertEquals(total, receipt.total());
        assertNotNull(receipt.generatedAt());

        // Verify file exists in exports/receipts with your required prefix
        assertTrue(Files.exists(exportDir), "Export directory was not created");

        Path newest = Files.list(exportDir)
                .filter(p -> p.getFileName().toString().startsWith("receipt_" + orderId + "_"))
                .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                .orElseThrow(() -> new AssertionError("No receipt file found for orderId=" + orderId));

        assertTrue(Files.size(newest) > 0, "Receipt file is empty");
    }

    @Test
    @Order(2)
    void generateReceiptForOrder_shouldFailIfNoPayment() {
        long sessionId = seedOpenSession(2);
        long orderId = seedOrder(sessionId);

        seedOrderLine(orderId, 2001, "Pasta", new BigDecimal("12.00"), 1, OrderLineStatus.NEW);

        assertThrows(IllegalStateException.class,
                () -> service.generateReceiptForOrder(orderId, Path.of("exports/receipts")));
    }

    @Test
    @Order(3)
    void generateReceiptForOrder_shouldFailIfPaymentDoesNotMatchTotal() {
        long sessionId = seedOpenSession(3);
        long orderId = seedOrder(sessionId);

        seedOrderLine(orderId, 3001, "Steak", new BigDecimal("20.00"), 1, OrderLineStatus.NEW);

        // Correct total would be 20.00 + 2.60 = 22.60, but pay wrong amount
        seedPayment(orderId, new BigDecimal("10.00"), PaymentType.CASH, "EUR", null, null);

        assertThrows(IllegalStateException.class,
                () -> service.generateReceiptForOrder(orderId, Path.of("exports/receipts")));
    }
    
    
    @Test
    @Order(4)
    void generateReceiptForOrder_shouldFailIfCardAmountMismatch() {
        long sessionId = seedOpenSession(4);
        long orderId = seedOrder(sessionId);

        // subtotal = 25.00
        seedOrderLine(orderId, 4001, "Burger", new BigDecimal("10.00"), 2, OrderLineStatus.NEW); // 20
        seedOrderLine(orderId, 4002, "Drink",  new BigDecimal("5.00"),  1, OrderLineStatus.NEW); // 5

        BigDecimal subtotal = new BigDecimal("25.00");
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.13"))
                .setScale(2, java.math.RoundingMode.HALF_UP); // 3.25
        BigDecimal total = subtotal.add(tax)
                .setScale(2, java.math.RoundingMode.HALF_UP); // 28.25

        // CARD requires last4 (so we provide it), but amount is WRONG
        seedPayment(orderId, new BigDecimal("10.00"), PaymentType.CARD, "EUR", "1234", null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.generateReceiptForOrder(orderId, Path.of("exports/receipts")));

        assertTrue(ex.getMessage().toLowerCase().contains("card"));
        assertTrue(ex.getMessage().toLowerCase().contains("mismatch"));
    }
    @Test
    @Order(5)
    void generateReceiptForOrder_shouldFailIfVoucherMissingCode() {
        long sessionId = seedOpenSession(5);
        long orderId = seedOrder(sessionId);

        seedOrderLine(orderId, 5001, "Pasta", new BigDecimal("12.00"), 2, OrderLineStatus.NEW); // 24
        seedOrderLine(orderId, 5002, "Water", new BigDecimal("1.00"),  1, OrderLineStatus.NEW); // 1

        BigDecimal subtotal = new BigDecimal("25.00");
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.13")).setScale(2, java.math.RoundingMode.HALF_UP); // 3.25
        BigDecimal total = subtotal.add(tax).setScale(2, java.math.RoundingMode.HALF_UP); // 28.25

        // VOUCHER but code blank -> should fail
        seedPayment(orderId, total, PaymentType.VOUCHER, "EUR", null, "   ");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.generateReceiptForOrder(orderId, Path.of("exports/receipts")));

        assertTrue(ex.getMessage().toLowerCase().contains("voucher"));
    }
}