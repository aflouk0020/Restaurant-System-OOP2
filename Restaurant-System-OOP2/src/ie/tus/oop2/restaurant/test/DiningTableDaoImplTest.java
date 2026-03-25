package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.dao.DiningTableDao;
import ie.tus.oop2.restaurant.dao.DiningTableDaoImpl;
import ie.tus.oop2.restaurant.model.DiningTable;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DiningTableDaoImplTest {

    private DiningTableDao tableDao;

    @BeforeAll
    static void verifyDbConnection() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        assertNotNull(c, "DatabaseConnection.getConnection() returned null");
        assertFalse(c.isClosed(), "Database connection is closed");
    }

    @BeforeEach
    void setUp() {
        tableDao = new DiningTableDaoImpl();
        cleanDiningTable();
    }

    @AfterAll
    static void tearDownAll() {
        DatabaseConnection.closeConnection();
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private void cleanDiningTable() {
        // Dining table is referenced by reservation + table_session.
        // For now safe to wipe if those tables are not implemented yet.
        // If you add FKs later, switch to deleting only inserted IDs.
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM dining_table")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            fail("Failed to clean dining_table: " + e.getMessage());
        }
    }

    private static DiningTable newTable(int id, String label, int cap, boolean active) {
        return new DiningTable(id, label, cap, active);
    }

    // ------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------

    @Test
    @Order(1)
    void insert_shouldCreateRow() {
        DiningTable t = newTable(1, "T1", 4, true);

        DiningTable saved = tableDao.insert(t);

        assertNotNull(saved);
        assertEquals(1, saved.tableId());
        assertEquals("T1", saved.label());
        assertEquals(4, saved.capacity());
        assertTrue(saved.active());
    }

    @Test
    @Order(2)
    void findById_shouldReturnInsertedRow() {
        tableDao.insert(newTable(2, "Window-2", 2, true));

        DiningTable found = tableDao.findById(2);

        assertNotNull(found);
        assertEquals(2, found.tableId());
        assertEquals("Window-2", found.label());
        assertEquals(2, found.capacity());
        assertTrue(found.active());
    }

    @Test
    @Order(3)
    void findById_nonExisting_shouldReturnNull() {
        DiningTable found = tableDao.findById(999);
        assertNull(found);
    }

    @Test
    @Order(4)
    void findAll_whenEmpty_shouldReturnEmptyList() {
        List<DiningTable> all = tableDao.findAll();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    @Order(5)
    void findAll_shouldReturnAllInsertedRows() {
        tableDao.insert(newTable(1, "T1", 4, true));
        tableDao.insert(newTable(2, "T2", 2, true));
        tableDao.insert(newTable(3, "T3", 6, false));

        List<DiningTable> all = tableDao.findAll();

        assertEquals(3, all.size());
    }

    @Test
    @Order(6)
    void update_existing_shouldReturnTrue_andPersistChanges() {
        tableDao.insert(newTable(5, "T5", 4, true));

        DiningTable updated = newTable(5, "T5-Updated", 8, false);

        boolean ok = tableDao.update(updated);
        assertTrue(ok);

        DiningTable reloaded = tableDao.findById(5);
        assertNotNull(reloaded);
        assertEquals("T5-Updated", reloaded.label());
        assertEquals(8, reloaded.capacity());
        assertFalse(reloaded.active());
    }

    @Test
    @Order(7)
    void update_nonExisting_shouldReturnFalse() {
        boolean ok = tableDao.update(newTable(123, "Ghost", 2, true));
        assertFalse(ok);
    }

    @Test
    @Order(8)
    void delete_existing_shouldReturnTrue_andRemoveRow() {
        tableDao.insert(newTable(10, "T10", 4, true));

        boolean ok = tableDao.delete(10);
        assertTrue(ok);

        assertNull(tableDao.findById(10));
    }

    @Test
    @Order(9)
    void delete_nonExisting_shouldReturnFalse() {
        boolean ok = tableDao.delete(999);
        assertFalse(ok);
    }
}