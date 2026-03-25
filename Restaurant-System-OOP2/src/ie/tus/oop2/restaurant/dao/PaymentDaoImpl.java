package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.model.Payment;
import ie.tus.oop2.restaurant.model.PaymentType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentDaoImpl implements PaymentDao {

    private static final String COL_ID = "payment_id";
    private static final String COL_ORDER_ID = "order_id";
    private static final String COL_PAID_AT = "paid_at";
    private static final String COL_TYPE = "payment_type";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_CURRENCY = "currency";
    private static final String COL_CARD_LAST4 = "card_last4";
    private static final String COL_VOUCHER = "voucher_code";

    @Override
    public Payment findById(long paymentId) {
        String sql = """
                SELECT payment_id, order_id, paid_at, payment_type,
                       amount, currency, card_last4, voucher_code
                FROM payment
                WHERE payment_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, paymentId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find Payment by id: " + paymentId, e);
        }
    }

    @Override
    public Payment findByOrderId(long orderId) {
        String sql = """
                SELECT payment_id, order_id, paid_at, payment_type,
                       amount, currency, card_last4, voucher_code
                FROM payment
                WHERE order_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find Payment by orderId: " + orderId, e);
        }
    }

    @Override
    public List<Payment> findAll() {
        String sql = """
                SELECT payment_id, order_id, paid_at, payment_type,
                       amount, currency, card_last4, voucher_code
                FROM payment
                ORDER BY paid_at, payment_id
                """;

        Connection c = DatabaseConnection.getConnection();
        List<Payment> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to findAll Payments", e);
        }
    }

    @Override
    public Payment insert(Payment payment) {
        String sql = """
                INSERT INTO payment (order_id, paid_at, payment_type,
                                     amount, currency, card_last4, voucher_code)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, payment.orderId());
            ps.setTimestamp(2, Timestamp.valueOf(payment.paidAt()));
            ps.setString(3, payment.paymentType().name());
            ps.setBigDecimal(4, payment.amount());
            ps.setString(5, payment.currency());

            if (payment.cardLast4() == null)
                ps.setNull(6, Types.CHAR);
            else
                ps.setString(6, payment.cardLast4());

            if (payment.voucherCode() == null)
                ps.setNull(7, Types.VARCHAR);
            else
                ps.setString(7, payment.voucherCode());

            int affected = ps.executeUpdate();
            if (affected == 0)
                throw new SQLException("Insert failed, no rows affected.");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1));
                }
                throw new SQLException("Insert failed, no generated key returned.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert Payment", e);
        }
    }

    @Override
    public boolean delete(long paymentId) {
        String sql = "DELETE FROM payment WHERE payment_id = ?";

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, paymentId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete Payment id: " + paymentId, e);
        }
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong(COL_ID);
        long orderId = rs.getLong(COL_ORDER_ID);

        Timestamp ts = rs.getTimestamp(COL_PAID_AT);
        LocalDateTime paidAt = ts.toLocalDateTime();

        PaymentType type = PaymentType.valueOf(rs.getString(COL_TYPE));

        BigDecimal amount = rs.getBigDecimal(COL_AMOUNT);
        String currency = rs.getString(COL_CURRENCY);

        String cardLast4 = rs.getString(COL_CARD_LAST4);
        if (rs.wasNull()) cardLast4 = null;

        String voucher = rs.getString(COL_VOUCHER);
        if (rs.wasNull()) voucher = null;

        return new Payment(id, orderId, paidAt, type, amount, currency, cardLast4, voucher);
    }
}