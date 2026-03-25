package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.model.Reservation;
import ie.tus.oop2.restaurant.model.ReservationStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC DAO implementation for RESERVATION table.
 *
 * Table: reservation
 * Columns:
 *  reservation_id, customer_name, phone, party_size, reserved_for,
 *  table_id, notes, status, created_at
 *
 * Important: DO NOT close the shared connection from DatabaseConnection in DAO methods.
 */
public class ReservationDaoImpl implements ReservationDao {

    private static final String COL_ID = "reservation_id";
    private static final String COL_CUSTOMER = "customer_name";
    private static final String COL_PHONE = "phone";
    private static final String COL_PARTY = "party_size";
    private static final String COL_RESERVED_FOR = "reserved_for";
    private static final String COL_TABLE_ID = "table_id";
    private static final String COL_NOTES = "notes";
    private static final String COL_STATUS = "status";
    private static final String COL_CREATED_AT = "created_at";

    @Override
    public Reservation findById(long reservationId) {
        String sql = """
                SELECT reservation_id, customer_name, phone, party_size, reserved_for,
                       table_id, notes, status, created_at
                FROM reservation
                WHERE reservation_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by id: " + reservationId, e);
        }
    }

    @Override
    public List<Reservation> findAll() {
        String sql = """
                SELECT reservation_id, customer_name, phone, party_size, reserved_for,
                       table_id, notes, status, created_at
                FROM reservation
                ORDER BY reserved_for, reservation_id
                """;

        Connection c = DatabaseConnection.getConnection();
        List<Reservation> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to findAll reservations", e);
        }
    }

    @Override
    public Reservation insert(Reservation reservation) {
        // created_at can be DB default CURRENT_TIMESTAMP
        // status can be DB default 'CREATED' BUT we still send it explicitly for consistency
        String sql = """
                INSERT INTO reservation
                  (customer_name, phone, party_size, reserved_for, table_id, notes, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, reservation.customerName());
            ps.setString(2, reservation.phone());             // nullable ok
            ps.setInt(3, reservation.partySize());

            // LocalDateTime -> Timestamp
            ps.setTimestamp(4, Timestamp.valueOf(reservation.reservedFor()));

            // Nullable FK table_id
            if (reservation.tableId() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, reservation.tableId());
            }

            ps.setString(6, reservation.notes());             // nullable ok

            ReservationStatus status = (reservation.status() == null)
                    ? ReservationStatus.CREATED
                    : reservation.status();
            ps.setString(7, status.name());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Insert failed, no rows affected.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long newId = keys.getLong(1);
                    return findById(newId);
                }
                throw new SQLException("Insert failed, no generated key returned.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }

    @Override
    public boolean update(Reservation reservation) {
        // Do NOT update created_at
        String sql = """
                UPDATE reservation
                SET customer_name = ?,
                    phone = ?,
                    party_size = ?,
                    reserved_for = ?,
                    table_id = ?,
                    notes = ?,
                    status = ?
                WHERE reservation_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, reservation.customerName());
            ps.setString(2, reservation.phone());
            ps.setInt(3, reservation.partySize());
            ps.setTimestamp(4, Timestamp.valueOf(reservation.reservedFor()));

            if (reservation.tableId() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, reservation.tableId());
            }

            ps.setString(6, reservation.notes());

            ReservationStatus status = (reservation.status() == null)
                    ? ReservationStatus.CREATED
                    : reservation.status();
            ps.setString(7, status.name());

            ps.setLong(8, reservation.reservationId());

            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation id: " + reservation.reservationId(), e);
        }
    }

    @Override
    public boolean delete(long reservationId) {
        String sql = "DELETE FROM reservation WHERE reservation_id = ?";

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, reservationId);
            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete reservation id: " + reservationId, e);
        }
    }

    // -------------------------
    // Row mapper
    // -------------------------
    private Reservation mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong(COL_ID);
        String customer = rs.getString(COL_CUSTOMER);
        String phone = rs.getString(COL_PHONE);
        int partySize = rs.getInt(COL_PARTY);

        Timestamp reservedTs = rs.getTimestamp(COL_RESERVED_FOR);
        LocalDateTime reservedFor = (reservedTs == null) ? null : reservedTs.toLocalDateTime();

        // table_id nullable
        int tableIdValue = rs.getInt(COL_TABLE_ID);
        Integer tableId = rs.wasNull() ? null : tableIdValue;

        String notes = rs.getString(COL_NOTES);

        String statusStr = rs.getString(COL_STATUS);
        ReservationStatus status = ReservationStatus.valueOf(statusStr);

        Timestamp createdTs = rs.getTimestamp(COL_CREATED_AT);
        LocalDateTime createdAt = (createdTs == null) ? null : createdTs.toLocalDateTime();

        return new Reservation(
                id,
                customer,
                phone,
                partySize,
                reservedFor,
                tableId,
                notes,
                status,
                createdAt
        );
    }
}