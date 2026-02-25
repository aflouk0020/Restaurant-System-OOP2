package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.model.Payment;

import java.util.List;

public interface PaymentDao {

    Payment findById(long paymentId);

    Payment findByOrderId(long orderId); // one payment per order (UNIQUE)

    List<Payment> findAll();

    Payment insert(Payment payment);

    boolean delete(long paymentId);
}