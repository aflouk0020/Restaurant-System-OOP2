package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.model.Order;
import ie.tus.oop2.restaurant.model.OrderStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderDaoImpl implements OrderDao {

    private static final String COL_ID = "order_id";
    private static final String COL_SESSION_ID = "session_id";
    private static final String COL_CREATED_AT = "created_at";
    private static final String COL_STATUS = "status";
    private static final String COL_CREATED_BY = "created_by_staff_id";
    private static final String COL_NOTES = "notes";

    @Override
    public Order findById(long orderId) {
        String sql = """
                SELECT order_id, session_id, created_at, status, created_by_staff_id, notes
                FROM orders
                WHERE order_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find order by id: " + orderId, e);
        }
    }

    @Override
    public List<Order> findAll() {
        String sql = """
                SELECT order_id, session_id, created_at, status, created_by_staff_id, notes
                FROM orders
                ORDER BY created_at, order_id
                """;

        Connection c = DatabaseConnection.getConnection();
        List<Order> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to findAll orders", e);
        }
    }

    @Override
    public List<Order> findBySessionId(long sessionId) {
        String sql = """
                SELECT order_id, session_id, created_at, status, created_by_staff_id, notes
                FROM orders
                WHERE session_id = ?
                ORDER BY created_at, order_id
                """;

        Connection c = DatabaseConnection.getConnection();
        List<Order> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, sessionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find orders by sessionId: " + sessionId, e);
        }
    }

    @Override
    public Order insert(Order order) {
        String sql = """
                INSERT INTO orders (session_id, created_at, status, created_by_staff_id, notes)
                VALUES (?, ?, ?, ?, ?)
                """;

        Connection c = DatabaseConnection.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, order.sessionId());
            ps.setTimestamp(2, Timestamp.valueOf(order.createdAt()));
            ps.setString(3, order.status().name());

            if (order.createdByStaffId() == null) ps.setNull(4, Types.BIGINT);
            else ps.setLong(4, order.createdByStaffId());

            ps.setString(5, order.notes());

            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("Insert failed, no rows affected.");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return findById(keys.getLong(1));
                throw new SQLException("Insert failed, no generated key returned.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert order", e);
        }
    }

    @Override
    public boolean update(Order order) {
        String sql = """
                UPDATE orders
                SET session_id = ?, created_at = ?, status = ?, created_by_staff_id = ?, notes = ?
                WHERE order_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, order.sessionId());
            ps.setTimestamp(2, Timestamp.valueOf(order.createdAt()));
            ps.setString(3, order.status().name());

            if (order.createdByStaffId() == null) ps.setNull(4, Types.BIGINT);
            else ps.setLong(4, order.createdByStaffId());

            ps.setString(5, order.notes());
            ps.setLong(6, order.orderId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update order id: " + order.orderId(), e);
        }
    }

    @Override
    public boolean delete(long orderId) {
        String sql = "DELETE FROM orders WHERE order_id = ?";

        Connection c = DatabaseConnection.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete order id: " + orderId, e);
        }
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong(COL_ID);
        long sessionId = rs.getLong(COL_SESSION_ID);

        Timestamp createdTs = rs.getTimestamp(COL_CREATED_AT);
        LocalDateTime createdAt = createdTs.toLocalDateTime();

        OrderStatus status = OrderStatus.valueOf(rs.getString(COL_STATUS));

        long createdBy = rs.getLong(COL_CREATED_BY);
        Long createdByStaffId = rs.wasNull() ? null : createdBy;

        String notes = rs.getString(COL_NOTES);

        return new Order(id, sessionId, createdAt, status, createdByStaffId, notes);
    }
}