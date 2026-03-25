package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.dao.ReservationDao;
import ie.tus.oop2.restaurant.dao.ReservationDaoImpl;
import ie.tus.oop2.restaurant.model.Reservation;
import ie.tus.oop2.restaurant.model.ReservationStatus;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationDaoImplTest {

    private ReservationDao reservationDao;

    @BeforeAll
    static void verifyDbConnection() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        assertNotNull(c, "DatabaseConnection.getConnection() returned null");
        assertFalse(c.isClosed(), "Database connection is closed");
    }

    @BeforeEach
    void setUp() {
        reservationDao = new ReservationDaoImpl();
        cleanReservationTable();
    }

    @AfterAll
    static void tearDownAll() {
        DatabaseConnection.closeConnection();
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private void cleanReservationTable() {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM reservation")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            fail("Failed to clean reservation table: " + e.getMessage());
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

    private static String uniquePhone() {
        return "+353-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static Reservation newReservation(
            String customerName,
            String phone,
            int partySize,
            LocalDateTime reservedFor,
            Integer tableId,
            String notes,
            ReservationStatus status
    ) {
        return new Reservation(
                0,
                customerName,
                phone,
                partySize,
                reservedFor,
                tableId,
                notes,
                status,
                null
        );
    }

    // ------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------

    @Test
    @Order(1)
    void insert_shouldGenerateId_andCreatedAt() {
        Reservation input = newReservation(
                "John Booking",
                uniquePhone(),
                4,
                LocalDateTime.now().plusDays(1).withNano(0),
                null,
                "Window seat pls",
                ReservationStatus.CREATED
        );

        Reservation saved = reservationDao.insert(input);

        assertNotNull(saved);
        assertTrue(saved.reservationId() > 0, "Expected generated reservationId > 0");
        assertEquals("John Booking", saved.customerName());
        assertEquals(4, saved.partySize());
        assertNotNull(saved.createdAt(), "Expected createdAt to be populated");
        assertEquals(ReservationStatus.CREATED, saved.status());
        assertNull(saved.tableId(), "tableId should be null if not assigned");
    }

    @Test
    @Order(2)
    void insert_shouldAllowNullPhone_andNullNotes() {
        Reservation input = newReservation(
                "No Phone",
                null,
                2,
                LocalDateTime.now().plusHours(5).withNano(0),
                null,
                null,
                ReservationStatus.CREATED
        );

        Reservation saved = reservationDao.insert(input);

        assertNotNull(saved);
        assertTrue(saved.reservationId() > 0);
        assertNull(saved.phone());
        assertNull(saved.notes());
    }

    @Test
    @Order(3)
    void insert_shouldAllowAssignedTableId() {
        ensureDiningTableExists(5);

        Reservation input = newReservation(
                "Assigned Table",
                uniquePhone(),
                3,
                LocalDateTime.now().plusDays(2).withNano(0),
                5,
                null,
                ReservationStatus.CONFIRMED
        );

        Reservation saved = reservationDao.insert(input);

        assertNotNull(saved);
        assertEquals(Integer.valueOf(5), saved.tableId());
        assertEquals(ReservationStatus.CONFIRMED, saved.status());
    }

    @Test
    @Order(4)
    void findById_shouldReturnInsertedRow() {
        Reservation saved = reservationDao.insert(newReservation(
                "Find Me",
                uniquePhone(),
                6,
                LocalDateTime.now().plusDays(3).withNano(0),
                null,
                "Birthday",
                ReservationStatus.CREATED
        ));

        Reservation found = reservationDao.findById(saved.reservationId());

        assertNotNull(found);
        assertEquals(saved.reservationId(), found.reservationId());
        assertEquals("Find Me", found.customerName());
        assertEquals(6, found.partySize());
        assertEquals(saved.status(), found.status());
    }

    @Test
    @Order(5)
    void findById_nonExisting_shouldReturnNull() {
        Reservation found = reservationDao.findById(99999999L);
        assertNull(found);
    }

    @Test
    @Order(6)
    void findAll_whenEmpty_shouldReturnEmptyList() {
        List<Reservation> all = reservationDao.findAll();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    @Order(7)
    void findAll_shouldReturnAllInsertedRows() {
        ensureDiningTableExists(3);

        reservationDao.insert(newReservation(
                "A",
                uniquePhone(),
                2,
                LocalDateTime.now().plusDays(1).withNano(0),
                null,
                null,
                ReservationStatus.CREATED
        ));
        reservationDao.insert(newReservation(
                "B",
                uniquePhone(),
                4,
                LocalDateTime.now().plusDays(2).withNano(0),
                3,
                "VIP",
                ReservationStatus.CONFIRMED
        ));
        reservationDao.insert(newReservation(
                "C",
                uniquePhone(),
                1,
                LocalDateTime.now().plusDays(3).withNano(0),
                null,
                null,
                ReservationStatus.CANCELLED
        ));

        List<Reservation> all = reservationDao.findAll();
        assertEquals(3, all.size());
    }

    @Test
    @Order(8)
    void update_existing_shouldReturnTrue_andPersistChanges() {
        ensureDiningTableExists(7);

        Reservation saved = reservationDao.insert(newReservation(
                "Update Me",
                uniquePhone(),
                2,
                LocalDateTime.now().plusDays(1).withNano(0),
                null,
                null,
                ReservationStatus.CREATED
        ));

        Reservation updated = new Reservation(
                saved.reservationId(),
                "Update Me (Edited)",
                saved.phone(),
                5,
                saved.reservedFor().plusHours(1),
                7,
                "Updated notes",
                ReservationStatus.CONFIRMED,
                saved.createdAt()
        );

        boolean ok = reservationDao.update(updated);
        assertTrue(ok);

        Reservation reloaded = reservationDao.findById(saved.reservationId());
        assertNotNull(reloaded);
        assertEquals("Update Me (Edited)", reloaded.customerName());
        assertEquals(5, reloaded.partySize());
        assertEquals(Integer.valueOf(7), reloaded.tableId());
        assertEquals("Updated notes", reloaded.notes());
        assertEquals(ReservationStatus.CONFIRMED, reloaded.status());
        assertEquals(saved.createdAt(), reloaded.createdAt(), "createdAt should not change");
    }

    @Test
    @Order(9)
    void update_nonExisting_shouldReturnFalse() {
        Reservation ghost = new Reservation(
                123456789L,
                "Ghost",
                uniquePhone(),
                2,
                LocalDateTime.now().plusDays(1).withNano(0),
                null,
                null,
                ReservationStatus.CREATED,
                LocalDateTime.now()
        );

        boolean ok = reservationDao.update(ghost);
        assertFalse(ok);
    }

    @Test
    @Order(10)
    void delete_existing_shouldReturnTrue_andRemoveRow() {
        Reservation saved = reservationDao.insert(newReservation(
                "To Delete",
                uniquePhone(),
                2,
                LocalDateTime.now().plusDays(1).withNano(0),
                null,
                null,
                ReservationStatus.CREATED
        ));

        boolean ok = reservationDao.delete(saved.reservationId());
        assertTrue(ok);

        assertNull(reservationDao.findById(saved.reservationId()));
    }

    @Test
    @Order(11)
    void delete_nonExisting_shouldReturnFalse() {
        boolean ok = reservationDao.delete(99999999L);
        assertFalse(ok);
    }
}