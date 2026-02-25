package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.model.Receipt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReceiptDaoImpl implements ReceiptDao {

    private static final String COL_ID = "receipt_id";
    private static final String COL_ORDER_ID = "order_id";
    private static final String COL_PAYMENT_ID = "payment_id";
    private static final String COL_SUBTOTAL = "subtotal";
    private static final String COL_TAX = "tax";
    private static final String COL_TOTAL = "total";
    private static final String COL_GENERATED_AT = "generated_at";

    @Override
    public Receipt findById(long receiptId) {
        String sql = """
                SELECT receipt_id, order_id, payment_id, subtotal, tax, total, generated_at
                FROM receipt
                WHERE receipt_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, receiptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find receipt by id: " + receiptId, e);
        }
    }

    @Override
    public Receipt findByOrderId(long orderId) {
        String sql = """
                SELECT receipt_id, order_id, payment_id, subtotal, tax, total, generated_at
                FROM receipt
                WHERE order_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find receipt by orderId: " + orderId, e);
        }
    }

    @Override
    public Receipt findByPaymentId(long paymentId) {
        String sql = """
                SELECT receipt_id, order_id, payment_id, subtotal, tax, total, generated_at
                FROM receipt
                WHERE payment_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find receipt by paymentId: " + paymentId, e);
        }
    }

    @Override
    public List<Receipt> findAll() {
        String sql = """
                SELECT receipt_id, order_id, payment_id, subtotal, tax, total, generated_at
                FROM receipt
                ORDER BY generated_at DESC, receipt_id DESC
                """;

        Connection c = DatabaseConnection.getConnection();
        List<Receipt> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to findAll receipts", e);
        }
    }

    @Override
    public Receipt insert(Receipt receipt) {
        String sql = """
                INSERT INTO receipt (order_id, payment_id, subtotal, tax, total, generated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        Connection c = DatabaseConnection.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, receipt.orderId());
            ps.setLong(2, receipt.paymentId());
            ps.setBigDecimal(3, receipt.subtotal());
            ps.setBigDecimal(4, receipt.tax());
            ps.setBigDecimal(5, receipt.total());
            ps.setTimestamp(6, Timestamp.valueOf(receipt.generatedAt()));

            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("Insert failed, no rows affected.");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return findById(id);
                }
                throw new SQLException("Insert failed, no generated key returned.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert receipt", e);
        }
    }

    @Override
    public boolean delete(long receiptId) {
        String sql = "DELETE FROM receipt WHERE receipt_id = ?";

        Connection c = DatabaseConnection.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, receiptId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete receipt id: " + receiptId, e);
        }
    }

    private Receipt mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong(COL_ID);
        long orderId = rs.getLong(COL_ORDER_ID);
        long paymentId = rs.getLong(COL_PAYMENT_ID);

        var subtotal = rs.getBigDecimal(COL_SUBTOTAL);
        var tax = rs.getBigDecimal(COL_TAX);
        var total = rs.getBigDecimal(COL_TOTAL);

        Timestamp genTs = rs.getTimestamp(COL_GENERATED_AT);
        LocalDateTime generatedAt = (genTs == null) ? null : genTs.toLocalDateTime();

        return new Receipt(id, orderId, paymentId, subtotal, tax, total, generatedAt);
    }
}