package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.model.DiningTable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC DAO implementation for DINING_TABLE table.
 *
 * Table: dining_table
 * Columns: table_id, label, capacity, active
 *
 * Important: DO NOT close the shared connection from DatabaseConnection in DAO methods.
 */
public class DiningTableDaoImpl implements DiningTableDao {

    private static final String COL_ID = "table_id";
    private static final String COL_LABEL = "label";
    private static final String COL_CAPACITY = "capacity";
    private static final String COL_ACTIVE = "active";

    @Override
    public DiningTable findById(int tableId) {
        String sql = """
                SELECT table_id, label, capacity, active
                FROM dining_table
                WHERE table_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, tableId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find dining table by id: " + tableId, e);
        }
    }

    @Override
    public List<DiningTable> findAll() {
        String sql = """
                SELECT table_id, label, capacity, active
                FROM dining_table
                ORDER BY table_id
                """;

        Connection c = DatabaseConnection.getConnection();
        List<DiningTable> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to findAll dining tables", e);
        }
    }

    @Override
    public DiningTable insert(DiningTable table) {
        String sql = """
                INSERT INTO dining_table (table_id, label, capacity, active)
                VALUES (?, ?, ?, ?)
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, table.tableId());
            ps.setString(2, table.label());
            ps.setInt(3, table.capacity());
            ps.setBoolean(4, table.active());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Insert failed, no rows affected.");
            }

            return findById(table.tableId());

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert dining table", e);
        }
    }

    @Override
    public boolean update(DiningTable table) {
        String sql = """
                UPDATE dining_table
                SET label = ?, capacity = ?, active = ?
                WHERE table_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, table.label());
            ps.setInt(2, table.capacity());
            ps.setBoolean(3, table.active());
            ps.setInt(4, table.tableId());

            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update dining table id: " + table.tableId(), e);
        }
    }

    @Override
    public boolean delete(int tableId) {
        String sql = "DELETE FROM dining_table WHERE table_id = ?";

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete dining table id: " + tableId, e);
        }
    }

    private DiningTable mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt(COL_ID);
        String label = rs.getString(COL_LABEL);
        int capacity = rs.getInt(COL_CAPACITY);
        boolean active = rs.getBoolean(COL_ACTIVE);

        return new DiningTable(id, label, capacity, active);
    }
}