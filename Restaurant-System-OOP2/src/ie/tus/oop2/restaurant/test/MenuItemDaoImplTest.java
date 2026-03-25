package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.dao.MenuItemDao;
import ie.tus.oop2.restaurant.dao.MenuItemDaoImpl;
import ie.tus.oop2.restaurant.model.MenuCategory;
import ie.tus.oop2.restaurant.model.MenuItem;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MenuItemDaoImplTest {

    private MenuItemDao menuDao;

    @BeforeAll
    static void verifyDbConnection() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        assertNotNull(c, "DatabaseConnection.getConnection() returned null");
        assertFalse(c.isClosed(), "Database connection is closed");
    }

    @BeforeEach
    void setUp() {
        menuDao = new MenuItemDaoImpl();
        cleanMenuItemTable();
    }

    @AfterAll
    static void tearDownAll() {
        DatabaseConnection.closeConnection();
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private void cleanMenuItemTable() {
        // Safe now because order_line references menu_item later.
        // While order_line table is not used yet, wiping is fine.
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM menu_item")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            fail("Failed to clean menu_item table: " + e.getMessage());
        }
    }

    private static MenuItem newItem(String name, MenuCategory category, String price, boolean veg, boolean available) {
        return new MenuItem(
                0,
                name,
                category,
                new BigDecimal(price),
                veg,
                available,
                null
        );
    }

    // ------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------

    @Test
    @Order(1)
    void insert_shouldGenerateId_andCreatedAt() {
        MenuItem saved = menuDao.insert(newItem("Soup of the Day", MenuCategory.STARTER, "6.50", true, true));

        assertNotNull(saved);
        assertTrue(saved.menuItemId() > 0, "Expected generated menuItemId > 0");
        assertEquals("Soup of the Day", saved.name());
        assertEquals(MenuCategory.STARTER, saved.category());
        assertEquals(new BigDecimal("6.50"), saved.price());
        assertTrue(saved.vegetarian());
        assertTrue(saved.available());
        assertNotNull(saved.createdAt(), "Expected createdAt to be populated");
    }

    @Test
    @Order(2)
    void findById_shouldReturnInsertedRow() {
        MenuItem saved = menuDao.insert(newItem("Burger", MenuCategory.MAIN, "12.99", false, true));

        MenuItem found = menuDao.findById(saved.menuItemId());

        assertNotNull(found);
        assertEquals(saved.menuItemId(), found.menuItemId());
        assertEquals("Burger", found.name());
        assertEquals(MenuCategory.MAIN, found.category());
        assertEquals(new BigDecimal("12.99"), found.price());
    }

    @Test
    @Order(3)
    void findById_nonExisting_shouldReturnNull() {
        assertNull(menuDao.findById(99999999L));
    }

    @Test
    @Order(4)
    void findAll_whenEmpty_shouldReturnEmptyList() {
        List<MenuItem> all = menuDao.findAll();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    @Order(5)
    void findAll_shouldReturnAllInsertedRows() {
        menuDao.insert(newItem("A", MenuCategory.STARTER, "5.00", true, true));
        menuDao.insert(newItem("B", MenuCategory.MAIN, "10.00", false, true));
        menuDao.insert(newItem("C", MenuCategory.DRINK, "3.50", true, false));

        List<MenuItem> all = menuDao.findAll();
        assertEquals(3, all.size());
    }

    @Test
    @Order(6)
    void findByCategory_shouldReturnOnlyMatching() {
        menuDao.insert(newItem("Soup", MenuCategory.STARTER, "6.00", true, true));
        menuDao.insert(newItem("Steak", MenuCategory.MAIN, "20.00", false, true));
        menuDao.insert(newItem("Salad", MenuCategory.STARTER, "7.00", true, true));

        List<MenuItem> starters = menuDao.findByCategory(MenuCategory.STARTER);

        assertEquals(2, starters.size());
        assertTrue(starters.stream().allMatch(i -> i.category() == MenuCategory.STARTER));
    }

    @Test
    @Order(7)
    void findAvailableOnly_shouldReturnOnlyAvailableTrue() {
        menuDao.insert(newItem("Coke", MenuCategory.DRINK, "3.00", true, true));
        menuDao.insert(newItem("Old Wine", MenuCategory.DRINK, "9.00", true, false));

        List<MenuItem> available = menuDao.findAvailableOnly();

        assertEquals(1, available.size());
        assertTrue(available.get(0).available());
        assertEquals("Coke", available.get(0).name());
    }

    @Test
    @Order(8)
    void update_existing_shouldReturnTrue_andPersistChanges() {
        MenuItem saved = menuDao.insert(newItem("Pasta", MenuCategory.MAIN, "11.00", true, true));

        MenuItem updated = new MenuItem(
                saved.menuItemId(),
                "Pasta Updated",
                MenuCategory.MAIN,
                new BigDecimal("12.50"),
                true,
                false,
                saved.createdAt()
        );

        boolean ok = menuDao.update(updated);
        assertTrue(ok);

        MenuItem reloaded = menuDao.findById(saved.menuItemId());
        assertNotNull(reloaded);
        assertEquals("Pasta Updated", reloaded.name());
        assertEquals(new BigDecimal("12.50"), reloaded.price());
        assertFalse(reloaded.available());
        assertEquals(saved.createdAt(), reloaded.createdAt(), "createdAt should not change");
    }

    @Test
    @Order(9)
    void update_nonExisting_shouldReturnFalse() {
        MenuItem ghost = new MenuItem(
                123456789L,
                "Ghost",
                MenuCategory.DESSERT,
                new BigDecimal("1.00"),
                true,
                true,
                LocalDateTime.now()
        );

        assertFalse(menuDao.update(ghost));
    }

    @Test
    @Order(10)
    void delete_existing_shouldReturnTrue_andRemoveRow() {
        MenuItem saved = menuDao.insert(newItem("To Delete", MenuCategory.DESSERT, "4.00", true, true));

        boolean ok = menuDao.delete(saved.menuItemId());
        assertTrue(ok);

        assertNull(menuDao.findById(saved.menuItemId()));
    }

    @Test
    @Order(11)
    void delete_nonExisting_shouldReturnFalse() {
        assertFalse(menuDao.delete(99999999L));
    }
}