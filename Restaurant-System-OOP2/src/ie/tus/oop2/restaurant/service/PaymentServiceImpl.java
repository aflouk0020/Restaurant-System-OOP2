package ie.tus.oop2.restaurant.service;

import ie.tus.oop2.restaurant.dao.OrderDao;
import ie.tus.oop2.restaurant.dao.OrderDaoImpl;
import ie.tus.oop2.restaurant.dao.PaymentDao;
import ie.tus.oop2.restaurant.dao.PaymentDaoImpl;
import ie.tus.oop2.restaurant.model.Order;
import ie.tus.oop2.restaurant.model.OrderStatus;
import ie.tus.oop2.restaurant.model.Payment;
import ie.tus.oop2.restaurant.model.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentServiceImpl implements PaymentService {

    private final PaymentDao paymentDao = new PaymentDaoImpl();
    private final OrderDao orderDao = new OrderDaoImpl();

    @Override
    public Payment pay(long orderId,
                       PaymentType type,
                       BigDecimal amount,
                       String currency,
                       String cardLast4,
                       String voucherCode) {

        if (paymentDao.findByOrderId(orderId) != null) {
            throw new IllegalStateException("Order already paid");
        }

        Order existingOrder = orderDao.findById(orderId);
        if (existingOrder == null) {
            throw new IllegalStateException("Order not found");
        }

        if (existingOrder.status() != OrderStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED orders can be paid");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid payment amount");
        }

        if (type == null) {
            throw new IllegalArgumentException("Payment type is required");
        }

        if (currency == null || currency.isBlank()) {
            currency = "EUR";
        }

        if (type == PaymentType.CARD) {
            if (cardLast4 == null || !cardLast4.matches("\\d{4}")) {
                throw new IllegalArgumentException("Card last 4 digits must be exactly 4 numbers");
            }
            voucherCode = null;
        } else if (type == PaymentType.VOUCHER) {
            if (voucherCode == null || voucherCode.isBlank()) {
                throw new IllegalArgumentException("Voucher code is required");
            }
            cardLast4 = null;
        } else {
            cardLast4 = null;
            voucherCode = null;
        }

        Payment payment = new Payment(
                0,
                orderId,
                LocalDateTime.now().withNano(0),
                type,
                amount,
                currency,
                cardLast4,
                voucherCode
        );

        Payment saved = paymentDao.insert(payment);

        Order paidOrder = new Order(
                existingOrder.orderId(),
                existingOrder.sessionId(),
                existingOrder.createdAt(),
                OrderStatus.PAID,
                existingOrder.createdByStaffId(),
                existingOrder.notes()
        );

        boolean updated = orderDao.update(paidOrder);
        if (!updated) {
            throw new IllegalStateException("Payment saved but failed to update order status to PAID");
        }

        return saved;
    }

    @Override
    public Payment findByOrderId(long orderId) {
        return paymentDao.findByOrderId(orderId);
    }

    @Override
    public List<Payment> listPayments() {
        return paymentDao.findAll();
    }
}