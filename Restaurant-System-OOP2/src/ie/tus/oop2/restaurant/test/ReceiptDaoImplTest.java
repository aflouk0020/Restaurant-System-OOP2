package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.dao.ReceiptDao;
import ie.tus.oop2.restaurant.dao.ReceiptDaoImpl;
import ie.tus.oop2.restaurant.model.PaymentType;
import ie.tus.oop2.restaurant.model.Receipt;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReceiptDaoImplTest {

    private ReceiptDao receiptDao;

    @BeforeAll
    static void verifyDbConnection() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        assertNotNull(c);
        assertFalse(c.isClosed());
    }

    @BeforeEach
    void setUp() {
        receiptDao = new ReceiptDaoImpl();
        cleanAllDependencySafe();
    }

    @AfterAll
    static void tearDownAll() {
        DatabaseConnection.closeConnection();
    }

    private void cleanAllDependencySafe() {
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

    private long seedTableSession(int tableId) {
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
                VALUES (?, ?, 'PAID', NULL, 'seed')
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, sessionId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().withNano(0)));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            fail("Failed to seed orders: " + e.getMessage());
            return -1;
        }
    }

    private long seedPayment(long orderId, BigDecimal amount) {
        String sql = """
                INSERT INTO payment (order_id, paid_at, payment_type, amount, currency, card_last4, voucher_code)
                VALUES (?, ?, ?, ?, 'EUR', NULL, NULL)
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, orderId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().withNano(0)));
            ps.setString(3, PaymentType.CASH.name());
            ps.setBigDecimal(4, amount);

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

    private static Receipt newReceipt(long orderId, long paymentId) {
        return new Receipt(
                0,
                orderId,
                paymentId,
                new BigDecimal("20.00"),
                new BigDecimal("2.60"),
                new BigDecimal("22.60"),
                LocalDateTime.now().withNano(0)
        );
    }

    @Test
    @Order(1)
    void insert_shouldGenerateId_andPersistFields() {
        long sessionId = seedTableSession(1);
        long orderId = seedOrder(sessionId);
        long paymentId = seedPayment(orderId, new BigDecimal("22.60"));

        Receipt saved = receiptDao.insert(newReceipt(orderId, paymentId));

        assertNotNull(saved);
        assertTrue(saved.receiptId() > 0);
        assertEquals(orderId, saved.orderId());
        assertEquals(paymentId, saved.paymentId());
        assertEquals(new BigDecimal("20.00"), saved.subtotal());
        assertEquals(new BigDecimal("2.60"), saved.tax());
        assertEquals(new BigDecimal("22.60"), saved.total());
        assertNotNull(saved.generatedAt());
    }

    @Test
    @Order(2)
    void findByOrderId_shouldReturnInsertedRow() {
        long sessionId = seedTableSession(2);
        long orderId = seedOrder(sessionId);
        long paymentId = seedPayment(orderId, new BigDecimal("22.60"));

        Receipt saved = receiptDao.insert(newReceipt(orderId, paymentId));

        Receipt found = receiptDao.findByOrderId(orderId);
        assertNotNull(found);
        assertEquals(saved.receiptId(), found.receiptId());
    }

    @Test
    @Order(3)
    void findByPaymentId_shouldReturnInsertedRow() {
        long sessionId = seedTableSession(3);
        long orderId = seedOrder(sessionId);
        long paymentId = seedPayment(orderId, new BigDecimal("22.60"));

        Receipt saved = receiptDao.insert(newReceipt(orderId, paymentId));

        Receipt found = receiptDao.findByPaymentId(paymentId);
        assertNotNull(found);
        assertEquals(saved.receiptId(), found.receiptId());
    }

    @Test
    @Order(4)
    void findAll_shouldReturnAllInsertedRows() {
        long s1 = seedTableSession(4);
        long o1 = seedOrder(s1);
        long p1 = seedPayment(o1, new BigDecimal("22.60"));
        receiptDao.insert(newReceipt(o1, p1));

        long s2 = seedTableSession(5);
        long o2 = seedOrder(s2);
        long p2 = seedPayment(o2, new BigDecimal("22.60"));
        receiptDao.insert(newReceipt(o2, p2));

        List<Receipt> all = receiptDao.findAll();
        assertEquals(2, all.size());
    }

    @Test
    @Order(5)
    void delete_shouldRemoveReceipt() {
        long sessionId = seedTableSession(6);
        long orderId = seedOrder(sessionId);
        long paymentId = seedPayment(orderId, new BigDecimal("22.60"));

        Receipt saved = receiptDao.insert(newReceipt(orderId, paymentId));

        boolean ok = receiptDao.delete(saved.receiptId());
        assertTrue(ok);

        assertNull(receiptDao.findById(saved.receiptId()));
    }
}