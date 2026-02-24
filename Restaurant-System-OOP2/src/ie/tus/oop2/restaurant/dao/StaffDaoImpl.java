package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.model.Staff;
import ie.tus.oop2.restaurant.model.StaffRole;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC DAO implementation for STAFF table.
 *
 * Table: staff
 * Columns: staff_id, full_name, role, email, active, created_at
 *
 * Important: DO NOT close the shared connection from DatabaseConnection in DAO methods.
 */
public class StaffDaoImpl implements StaffDao {

    private static final String COL_ID = "staff_id";
    private static final String COL_NAME = "full_name";
    private static final String COL_ROLE = "role";
    private static final String COL_EMAIL = "email";
    private static final String COL_ACTIVE = "active";
    private static final String COL_CREATED_AT = "created_at";

    @Override
    public List<Staff> findAll() {
        String sql = """
                SELECT staff_id, full_name, role, email, active, created_at
                FROM staff
                ORDER BY staff_id
                """;

        Connection c = DatabaseConnection.getConnection();
        List<Staff> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to findAll staff", e);
        }
    }

    @Override
    public Staff findById(long staffId) {
        String sql = """
                SELECT staff_id, full_name, role, email, active, created_at
                FROM staff
                WHERE staff_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, staffId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find staff by id: " + staffId, e);
        }
    }

    @Override
    public Staff insert(Staff staff) {
        // created_at comes from DB default CURRENT_TIMESTAMP
        String sql = """
                INSERT INTO staff (full_name, role, email, active)
                VALUES (?, ?, ?, ?)
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, staff.fullName());
            ps.setString(2, staff.role().name());           // enum -> DB enum string
            ps.setString(3, staff.email());                 // nullable ok
            ps.setBoolean(4, staff.active());

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
            throw new RuntimeException("Failed to insert staff", e);
        }
    }

    @Override
    public boolean update(Staff staff) {
        // Do NOT update created_at
        String sql = """
                UPDATE staff
                SET full_name = ?, role = ?, email = ?, active = ?
                WHERE staff_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, staff.fullName());
            ps.setString(2, staff.role().name());
            ps.setString(3, staff.email());
            ps.setBoolean(4, staff.active());
            ps.setLong(5, staff.staffId());

            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update staff id: " + staff.staffId(), e);
        }
    }

    @Override
    public boolean delete(long staffId) {
        String sql = "DELETE FROM staff WHERE staff_id = ?";

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete staff id: " + staffId, e);
        }
    }

    // -------------------------
    // Row mapper
    // -------------------------
    private Staff mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong(COL_ID);
        String name = rs.getString(COL_NAME);

        String roleStr = rs.getString(COL_ROLE);
        StaffRole role = StaffRole.valueOf(roleStr);

        String email = rs.getString(COL_EMAIL); // returns null if SQL NULL
        boolean active = rs.getBoolean(COL_ACTIVE);

        Timestamp ts = rs.getTimestamp(COL_CREATED_AT);
        LocalDateTime createdAt = (ts == null) ? null : ts.toLocalDateTime();

        return new Staff(id, name, role, email, active, createdAt);
    }
}