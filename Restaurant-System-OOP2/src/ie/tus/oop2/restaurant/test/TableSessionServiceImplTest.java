package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.model.ReservationStatus;
import ie.tus.oop2.restaurant.model.TableSession;
import ie.tus.oop2.restaurant.model.TableSessionStatus;
import ie.tus.oop2.restaurant.service.TableSessionService;
import ie.tus.oop2.restaurant.service.TableSessionServiceImpl;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TableSessionServiceImplTest {

    private TableSessionService service;

    @BeforeAll
    static void verifyDbConnection() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        assertNotNull(c, "Database connection is null");
        assertFalse(c.isClosed(), "Database connection is closed");
    }

    @BeforeEach
    void setUp() {
        // Assumes your service has a no-args constructor and builds its DAOs internally.
        // If your impl requires DAOs in constructor, tell me and I'll adjust instantly.
        service = new TableSessionServiceImpl();
        cleanTables();
    }

    @AfterAll
    static void tearDownAll() {
        DatabaseConnection.closeConnection();
    }

    // ------------------------------------------------------------
    // Cleaning (dependency-safe)
    // ------------------------------------------------------------
    private void cleanTables() {
        try (Connection c = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM receipt")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM payment")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM order_line")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM `orders`")) { ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM table_session")) { ps.executeUpdate(); }
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

    private long insertStaff(String name) {
        String email = "staff_" + UUID.randomUUID() + "@example.com";
        String sql = "INSERT INTO staff (full_name, role, email, active) VALUES (?, 'WAITER', ?, true)";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next(), "No generated key returned for staff insert");
                return keys.getLong(1);
            }

        } catch (SQLException e) {
            fail("Failed to insert staff: " + e.getMessage());
            return -1;
        }
    }

    private long insertReservation(String customer,
                                  int partySize,
                                  LocalDateTime reservedFor,
                                  Integer tableId,
                                  ReservationStatus status) {

        String sql = """
                INSERT INTO reservation (customer_name, phone, party_size, reserved_for, table_id, notes, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, customer);
            ps.setString(2, "+353-" + UUID.randomUUID().toString().substring(0, 8));
            ps.setInt(3, partySize);
            ps.setTimestamp(4, Timestamp.valueOf(reservedFor.withNano(0)));

            if (tableId == null) ps.setNull(5, Types.INTEGER);
            else ps.setInt(5, tableId);

            ps.setNull(6, Types.VARCHAR);         // notes null
            ps.setString(7, status.name());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next(), "No generated key returned for reservation insert");
                return keys.getLong(1);
            }

        } catch (SQLException e) {
            fail("Failed to insert reservation: " + e.getMessage());
            return -1;
        }
    }

    private ReservationStatus loadReservationStatus(long reservationId) {
        String sql = "SELECT status FROM reservation WHERE reservation_id = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Reservation not found for id: " + reservationId);
                return ReservationStatus.valueOf(rs.getString("status"));
            }

        } catch (SQLException e) {
            fail("Failed to load reservation status: " + e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------

    @Test
    @Order(1)
    void openSession_shouldCreateOpenSession() {
        int tableId = 1;
        ensureDiningTableExists(tableId);
        long staffId = insertStaff("Waiter One");

        TableSession s = service.openSession(tableId, staffId);

        assertNotNull(s);
        assertTrue(s.sessionId() > 0);
        assertEquals(tableId, s.tableId());
        assertEquals(TableSessionStatus.OPEN, s.status());
        assertNotNull(s.openedAt());
        assertNull(s.closedAt());
        assertEquals(Long.valueOf(staffId), s.openedByStaffId());
        assertNull(s.reservationId());
    }

    @Test
    @Order(2)
    void openSession_shouldFailIfAlreadyOpenForSameTable() {
        int tableId = 2;
        ensureDiningTableExists(tableId);
        long staffId = insertStaff("Waiter A");

        TableSession first = service.openSession(tableId, staffId);
        assertNotNull(first);

        assertThrows(IllegalStateException.class, () -> service.openSession(tableId, staffId));
    }

    @Test
    @Order(3)
    void closeSession_shouldCloseAndSetClosedAt() {
        int tableId = 3;
        ensureDiningTableExists(tableId);
        long staffId = insertStaff("Waiter Close");

        TableSession open = service.openSession(tableId, staffId);
        TableSession closed = service.closeSession(open.sessionId());

        assertNotNull(closed);
        assertEquals(TableSessionStatus.CLOSED, closed.status());
        assertNotNull(closed.closedAt());
        assertFalse(closed.closedAt().isBefore(closed.openedAt()),
                "closed_at must be >= opened_at");
    }

    @Test
    @Order(4)
    void seatReservation_shouldUpdateReservationToSeated_andCreateSessionLinked() {
        int tableId = 5;
        ensureDiningTableExists(tableId);
        long staffId = insertStaff("Waiter Seat");

        long reservationId = insertReservation(
                "John Reserved",
                4,
                LocalDateTime.now().plusDays(1),
                tableId,
                ReservationStatus.CONFIRMED
        );

        TableSession s = service.seatReservation(reservationId, staffId);

        assertNotNull(s);
        assertEquals(TableSessionStatus.OPEN, s.status());
        assertEquals(tableId, s.tableId());
        assertEquals(Long.valueOf(reservationId), s.reservationId());
        assertEquals(Long.valueOf(staffId), s.openedByStaffId());

        ReservationStatus updated = loadReservationStatus(reservationId);
        assertEquals(ReservationStatus.SEATED, updated);
    }

    @Test
    @Order(5)
    void seatReservation_shouldFailIfReservationCancelledOrNoShow() {
        int tableId = 6;
        ensureDiningTableExists(tableId);
        long staffId = insertStaff("Waiter Block");

        long cancelledId = insertReservation(
                "Cancel",
                2,
                LocalDateTime.now().plusDays(1),
                tableId,
                ReservationStatus.CANCELLED
        );

        assertThrows(IllegalStateException.class, () -> service.seatReservation(cancelledId, staffId));

        long noShowId = insertReservation(
                "NoShow",
                2,
                LocalDateTime.now().plusDays(1),
                tableId,
                ReservationStatus.NO_SHOW
        );

        assertThrows(IllegalStateException.class, () -> service.seatReservation(noShowId, staffId));
    }
}