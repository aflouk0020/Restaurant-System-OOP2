package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.model.TableSession;
import ie.tus.oop2.restaurant.model.TableSessionStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TableSessionDaoImpl implements TableSessionDao {

    private static final String COL_ID = "session_id";
    private static final String COL_TABLE_ID = "table_id";
    private static final String COL_RES_ID = "reservation_id";
    private static final String COL_OPENED_AT = "opened_at";
    private static final String COL_CLOSED_AT = "closed_at";
    private static final String COL_OPENED_BY = "opened_by_staff_id";
    private static final String COL_STATUS = "status";

    @Override
    public TableSession findById(long sessionId) {
        String sql = """
                SELECT session_id, table_id, reservation_id, opened_at, closed_at, opened_by_staff_id, status
                FROM table_session
                WHERE session_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find TableSession by id: " + sessionId, e);
        }
    }

    @Override
    public List<TableSession> findAll() {
        String sql = """
                SELECT session_id, table_id, reservation_id, opened_at, closed_at, opened_by_staff_id, status
                FROM table_session
                ORDER BY opened_at DESC, session_id DESC
                """;

        Connection c = DatabaseConnection.getConnection();
        List<TableSession> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to findAll TableSessions", e);
        }
    }

    @Override
    public TableSession insert(TableSession session) {
        String sql = """
                INSERT INTO table_session (table_id, reservation_id, opened_at, closed_at, opened_by_staff_id, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, session.tableId());

            if (session.reservationId() == null) ps.setNull(2, Types.BIGINT);
            else ps.setLong(2, session.reservationId());

            ps.setTimestamp(3, Timestamp.valueOf(session.openedAt()));

            if (session.closedAt() == null) ps.setNull(4, Types.TIMESTAMP);
            else ps.setTimestamp(4, Timestamp.valueOf(session.closedAt()));

            if (session.openedByStaffId() == null) ps.setNull(5, Types.BIGINT);
            else ps.setLong(5, session.openedByStaffId());

            ps.setString(6, session.status().name());

            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("Insert failed, no rows affected.");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long newId = keys.getLong(1);
                    return findById(newId);
                }
                throw new SQLException("Insert failed, no generated key returned.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert TableSession", e);
        }
    }

    @Override
    public boolean update(TableSession session) {
        String sql = """
                UPDATE table_session
                SET table_id = ?,
                    reservation_id = ?,
                    opened_at = ?,
                    closed_at = ?,
                    opened_by_staff_id = ?,
                    status = ?
                WHERE session_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, session.tableId());

            if (session.reservationId() == null) ps.setNull(2, Types.BIGINT);
            else ps.setLong(2, session.reservationId());

            ps.setTimestamp(3, Timestamp.valueOf(session.openedAt()));

            if (session.closedAt() == null) ps.setNull(4, Types.TIMESTAMP);
            else ps.setTimestamp(4, Timestamp.valueOf(session.closedAt()));

            if (session.openedByStaffId() == null) ps.setNull(5, Types.BIGINT);
            else ps.setLong(5, session.openedByStaffId());

            ps.setString(6, session.status().name());
            ps.setLong(7, session.sessionId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update TableSession id: " + session.sessionId(), e);
        }
    }

    @Override
    public boolean delete(long sessionId) {
        String sql = "DELETE FROM table_session WHERE session_id = ?";

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete TableSession id: " + sessionId, e);
        }
    }

    @Override
    public TableSession findOpenByTableId(int tableId) {
        String sql = """
                SELECT session_id, table_id, reservation_id, opened_at, closed_at, opened_by_staff_id, status
                FROM table_session
                WHERE table_id = ? AND status = 'OPEN'
                ORDER BY opened_at DESC
                LIMIT 1
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find OPEN TableSession for table_id: " + tableId, e);
        }
    }

    @Override
    public TableSession findByReservationId(long reservationId) {
        String sql = """
                SELECT session_id, table_id, reservation_id, opened_at, closed_at, opened_by_staff_id, status
                FROM table_session
                WHERE reservation_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find TableSession by reservationId: " + reservationId, e);
        }
    }

    private TableSession mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong(COL_ID);
        int tableId = rs.getInt(COL_TABLE_ID);

        long resIdVal = rs.getLong(COL_RES_ID);
        Long reservationId = rs.wasNull() ? null : resIdVal;

        Timestamp openedTs = rs.getTimestamp(COL_OPENED_AT);
        LocalDateTime openedAt = openedTs == null ? null : openedTs.toLocalDateTime();

        Timestamp closedTs = rs.getTimestamp(COL_CLOSED_AT);
        LocalDateTime closedAt = closedTs == null ? null : closedTs.toLocalDateTime();

        long staffIdVal = rs.getLong(COL_OPENED_BY);
        Long openedBy = rs.wasNull() ? null : staffIdVal;

        TableSessionStatus status = TableSessionStatus.valueOf(rs.getString(COL_STATUS));

        return new TableSession(id, tableId, reservationId, openedAt, closedAt, openedBy, status);
    }
}