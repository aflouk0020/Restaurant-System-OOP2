package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.dao.OrderLineDao;
import ie.tus.oop2.restaurant.dao.OrderLineDaoImpl;
import ie.tus.oop2.restaurant.model.OrderLine;
import ie.tus.oop2.restaurant.model.OrderLineStatus;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderLineDaoImplTest {

    private OrderLineDao orderLineDao;

    @BeforeAll
    static void verifyDbConnection() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        assertNotNull(c);
        assertFalse(c.isClosed());
    }

    @BeforeEach
    void setUp() {
        orderLineDao = new OrderLineDaoImpl();
        cleanTablesDependencySafe();
    }

    @AfterAll
    static void tearDownAll() {
        DatabaseConnection.closeConnection();
    }

    // ------------------------------------------------------------
    // Cleaning (dependency safe)
    // ------------------------------------------------------------
    private void cleanTablesDependencySafe() {
        try (Connection c = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM order_line")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM `orders`")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM table_session")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM menu_item")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM reservation")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM staff")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM dining_table")) { ps.executeUpdate(); }
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
                ON DUPLICATE KEY UPDATE label = VALUES(label), capacity = VALUES(capacity), active = VALUES(active)
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

    private long createSession(int tableId) {
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
            fail("Failed to create table_session: " + e.getMessage());
            return -1;
        }
    }

    private long createOrder(long sessionId) {
        String sql = """
                INSERT INTO `orders` (session_id, created_at, status, created_by_staff_id, notes)
                VALUES (?, ?, 'CREATED', NULL, ?)
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, sessionId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().withNano(0)));
            ps.setString(3, "seed-order");
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            fail("Failed to create order: " + e.getMessage());
            return -1;
        }
    }

    private long createMenuItem(String name) {
        String sql = """
                INSERT INTO menu_item (name, category, price, vegetarian, available)
                VALUES (?, 'MAIN', ?, false, true)
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setBigDecimal(2, new BigDecimal("12.50"));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            fail("Failed to create menu_item: " + e.getMessage());
            return -1;
        }
    }

    private static OrderLine newLine(long orderId, long menuItemId, String nameSnap, BigDecimal priceSnap, int qty, OrderLineStatus st) {
        return new OrderLine(0, orderId, menuItemId, nameSnap, priceSnap, qty, st);
    }

    // ------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------

    @Test
    @Order(1)
    void insert_shouldGenerateId_andPersistFields() {
        long sessionId = createSession(1);
        long orderId = createOrder(sessionId);
        long menuItemId = createMenuItem("Burger");

        OrderLine saved = orderLineDao.insert(newLine(
                orderId,
                menuItemId,
                "Burger",
                new BigDecimal("12.50"),
                2,
                OrderLineStatus.NEW
        ));

        assertNotNull(saved);
        assertTrue(saved.orderLineId() > 0);
        assertEquals(orderId, saved.orderId());
        assertEquals(menuItemId, saved.menuItemId());
        assertEquals("Burger", saved.itemNameSnapshot());
        assertEquals(0, saved.unitPriceSnapshot().compareTo(new BigDecimal("12.50")));
        assertEquals(2, saved.quantity());
        assertEquals(OrderLineStatus.NEW, saved.lineStatus());
    }

    @Test
    @Order(2)
    void findById_shouldReturnInsertedRow() {
        long sessionId = createSession(2);
        long orderId = createOrder(sessionId);
        long menuItemId = createMenuItem("Pasta");

        OrderLine saved = orderLineDao.insert(newLine(orderId, menuItemId, "Pasta", new BigDecimal("12.50"), 1, OrderLineStatus.NEW));
        OrderLine found = orderLineDao.findById(saved.orderLineId());

        assertNotNull(found);
        assertEquals(saved.orderLineId(), found.orderLineId());
        assertEquals("Pasta", found.itemNameSnapshot());
    }

    @Test
    @Order(3)
    void findById_nonExisting_shouldReturnNull() {
        assertNull(orderLineDao.findById(99999999L));
    }

    @Test
    @Order(4)
    void findAll_whenEmpty_shouldReturnEmptyList() {
        List<OrderLine> all = orderLineDao.findAll();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    @Order(5)
    void findByOrderId_shouldReturnOnlyLinesForThatOrder() {
        long sessionId = createSession(3);
        long orderId1 = createOrder(sessionId);
        long orderId2 = createOrder(sessionId);

        long mi1 = createMenuItem("A");
        long mi2 = createMenuItem("B");
        long mi3 = createMenuItem("C");

        orderLineDao.insert(newLine(orderId1, mi1, "A", new BigDecimal("12.50"), 1, OrderLineStatus.NEW));
        orderLineDao.insert(newLine(orderId1, mi2, "B", new BigDecimal("12.50"), 2, OrderLineStatus.NEW));
        orderLineDao.insert(newLine(orderId2, mi3, "C", new BigDecimal("12.50"), 1, OrderLineStatus.NEW));

        List<OrderLine> for1 = orderLineDao.findByOrderId(orderId1);
        assertEquals(2, for1.size());
        assertTrue(for1.stream().allMatch(l -> l.orderId() == orderId1));
    }

    @Test
    @Order(6)
    void findByOrderIdAndMenuItemId_shouldReturnRow() {
        long sessionId = createSession(4);
        long orderId = createOrder(sessionId);
        long menuItemId = createMenuItem("Steak");

        OrderLine saved = orderLineDao.insert(newLine(orderId, menuItemId, "Steak", new BigDecimal("12.50"), 1, OrderLineStatus.NEW));

        OrderLine found = orderLineDao.findByOrderIdAndMenuItemId(orderId, menuItemId);
        assertNotNull(found);
        assertEquals(saved.orderLineId(), found.orderLineId());
    }

    @Test
    @Order(7)
    void update_existing_shouldReturnTrue_andPersistChanges() {
        long sessionId = createSession(5);
        long orderId = createOrder(sessionId);
        long menuItemId = createMenuItem("Pizza");

        OrderLine saved = orderLineDao.insert(newLine(orderId, menuItemId, "Pizza", new BigDecimal("12.50"), 1, OrderLineStatus.NEW));

        OrderLine updated = new OrderLine(
                saved.orderLineId(),
                saved.orderId(),
                saved.menuItemId(),
                saved.itemNameSnapshot(),
                saved.unitPriceSnapshot(),
                3,
                OrderLineStatus.IN_KITCHEN
        );

        boolean ok = orderLineDao.update(updated);
        assertTrue(ok);

        OrderLine reloaded = orderLineDao.findById(saved.orderLineId());
        assertNotNull(reloaded);
        assertEquals(3, reloaded.quantity());
        assertEquals(OrderLineStatus.IN_KITCHEN, reloaded.lineStatus());
    }

    @Test
    @Order(8)
    void update_nonExisting_shouldReturnFalse() {
        long sessionId = createSession(6);
        long orderId = createOrder(sessionId);
        long menuItemId = createMenuItem("GhostItem");

        OrderLine ghost = new OrderLine(
                123456789L,
                orderId,
                menuItemId,
                "GhostItem",
                new BigDecimal("9.99"),
                1,
                OrderLineStatus.NEW
        );

        assertFalse(orderLineDao.update(ghost));
    }

    @Test
    @Order(9)
    void delete_existing_shouldReturnTrue_andRemoveRow() {
        long sessionId = createSession(7);
        long orderId = createOrder(sessionId);
        long menuItemId = createMenuItem("DeleteMe");

        OrderLine saved = orderLineDao.insert(newLine(orderId, menuItemId, "DeleteMe", new BigDecimal("12.50"), 1, OrderLineStatus.NEW));

        boolean ok = orderLineDao.delete(saved.orderLineId());
        assertTrue(ok);

        assertNull(orderLineDao.findById(saved.orderLineId()));
    }

    @Test
    @Order(10)
    void delete_nonExisting_shouldReturnFalse() {
        assertFalse(orderLineDao.delete(99999999L));
    }
}