package ie.tus.oop2.restaurant.service;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.dao.TableSessionDao;
import ie.tus.oop2.restaurant.dao.TableSessionDaoImpl;
import ie.tus.oop2.restaurant.model.ReservationStatus;
import ie.tus.oop2.restaurant.model.TableSession;
import ie.tus.oop2.restaurant.model.TableSessionStatus;

import java.sql.*;
import java.time.LocalDateTime;

public class TableSessionServiceImpl implements TableSessionService {

    private final TableSessionDao sessionDao = new TableSessionDaoImpl();

    @Override
    public TableSession openSession(int tableId, Long openedByStaffId) {
        TableSession existing = sessionDao.findOpenByTableId(tableId);
        if (existing != null) {
            throw new IllegalStateException("Table " + tableId + " already has an OPEN session: " + existing.sessionId());
        }

        TableSession toInsert = new TableSession(
                0,
                tableId,
                null,
                LocalDateTime.now().withNano(0),
                null,
                openedByStaffId,
                TableSessionStatus.OPEN
        );
        return sessionDao.insert(toInsert);
    }

    @Override
    public TableSession closeSession(long sessionId) {
        TableSession current = sessionDao.findById(sessionId);
        if (current == null) {
            throw new IllegalArgumentException("No TableSession found for id: " + sessionId);
        }
        if (current.status() == TableSessionStatus.CLOSED) {
            throw new IllegalStateException("Session is already CLOSED: " + sessionId);
        }

        TableSession updated = new TableSession(
                current.sessionId(),
                current.tableId(),
                current.reservationId(),
                current.openedAt(),
                LocalDateTime.now().withNano(0),
                current.openedByStaffId(),
                TableSessionStatus.CLOSED
        );

        boolean ok = sessionDao.update(updated);
        if (!ok) throw new IllegalStateException("Failed to close session: " + sessionId);

        return sessionDao.findById(sessionId);
    }

    @Override
    public TableSession seatReservation(long reservationId, Long openedByStaffId) {
        Connection c = DatabaseConnection.getConnection();

        try {
            c.setAutoCommit(false);

            // 1) Load reservation
            ReservationRow r = loadReservationForSeating(c, reservationId);

            if (r.tableId == null) {
                throw new IllegalStateException("Reservation must have a table assigned before seating.");
            }

            // 2) Check if table already has open session
            TableSession open = sessionDao.findOpenByTableId(r.tableId);
            if (open != null) {
                throw new IllegalStateException("Table " + r.tableId + " already has an OPEN session: " + open.sessionId());
            }

            // 3) Update reservation status -> SEATED
            updateReservationStatus(c, reservationId, ReservationStatus.SEATED);

            // 4) Create new table_session linked to reservation
            TableSession created = insertSessionLinkedToReservation(c, r.tableId, reservationId, openedByStaffId);

            c.commit();
            return created;

        } catch (RuntimeException e) {
            rollbackQuietly(c);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(c);
            throw new RuntimeException("Failed to seat reservation in transaction", e);
        } finally {
            try { c.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    private ReservationRow loadReservationForSeating(Connection c, long reservationId) throws SQLException {
        String sql = "SELECT reservation_id, table_id, status FROM reservation WHERE reservation_id = ?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Reservation not found: " + reservationId);
                }

                Integer tableId = null;
                int tableVal = rs.getInt("table_id");
                if (!rs.wasNull()) tableId = tableVal;

                ReservationStatus status = ReservationStatus.valueOf(rs.getString("status"));

                if (status == ReservationStatus.CANCELLED || status == ReservationStatus.NO_SHOW) {
                    throw new IllegalStateException("Cannot seat reservation in status: " + status);
                }

                return new ReservationRow(tableId, status);
            }
        }
    }

    private void updateReservationStatus(Connection c, long reservationId, ReservationStatus status) throws SQLException {
        String sql = "UPDATE reservation SET status = ? WHERE reservation_id = ?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, reservationId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new IllegalStateException("Failed to update reservation status for id: " + reservationId);
            }
        }
    }

    private TableSession insertSessionLinkedToReservation(Connection c, int tableId, long reservationId, Long openedByStaffId) throws SQLException {
        String insert = """
                INSERT INTO table_session (table_id, reservation_id, opened_at, closed_at, opened_by_staff_id, status)
                VALUES (?, ?, ?, NULL, ?, 'OPEN')
                """;

        try (PreparedStatement ps = c.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tableId);
            ps.setLong(2, reservationId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().withNano(0)));

            if (openedByStaffId == null) ps.setNull(4, Types.BIGINT);
            else ps.setLong(4, openedByStaffId);

            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("Insert session failed, no rows affected.");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Insert session failed, no key.");
                long newId = keys.getLong(1);
                return sessionDao.findById(newId);
            }
        }
    }

    private void rollbackQuietly(Connection c) {
        try { c.rollback(); } catch (SQLException ignored) {}
    }

    private static final class ReservationRow {
        final Integer tableId;
        final ReservationStatus status;

        ReservationRow(Integer tableId, ReservationStatus status) {
            this.tableId = tableId;
            this.status = status;
        }
    }
    
    @Override
    public java.util.List<TableSession> listAllSessions() {
        return sessionDao.findAll();
    }

    @Override
    public java.util.List<TableSession> listOpenSessions() {
        return sessionDao.findAll().stream()
                .filter(s -> s.status() == TableSessionStatus.OPEN)
                .toList();
    }
}