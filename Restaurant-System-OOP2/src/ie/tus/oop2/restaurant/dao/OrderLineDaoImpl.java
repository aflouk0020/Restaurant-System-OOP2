package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.model.OrderLine;
import ie.tus.oop2.restaurant.model.OrderLineStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderLineDaoImpl implements OrderLineDao {

    private static final String COL_ID = "order_line_id";
    private static final String COL_ORDER_ID = "order_id";
    private static final String COL_MENU_ITEM_ID = "menu_item_id";
    private static final String COL_ITEM_NAME = "item_name_snapshot";
    private static final String COL_UNIT_PRICE = "unit_price_snapshot";
    private static final String COL_QTY = "quantity";
    private static final String COL_STATUS = "line_status";

    @Override
    public OrderLine findById(long orderLineId) {
        String sql = """
                SELECT order_line_id, order_id, menu_item_id, item_name_snapshot, unit_price_snapshot, quantity, line_status
                FROM order_line
                WHERE order_line_id = ?
                """;
        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderLineId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find OrderLine by id: " + orderLineId, e);
        }
    }

    @Override
    public List<OrderLine> findAll() {
        String sql = """
                SELECT order_line_id, order_id, menu_item_id, item_name_snapshot, unit_price_snapshot, quantity, line_status
                FROM order_line
                ORDER BY order_id, order_line_id
                """;
        Connection c = DatabaseConnection.getConnection();
        List<OrderLine> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to findAll OrderLines", e);
        }
    }

    @Override
    public List<OrderLine> findByOrderId(long orderId) {
        String sql = """
                SELECT order_line_id, order_id, menu_item_id, item_name_snapshot, unit_price_snapshot, quantity, line_status
                FROM order_line
                WHERE order_id = ?
                ORDER BY order_line_id
                """;
        Connection c = DatabaseConnection.getConnection();
        List<OrderLine> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find OrderLines by orderId: " + orderId, e);
        }
    }

    @Override
    public OrderLine findByOrderIdAndMenuItemId(long orderId, long menuItemId) {
        String sql = """
                SELECT order_line_id, order_id, menu_item_id, item_name_snapshot, unit_price_snapshot, quantity, line_status
                FROM order_line
                WHERE order_id = ? AND menu_item_id = ?
                """;
        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find OrderLine by (orderId, menuItemId): " + orderId + ", " + menuItemId, e);
        }
    }

    @Override
    public OrderLine insert(OrderLine line) {
        String sql = """
                INSERT INTO order_line (order_id, menu_item_id, item_name_snapshot, unit_price_snapshot, quantity, line_status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, line.orderId());
            ps.setLong(2, line.menuItemId());
            ps.setString(3, line.itemNameSnapshot());
            ps.setBigDecimal(4, line.unitPriceSnapshot());
            ps.setInt(5, line.quantity());

            OrderLineStatus st = (line.lineStatus() == null) ? OrderLineStatus.NEW : line.lineStatus();
            ps.setString(6, st.name());

            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("Insert failed, no rows affected.");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return findById(keys.getLong(1));
                throw new SQLException("Insert failed, no generated key returned.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert OrderLine", e);
        }
    }

    @Override
    public boolean update(OrderLine line) {
        String sql = """
                UPDATE order_line
                SET order_id = ?,
                    menu_item_id = ?,
                    item_name_snapshot = ?,
                    unit_price_snapshot = ?,
                    quantity = ?,
                    line_status = ?
                WHERE order_line_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, line.orderId());
            ps.setLong(2, line.menuItemId());
            ps.setString(3, line.itemNameSnapshot());
            ps.setBigDecimal(4, line.unitPriceSnapshot());
            ps.setInt(5, line.quantity());

            OrderLineStatus st = (line.lineStatus() == null) ? OrderLineStatus.NEW : line.lineStatus();
            ps.setString(6, st.name());

            ps.setLong(7, line.orderLineId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update OrderLine id: " + line.orderLineId(), e);
        }
    }

    @Override
    public boolean delete(long orderLineId) {
        String sql = "DELETE FROM order_line WHERE order_line_id = ?";

        Connection c = DatabaseConnection.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderLineId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete OrderLine id: " + orderLineId, e);
        }
    }

    private OrderLine mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong(COL_ID);
        long orderId = rs.getLong(COL_ORDER_ID);
        long menuItemId = rs.getLong(COL_MENU_ITEM_ID);
        String name = rs.getString(COL_ITEM_NAME);

        BigDecimal price = rs.getBigDecimal(COL_UNIT_PRICE);
        int qty = rs.getInt(COL_QTY);

        OrderLineStatus status = OrderLineStatus.valueOf(rs.getString(COL_STATUS));

        return new OrderLine(id, orderId, menuItemId, name, price, qty, status);
    }
}