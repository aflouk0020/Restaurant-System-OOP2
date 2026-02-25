package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.dao.OrderDao;
import ie.tus.oop2.restaurant.dao.OrderDaoImpl;
import ie.tus.oop2.restaurant.model.Order;
import ie.tus.oop2.restaurant.model.OrderStatus;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderDaoImplTest {

    private OrderDao orderDao;

    @BeforeAll
    static void verifyDbConnection() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        assertNotNull(c, "DatabaseConnection.getConnection() returned null");
        assertFalse(c.isClosed(), "Database connection is closed");
    }

    @BeforeEach
    void setUp() {
        orderDao = new OrderDaoImpl();
        cleanOrdersTable(); // clean ONLY orders (safe at this stage)
    }

    @AfterAll
    static void tearDownAll() {
        DatabaseConnection.closeConnection();
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private void cleanOrdersTable() {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM orders")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            fail("Failed to clean orders table: " + e.getMessage());
        }
    }

    private void ensureDiningTableExists(int tableId) {
        String sql = """
                INSERT INTO dining_table (table_id, label, capacity, active)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    label = VALUES(label),
                    capacity = VALUES(capacity),
                    active = VALUES(active)
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, tableId);
            ps.setString(2, "T" + tableId);
            ps.setInt(3, 4);
            ps.setBoolean(4, true);
            ps.executeUpdate();

        } catch (SQLException e) {
            fail("Failed to seed dining_table(" + tableId + "): " + e.getMessage());
        }
    }

    private long createTableSession(int tableId) {
        ensureDiningTableExists(tableId);

        String sql = """
                INSERT INTO table_session (table_id, reservation_id, opened_at, closed_at, opened_by_staff_id, status)
                VALUES (?, NULL, ?, NULL, NULL, 'OPEN')
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, tableId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().withNano(0)));

            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("Creating table_session failed (0 rows).");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("Creating table_session failed (no generated key).");
            }

        } catch (SQLException e) {
            fail("Failed to create table_session: " + e.getMessage());
            return -1; // unreachable because fail() throws
        }
    }

    private static Order newOrder(long sessionId, OrderStatus status, Long createdByStaffId, String notes) {
        return new Order(
                0,
                sessionId,
                LocalDateTime.now().withNano(0),
                status,
                createdByStaffId,
                notes
        );
    }

    // ------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------

    @Test
    @org.junit.jupiter.api.Order(1)
    void insert_shouldGenerateId_andPersistFields() {
        long sessionId = createTableSession(1);

        Order input = newOrder(sessionId, OrderStatus.CREATED, null, "First order");
        Order saved = orderDao.insert(input);

        assertNotNull(saved);
        assertTrue(saved.orderId() > 0, "Expected generated orderId > 0");
        assertEquals(sessionId, saved.sessionId());
        assertEquals(OrderStatus.CREATED, saved.status());
        assertEquals("First order", saved.notes());
        assertNotNull(saved.createdAt());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void findById_shouldReturnInsertedRow() {
        long sessionId = createTableSession(2);

        Order saved = orderDao.insert(newOrder(sessionId, OrderStatus.CREATED, null, "Find me"));
        Order found = orderDao.findById(saved.orderId());

        assertNotNull(found);
        assertEquals(saved.orderId(), found.orderId());
        assertEquals(sessionId, found.sessionId());
        assertEquals(OrderStatus.CREATED, found.status());
        assertEquals("Find me", found.notes());
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void findById_nonExisting_shouldReturnNull() {
        Order found = orderDao.findById(99999999L);
        assertNull(found);
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    void findAll_whenEmpty_shouldReturnEmptyList() {
        List<Order> all = orderDao.findAll();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    void findAll_shouldReturnAllInsertedRows() {
        long session1 = createTableSession(3);
        long session2 = createTableSession(4);

        orderDao.insert(newOrder(session1, OrderStatus.CREATED, null, "A"));
        orderDao.insert(newOrder(session1, OrderStatus.SUBMITTED, null, "B"));
        orderDao.insert(newOrder(session2, OrderStatus.CREATED, null, "C"));

        List<Order> all = orderDao.findAll();
        assertEquals(3, all.size());
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    void findBySessionId_shouldReturnOnlySessionOrders() {
        long session1 = createTableSession(5);
        long session2 = createTableSession(6);

        orderDao.insert(newOrder(session1, OrderStatus.CREATED, null, "S1-A"));
        orderDao.insert(newOrder(session1, OrderStatus.SUBMITTED, null, "S1-B"));
        orderDao.insert(newOrder(session2, OrderStatus.CREATED, null, "S2-A"));

        List<Order> s1Orders = orderDao.findBySessionId(session1);
        assertEquals(2, s1Orders.size());
        assertTrue(s1Orders.stream().allMatch(o -> o.sessionId() == session1));
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    void update_existing_shouldReturnTrue_andPersistChanges() {
        long sessionId = createTableSession(7);

        Order saved = orderDao.insert(newOrder(sessionId, OrderStatus.CREATED, null, "Before"));
        Order updated = new Order(
                saved.orderId(),
                saved.sessionId(),
                saved.createdAt(),          // keep createdAt
                OrderStatus.SUBMITTED,
                saved.createdByStaffId(),
                "After"
        );

        boolean ok = orderDao.update(updated);
        assertTrue(ok);

        Order reloaded = orderDao.findById(saved.orderId());
        assertNotNull(reloaded);
        assertEquals(OrderStatus.SUBMITTED, reloaded.status());
        assertEquals("After", reloaded.notes());
        assertEquals(saved.createdAt(), reloaded.createdAt());
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    void update_nonExisting_shouldReturnFalse() {
        long sessionId = createTableSession(8);

        Order ghost = new Order(
                123456789L,
                sessionId,
                LocalDateTime.now().withNano(0),
                OrderStatus.CREATED,
                null,
                "Ghost"
        );

        boolean ok = orderDao.update(ghost);
        assertFalse(ok);
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    void delete_existing_shouldReturnTrue_andRemoveRow() {
        long sessionId = createTableSession(9);

        Order saved = orderDao.insert(newOrder(sessionId, OrderStatus.CREATED, null, "To Delete"));

        boolean ok = orderDao.delete(saved.orderId());
        assertTrue(ok);

        assertNull(orderDao.findById(saved.orderId()));
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    void delete_nonExisting_shouldReturnFalse() {
        boolean ok = orderDao.delete(99999999L);
        assertFalse(ok);
    }
}