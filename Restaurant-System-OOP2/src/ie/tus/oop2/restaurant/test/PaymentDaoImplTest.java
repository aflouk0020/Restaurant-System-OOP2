package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.dao.PaymentDao;
import ie.tus.oop2.restaurant.dao.PaymentDaoImpl;
import ie.tus.oop2.restaurant.model.Payment;
import ie.tus.oop2.restaurant.model.PaymentType;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PaymentDaoImplTest {

    private PaymentDao paymentDao;

    @BeforeEach
    void setUp() {
        paymentDao = new PaymentDaoImpl();
        cleanTables();
    }

    private void cleanTables() {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.prepareStatement("DELETE FROM receipt").executeUpdate();
            c.prepareStatement("DELETE FROM payment").executeUpdate();
            c.prepareStatement("DELETE FROM `orders`").executeUpdate();
            c.prepareStatement("DELETE FROM table_session").executeUpdate();
            c.prepareStatement("DELETE FROM dining_table").executeUpdate();
        } catch (SQLException e) {
            fail("Cleanup failed: " + e.getMessage());
        }
    }

    private long seedOrder() throws SQLException {
        Connection c = DatabaseConnection.getConnection();

        c.prepareStatement("""
                INSERT INTO dining_table (table_id, label, capacity, active)
                VALUES (1, 'T1', 4, true)
                ON DUPLICATE KEY UPDATE label='T1'
                """).executeUpdate();

        c.prepareStatement("""
                INSERT INTO table_session (table_id, reservation_id, opened_at, closed_at, opened_by_staff_id, status)
                VALUES (1, NULL, NOW(), NULL, NULL, 'OPEN')
                """).executeUpdate();

        ResultSet rs = c.createStatement().executeQuery("SELECT session_id FROM table_session LIMIT 1");
        rs.next();
        long sessionId = rs.getLong(1);

        PreparedStatement ps = c.prepareStatement("""
                INSERT INTO `orders` (session_id, created_at, status, created_by_staff_id, notes)
                VALUES (?, NOW(), 'CREATED', NULL, 'test')
                """, Statement.RETURN_GENERATED_KEYS);

        ps.setLong(1, sessionId);
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        keys.next();
        return keys.getLong(1);
    }

    @Test
    @Order(1)
    void insert_shouldPersistCardPayment() throws Exception {
        long orderId = seedOrder();

        Payment payment = new Payment(
                0,
                orderId,
                LocalDateTime.now().withNano(0),
                PaymentType.CARD,
                new BigDecimal("50.00"),
                "EUR",
                "1234",
                null
        );

        Payment saved = paymentDao.insert(payment);

        assertNotNull(saved);
        assertTrue(saved.paymentId() > 0);
        assertEquals(PaymentType.CARD, saved.paymentType());
        assertEquals("1234", saved.cardLast4());
    }

    @Test
    @Order(2)
    void findByOrderId_shouldReturnPayment() throws Exception {
        long orderId = seedOrder();

        Payment payment = new Payment(
                0,
                orderId,
                LocalDateTime.now().withNano(0),
                PaymentType.CASH,
                new BigDecimal("20.00"),
                "EUR",
                null,
                null
        );

        Payment saved = paymentDao.insert(payment);

        Payment found = paymentDao.findByOrderId(orderId);

        assertNotNull(found);
        assertEquals(saved.paymentId(), found.paymentId());
        assertEquals(PaymentType.CASH, found.paymentType());
    }

    @Test
    @Order(3)
    void delete_shouldRemovePayment() throws Exception {
        long orderId = seedOrder();

        Payment payment = new Payment(
                0,
                orderId,
                LocalDateTime.now().withNano(0),
                PaymentType.CASH,
                new BigDecimal("10.00"),
                "EUR",
                null,
                null
        );

        Payment saved = paymentDao.insert(payment);

        boolean deleted = paymentDao.delete(saved.paymentId());
        assertTrue(deleted);

        assertNull(paymentDao.findById(saved.paymentId()));
    }
}