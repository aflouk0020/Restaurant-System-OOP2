package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.dao.StaffDao;
import ie.tus.oop2.restaurant.dao.StaffDaoImpl;
import ie.tus.oop2.restaurant.model.Staff;
import ie.tus.oop2.restaurant.model.StaffRole;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StaffDaoImplTest {

    private StaffDao staffDao;

    @BeforeAll
    static void verifyDbConnection() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        assertNotNull(c);
        assertFalse(c.isClosed());
    }

    @BeforeEach
    void setUp() {
        staffDao = new StaffDaoImpl();
        cleanStaffTable();
    }

    @AfterAll
    static void tearDownAll() {
        DatabaseConnection.closeConnection();
    }

    private void cleanStaffTable() {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM staff")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            fail("Failed to clean staff table: " + e.getMessage());
        }
    }

    private static String uniqueEmail() {
        return "staff_" + UUID.randomUUID() + "@example.com";
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void insert_shouldGenerateId_andCreatedAt() {
        Staff saved = staffDao.insert(new Staff(
                0, "Alice Brown", StaffRole.WAITER, uniqueEmail(), true, null
        ));

        assertNotNull(saved);
        assertTrue(saved.staffId() > 0);
        assertNotNull(saved.createdAt());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void findAll_shouldReturnRows() {
        staffDao.insert(new Staff(0, "A", StaffRole.WAITER, uniqueEmail(), true, null));
        staffDao.insert(new Staff(0, "B", StaffRole.CHEF, uniqueEmail(), true, null));

        List<Staff> all = staffDao.findAll();
        assertEquals(2, all.size());
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void findById_shouldReturnCorrectRow() {
        Staff saved = staffDao.insert(new Staff(
                0, "John", StaffRole.MANAGER, uniqueEmail(), true, null
        ));

        Staff found = staffDao.findById(saved.staffId());
        assertNotNull(found);
        assertEquals(saved.staffId(), found.staffId());
        assertEquals("John", found.fullName());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    void update_shouldPersistChanges() {
        Staff saved = staffDao.insert(new Staff(
                0, "Alice", StaffRole.WAITER, uniqueEmail(), true, null
        ));

        Staff updated = new Staff(
                saved.staffId(), "Alice Updated", StaffRole.MANAGER,
                saved.email(), false, saved.createdAt()
        );

        assertTrue(staffDao.update(updated));

        Staff reloaded = staffDao.findById(saved.staffId());
        assertEquals("Alice Updated", reloaded.fullName());
        assertEquals(StaffRole.MANAGER, reloaded.role());
        assertFalse(reloaded.active());
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    void delete_shouldRemoveRow() {
        Staff saved = staffDao.insert(new Staff(
                0, "DeleteMe", StaffRole.CHEF, uniqueEmail(), true, null
        ));

        assertTrue(staffDao.delete(saved.staffId()));
        assertNull(staffDao.findById(saved.staffId()));
    }
}